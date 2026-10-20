package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.flywaydb.core.Flyway;
import vn.deadline.tracker.ops.*;
import vn.deadline.tracker.service.*;
import vn.deadline.tracker.domain.TaskStatus;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Every principal/schema in this test lives only in the disposable Testcontainers server. */
class MigrationRuntimeSeparationTest extends DatabaseTest {
    String database,migrationUser,runtimeUser,url,migrationPassword,runtimePassword,scope;
    JdbcTemplate runtime,migration;
    @BeforeEach void freshPrincipals() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,12);
        database="deadline_restore_p302_"+suffix;migrationUser="mg_"+suffix;runtimeUser="rt_"+suffix;
        migrationPassword=UUID.randomUUID().toString();runtimePassword=UUID.randomUUID().toString();
        url=DB.getJdbcUrl().replace("/deadline_test","/"+database);
        scope="`"+database.replace("_","\\_")+"`.*";
        admin("CREATE DATABASE `"+database+"`;\n"
            +"CREATE USER '"+migrationUser+"'@'%' IDENTIFIED BY '"+migrationPassword+"';\n"
            +"CREATE USER '"+runtimeUser+"'@'%' IDENTIFIED BY '"+runtimePassword+"';\n"
            +"GRANT ALL ON "+scope+" TO '"+migrationUser+"'@'%';\n"
            +"GRANT SELECT, TRIGGER ON "+scope+" TO '"+runtimeUser+"'@'%';");
        runtime=new JdbcTemplate(new DriverManagerDataSource(url,runtimeUser,runtimePassword));
        migration=new JdbcTemplate(new DriverManagerDataSource(url,migrationUser,migrationPassword));
    }
    void admin(String sql)throws Exception {
        var result=DB.execInContainer("bash","-lc","MYSQL_PWD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" mariadb -h127.0.0.1 -uroot <<'SQL'\n"+sql+"\nSQL");
        assertEquals(0,result.getExitCode(),"Disposable fixture admin SQL failed");
    }
    Map<String,String> migrationEnv() {return Map.of("MIGRATION_DB_URL",url.replace("://localhost:","://127.0.0.1:"),"MIGRATION_DB_USER",migrationUser,"MIGRATION_DB_PASSWORD",migrationPassword);}
    void migrateAndGrantDml()throws Exception {
        MigrationCommand.execute(migrationEnv());
        StringBuilder grants=new StringBuilder();
        for(String table:List.of("app_user","project_group","group_member","task","invitation"))
            grants.append("GRANT INSERT, UPDATE ON `").append(database).append("`.").append(table).append(" TO '").append(runtimeUser).append("'@'%';\n");
        grants.append("GRANT INSERT ON `").append(database).append("`.task_event TO '").append(runtimeUser).append("'@'%';\n");
        for(String table:List.of("SPRING_SESSION","SPRING_SESSION_ATTRIBUTES"))
            grants.append("GRANT INSERT, UPDATE, DELETE ON `").append(database).append("`.").append(table).append(" TO '").append(runtimeUser).append("'@'%';\n");
        admin(grants.toString());
    }
    ConfigurableApplicationContext start(String profile) {return start(profile,false);}
    ConfigurableApplicationContext start(String profile,boolean web) {
        var env=new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        var values=new HashMap<String,Object>(Map.of("DB_PASSWORD",runtimePassword,"spring.datasource.url",url,
            "spring.datasource.username",runtimeUser,"spring.datasource.password",runtimePassword,
            "SERVER_ADDRESS","127.0.0.1","server.forward-headers-strategy","native",
            "app.ops.execute","false","logging.level.root","ERROR"));
        env.getPropertySources().addFirst(new MapPropertySource("runtime-only-fixture",values));
        return new SpringApplicationBuilder(DeadlineTrackerApplication.class).environment(env).profiles(profile)
            .properties("server.port=0").web(web?WebApplicationType.SERVLET:WebApplicationType.NONE).run();
    }
    String causes(Throwable error) {var text=new StringBuilder();for(Throwable t=error;t!=null;t=t.getCause())text.append(t.getMessage()).append('\n');return text.toString();}
    int migrateProcess(Map<String,String> configuration) throws Exception {
        Path output=Path.of("target/p302-migrate-"+UUID.randomUUID()+".log");
        String java=System.getProperty("os.name").startsWith("Windows")?"java.exe":"java";
        var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",java).toString(),
            "-Duser.timezone=UTC","-cp",System.getProperty("java.class.path"),DeadlineTrackerApplication.class.getName(),"migrate");
        builder.environment().keySet().removeIf(k->k.startsWith("DB_")||k.startsWith("SPRING_")||k.startsWith("MIGRATION_DB_")||k.equals("JAVA_TOOL_OPTIONS")||k.equals("JDK_JAVA_OPTIONS"));
        builder.environment().putAll(configuration);builder.redirectErrorStream(true).redirectOutput(output.toFile());
        Process process=builder.start();
        if(!process.waitFor(45,TimeUnit.SECONDS)){process.destroyForcibly();fail("Migration-only process timed out");}
        String text=Files.readString(output);
        assertFalse(text.contains(migrationPassword));assertFalse(text.contains(runtimePassword));
        assertFalse(text.contains("Tomcat started"));
        if(process.exitValue()==0)assertTrue(text.contains("MIGRATION_OK"));
        return process.exitValue();
    }
    @Test @DisplayName("P3-02 migration process uses only migration credential, reruns safely, never starts web")
    void separateProcessAndIdempotence()throws Exception {
        assertEquals(0,migrateProcess(migrationEnv()));
        assertEquals(0,migrateProcess(migrationEnv()));
        assertEquals(4,migration.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1",Integer.class));
        assertEquals(Set.of(migrationUser),new HashSet<>(migration.queryForList("SELECT DISTINCT installed_by FROM flyway_schema_history",String.class)));
        assertEquals(Set.of(migrationUser+"@%"),new HashSet<>(migration.queryForList("SELECT DISTINCT DEFINER FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()",String.class)));
        assertEquals(0,migration.queryForObject("SELECT COUNT(*) FROM project_group",Integer.class));
    }
    @Test @DisplayName("P3-02 reduced runtime supports Owner/task/event/session and non-web ops with locked definer login")
    @SuppressWarnings({"unchecked","rawtypes"})
    void runtimeBusinessAndOpsWithRetainedDefiner()throws Exception {
        migrateAndGrantDml();
        admin("REVOKE ALL PRIVILEGES, GRANT OPTION FROM '"+migrationUser+"'@'%';\n"
            +"GRANT SELECT, INSERT, TRIGGER ON "+scope+" TO '"+migrationUser+"'@'%';\n"
            +"ALTER USER '"+migrationUser+"'@'%' ACCOUNT LOCK;");
        assertThrows(Exception.class,()->migration.queryForObject("SELECT 1",Integer.class));
        String username="p302_"+UUID.randomUUID().toString().replace("-","").substring(0,15);
        try(var context=start("prod",true)) {
            assertTrue(context.getBeansOfType(Flyway.class).isEmpty());
            assertNotNull(context.getBean(SchemaInvariantVerifier.class));
            assertNull(context.getEnvironment().getProperty("MIGRATION_DB_PASSWORD"));
            var account=context.getBean(AccountService.class);var work=context.getBean(WorkService.class);
            String actor=account.register(username,"Synthetic Runtime","Synthetic user passphrase 123!");
            var group=work.createGroup(actor,"P3-02 private fixture","");
            assertEquals(1,runtime.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND role='OWNER'",Integer.class,group.getId()));
            var task=work.createTask(actor,group.getId(),"P3-02 task","",actor,Instant.now().plusSeconds(3600));
            work.changeStatus(actor,group.getId(),task.getId(),TaskStatus.IN_PROGRESS,task.getVersion());
            assertEquals(2,runtime.queryForObject("SELECT COUNT(*) FROM task_event WHERE task_id=?",Integer.class,task.getId()));
            org.springframework.session.SessionRepository sessions=context.getBean(JdbcIndexedSessionRepository.class);
            org.springframework.session.Session session=sessions.createSession();session.setAttribute("synthetic","value");sessions.save(session);
            assertNotNull(sessions.findById(session.getId()));sessions.deleteById(session.getId());assertNull(sessions.findById(session.getId()));
            assertThrows(Exception.class,()->runtime.update("UPDATE group_member SET role='MEMBER' WHERE group_id=?",group.getId()));
            assertEquals(1,runtime.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND role='OWNER'",Integer.class,group.getId()));
        }
        try(var context=start("ops")) {
            assertTrue(context.getBeansOfType(Flyway.class).isEmpty());assertNotNull(context.getBean(SchemaInvariantVerifier.class));
            assertFalse(context.getEnvironment().getProperty("spring.flyway.enabled",Boolean.class,true));
            assertNotNull(context.getBean(OpsActions.class).resetPassword(username));
            assertEquals(1,runtime.queryForObject("SELECT must_change_password FROM app_user WHERE username=?",Integer.class,username));
        }
    }
    @Test @DisplayName("P3-02 runtime denies CREATE ALTER DROP and migration-history writes; TRIGGER still permits trigger DDL")
    void runtimePrivilegeBoundary()throws Exception {
        migrateAndGrantDml();
        assertThrows(Exception.class,()->runtime.execute("CREATE TABLE forbidden_p302(id INT)"));
        assertThrows(Exception.class,()->runtime.execute("ALTER TABLE task ADD COLUMN forbidden_p302 INT"));
        assertThrows(Exception.class,()->runtime.execute("DROP TABLE task_event"));
        assertThrows(Exception.class,()->runtime.update("UPDATE flyway_schema_history SET installed_by='forbidden'"));
        assertThrows(Exception.class,()->runtime.update("DELETE FROM task_event"));
        migration.execute("CREATE TABLE trigger_probe_p302(id INT)");
        runtime.execute("CREATE TRIGGER runtime_trigger_p302 BEFORE INSERT ON trigger_probe_p302 FOR EACH ROW SET @p302_probe=1");
        assertEquals(1,runtime.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='runtime_trigger_p302'",Integer.class));
        runtime.execute("DROP TRIGGER runtime_trigger_p302");
        migration.execute("DROP TABLE trigger_probe_p302");
        assertEquals(5,runtime.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()",Integer.class));
    }
    @Test @DisplayName("P3-02 prod/ops fail closed on empty schema without creating Flyway history")
    void missingMigrationRefused() {
        for(String profile:List.of("prod","ops"))assertThrows(Exception.class,()->{try(var ignored=start(profile)){fail("Unmigrated schema accepted");}});
        assertEquals(0,runtime.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()",Integer.class));
    }
    @Test @DisplayName("P3-02 missing TRIGGER metadata privilege blocks startup, never bypass verifier")
    void missingTriggerPrivilegeRefused()throws Exception {
        migrateAndGrantDml();admin("REVOKE TRIGGER ON "+scope+" FROM '"+runtimeUser+"'@'%';");
        for(String profile:List.of("prod","ops")) {
            var error=assertThrows(Exception.class,()->{try(var ignored=start(profile)){fail("Invisible triggers accepted");}});
            assertTrue(causes(error).contains("tr_group_create_owner"));
        }
    }
    @Test @DisplayName("P3-02 exact trigger-body drift refused by runtime and migration validation")
    void triggerDriftRefused()throws Exception {
        migrateAndGrantDml();
        migration.execute("DROP TRIGGER tr_group_create_owner");
        migration.execute("CREATE TRIGGER tr_group_create_owner AFTER INSERT ON project_group FOR EACH ROW SET @p302_noop=1");
        var error=assertThrows(Exception.class,()->{try(var ignored=start("prod")){fail("No-op trigger accepted");}});
        assertTrue(causes(error).contains("tr_group_create_owner"));
        assertThrows(Exception.class,()->MigrationCommand.execute(migrationEnv()));
    }
    @Test @DisplayName("P3-02 runtime validates Flyway checksums without repairing history")
    void historyDriftRefused()throws Exception {
        migrateAndGrantDml();migration.update("UPDATE flyway_schema_history SET checksum=1 WHERE version='1'");
        for(String profile:List.of("prod","ops"))assertThrows(Exception.class,()->{try(var ignored=start(profile)){fail("Bad migration history accepted");}});
        assertEquals(1,runtime.queryForObject("SELECT checksum FROM flyway_schema_history WHERE version='1'",Integer.class));
    }
    @Test @DisplayName("P3-02 migration refuses runtime-only credentials, root and example secrets before connecting")
    void migrationCredentialValidation()throws Exception {
        assertThrows(IllegalArgumentException.class,()->MigrationCommand.execute(Map.of("DB_URL",url,"DB_USER",runtimeUser,"DB_PASSWORD",runtimePassword)));
        var env=new HashMap<>(migrationEnv());env.put("MIGRATION_DB_USER","root");assertThrows(IllegalArgumentException.class,()->MigrationCommand.execute(env));
        env.put("MIGRATION_DB_USER",migrationUser);env.put("MIGRATION_DB_PASSWORD","CHANGE_ME_MIGRATION_PASSWORD");assertThrows(IllegalArgumentException.class,()->MigrationCommand.execute(env));
        env.put("MIGRATION_DB_PASSWORD",migrationPassword);env.put("MIGRATION_DB_URL",url+"?user=root");assertThrows(IllegalArgumentException.class,()->MigrationCommand.execute(env));
        env.put("MIGRATION_DB_URL","jdbc:mariadb://192.0.2.1:3306/forbidden");assertThrows(IllegalArgumentException.class,()->MigrationCommand.execute(env));
        assertNotEquals(0,migrateProcess(Map.of()));
    }
}
