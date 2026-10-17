package vn.deadline.tracker;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaStartupTest extends DatabaseTest {
    String url;
    JdbcTemplate fresh() throws Exception {
        String db="deadline_restore_boot_"+UUID.randomUUID().toString().replace("-","");
        var r=DB.execInContainer("bash","-lc","MYSQL_PWD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" mariadb -h127.0.0.1 -uroot -e \"CREATE DATABASE \\`"+db+"\\`; GRANT ALL ON \\`"+db+"\\`.* TO 'deadline_test'@'%'\"");
        assertEquals(0,r.getExitCode(),r.getStderr());
        url=DB.getJdbcUrl().replace("/deadline_test","/"+db);
        Flyway.configure().dataSource(url,DB.getUsername(),DB.getPassword()).load().migrate();
        return new JdbcTemplate(new DriverManagerDataSource(url,DB.getUsername(),DB.getPassword()));
    }
    org.springframework.context.ConfigurableApplicationContext start() { return start("test"); }
    org.springframework.context.ConfigurableApplicationContext start(String profile) {
        return new SpringApplicationBuilder(DeadlineTrackerApplication.class).web(WebApplicationType.NONE).profiles(profile)
            .initializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("schema-fixture",Map.of(
                "spring.datasource.url",url,"spring.datasource.username",DB.getUsername(),"spring.datasource.password",DB.getPassword(),"spring.session.jdbc.cleanup-cron","-",
                "DB_PASSWORD",DB.getPassword(),"DB_ROOT_PASSWORD",UUID.randomUUID().toString(),"SERVER_ADDRESS","127.0.0.1","server.forward-headers-strategy","native")))).run();
    }
    void rejected(String object) {
        var error=assertThrows(Exception.class,()->{try(var ignored=start()) { fail("Context must refuse missing "+object); }});
        StringBuilder causes=new StringBuilder();
        for(Throwable cause=error;cause!=null;cause=cause.getCause())causes.append(cause.getMessage()).append('\n');
        assertTrue(causes.toString().contains(object),causes.toString());
    }
    @ParameterizedTest @ValueSource(strings={"tr_group_create_owner","tr_group_stable_owner","tr_member_owner_insert","tr_member_owner_update","tr_member_owner_delete"})
    @DisplayName("SEC-11 DATA-02 R2-01 startup refuses each missing V4 trigger")
    void missingTrigger(String trigger)throws Exception {
        var jdbc=fresh(); jdbc.execute("DROP TRIGGER "+trigger); rejected(trigger);
    }
    @Test @DisplayName("DATA-02 R2-01 same trigger name with wrong timing cannot pass startup")
    void wrongTiming()throws Exception {
        var jdbc=fresh();jdbc.execute("DROP TRIGGER tr_member_owner_update");
        jdbc.execute("CREATE TRIGGER tr_member_owner_update AFTER UPDATE ON group_member FOR EACH ROW SET @schema_probe=1");
        rejected("tr_member_owner_update");
    }
    @ParameterizedTest @ValueSource(strings={"fk_task_assignee","ck_task_status","uq_one_owner"})
    @DisplayName("DATA-02 R2-01 startup refuses missing FK CHECK or unique constraints")
    void missingConstraint(String name)throws Exception {
        var jdbc=fresh();jdbc.execute("ALTER TABLE "+(name.equals("uq_one_owner")?"group_member":"task")+" DROP CONSTRAINT "+name);rejected(name);
    }
    @Test @DisplayName("DATA-02 R2-01 intact V4 schema starts normally")
    void intact()throws Exception { fresh();try(var context=start()){assertTrue(context.isActive());} }
    @Test @DisplayName("SEC-11 DATA-02 R2-01 prod context also refuses missing V4 trigger")
    void missingProdTrigger()throws Exception {
        var jdbc=fresh();jdbc.execute("DROP TRIGGER tr_group_create_owner");
        var error=assertThrows(Exception.class,()->{try(var ignored=start("prod")){fail("prod must refuse missing trigger");}});
        String messages="";for(Throwable t=error;t!=null;t=t.getCause())messages+=t.getMessage();
        assertTrue(messages.contains("tr_group_create_owner"),messages);
    }
}
