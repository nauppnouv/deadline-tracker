package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import vn.deadline.tracker.service.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OwnerIntegrityTest extends DatabaseTest {
    @Autowired AccountService accounts; @Autowired WorkService work; @Autowired JdbcTemplate jdbc;
    String user(){return accounts.register("o"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Owner fixture","Owner integrity fixture passphrase 123!");}
    @Test @DisplayName("P7-05 BR-14 DATA-02 direct group INSERT atomically has exactly the matching Owner membership")
    void rawGroupInsert() {
        String actor=user(),group=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO project_group(id,name,description,owner_id,created_at) VALUES(?,'Direct group','',?,UTC_TIMESTAMP(6))",group,actor);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND user_id=? AND role='OWNER'",Integer.class,group,actor));
        assertTrue(work.group(actor,group).isOwner(actor));
    }
    @Test @DisplayName("P7-05 BR-14 DATA-02 SQL cannot demote, delete, move or replace the Owner")
    void cannotBreakOwner() {
        String actor=user(),other=user(),group=work.createGroup(actor,"Guarded group","").getId();
        assertAll(
            ()->assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE group_member SET role='MEMBER' WHERE group_id=? AND user_id=?",group,actor)),
            ()->assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("DELETE FROM group_member WHERE group_id=? AND user_id=?",group,actor)),
            ()->assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE group_member SET user_id=? WHERE group_id=? AND user_id=?",other,group,actor)),
            ()->assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE project_group SET owner_id=? WHERE id=?",other,group)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND user_id=? AND role='OWNER'",Integer.class,group,actor));
    }
    JdbcTemplate legacyDatabase(String name)throws Exception {
        var result=DB.execInContainer("bash","-lc","MYSQL_PWD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" mariadb -h127.0.0.1 -uroot -e \"CREATE DATABASE \\`"+name+"\\`; GRANT ALL ON \\`"+name+"\\`.* TO 'deadline_test'@'%'\"");
        assertEquals(0,result.getExitCode(),result.getStderr());
        String url=DB.getJdbcUrl().replace("/deadline_test","/"+name);
        Flyway.configure().dataSource(url,DB.getUsername(),DB.getPassword()).target("3").load().migrate();
        return new JdbcTemplate(new DriverManagerDataSource(url,DB.getUsername(),DB.getPassword()));
    }
    void seedLegacy(JdbcTemplate old,boolean member) {
        old.update("INSERT INTO app_user(id,username,display_name,password_hash,created_at) VALUES('owner','legacy.owner','Owner','disabled-fixture',UTC_TIMESTAMP(6))");
        old.update("INSERT INTO project_group(id,name,description,owner_id,created_at) VALUES('group','Legacy group','','owner',UTC_TIMESTAMP(6))");
        if(member)old.update("INSERT INTO group_member(id,group_id,user_id,role,joined_at) VALUES('membership','group','owner','OWNER',UTC_TIMESTAMP(6))");
    }
    @Test @DisplayName("P7-05 DATA-01/02 migration upgrades V3 valid data without changing membership and guards future SQL")
    void upgradeValid()throws Exception {
        String name="deadline_restore_upgrade_"+UUID.randomUUID().toString().replace("-","");var old=legacyDatabase(name);seedLegacy(old,true);
        Flyway.configure().dataSource(DB.getJdbcUrl().replace("/deadline_test","/"+name),DB.getUsername(),DB.getPassword()).load().migrate();
        assertTrue(old.queryForObject("SELECT MAX(version+0) FROM flyway_schema_history WHERE success=1",Integer.class)>=4);
        assertEquals("membership",old.queryForObject("SELECT id FROM group_member WHERE group_id='group'",String.class));
        assertThrows(org.springframework.dao.DataAccessException.class,()->old.update("UPDATE group_member SET role='MEMBER' WHERE id='membership'"));
    }
    @Test @DisplayName("P7-05 DATA-01/02 migration refuses preexisting group without Owner membership and retains data")
    void rejectInconsistentLegacy()throws Exception {
        String name="deadline_restore_invalid_"+UUID.randomUUID().toString().replace("-","");var old=legacyDatabase(name);seedLegacy(old,false);
        assertThrows(org.flywaydb.core.api.FlywayException.class,()->Flyway.configure().dataSource(DB.getJdbcUrl().replace("/deadline_test","/"+name),DB.getUsername(),DB.getPassword()).load().migrate());
        assertEquals("Legacy group",old.queryForObject("SELECT name FROM project_group WHERE id='group'",String.class));
        assertEquals(0,old.queryForObject("SELECT COUNT(*) FROM group_member",Integer.class));
    }
}
