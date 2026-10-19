package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.utility.MountableFile;
import vn.deadline.tracker.service.*;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RestoreOwnerGuardsTest extends DatabaseTest {
    @Autowired AccountService accounts;
    @Autowired WorkService work;
    @Autowired JdbcTemplate jdbc;
    String directory, actor, group;

    @BeforeEach void fixture() {
        String suffix=UUID.randomUUID().toString().replace("-", "");
        actor=accounts.register("guard."+suffix.substring(0,20),"Guard fixture","Synthetic restore guard password 123!");
        group=work.createGroup(actor,"Guard fixture","").getId();
        directory="/tmp/restore-guards-"+suffix;
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),"/deadline-project/scripts");
    }
    org.testcontainers.containers.Container.ExecResult shell(String command)throws Exception {
        return DB.execInContainer("bash","-lc",
            "export DB_HOST=127.0.0.1 DB_USER=root DB_NAME=deadline_test DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR="+directory+"; "+command);
    }
    String backup()throws Exception {
        var result=shell("bash /deadline-project/scripts/backup.sh");
        assertEquals(0,result.getExitCode(),result.getStderr());
        return result.getStdout().lines().filter(s->s.startsWith("BACKUP_FILE=")).findFirst().orElseThrow().substring(12);
    }
    String restoreDb(){return "deadline_restore_guards_"+UUID.randomUUID().toString().replace("-","");}

    @ParameterizedTest @CsvSource({
        "tr_group_create_owner,AFTER,INSERT,project_group",
        "tr_group_stable_owner,BEFORE,UPDATE,project_group",
        "tr_member_owner_insert,BEFORE,INSERT,group_member",
        "tr_member_owner_update,BEFORE,UPDATE,group_member",
        "tr_member_owner_delete,BEFORE,DELETE,group_member"})
    @DisplayName("REV-01 DATA-02/08 UAT-14 every no-op V4 trigger must reject restore despite intact tuple and rows")
    void noopTriggerRejected(String trigger,String timing,String event,String table)throws Exception {
        String file=backup();
        String ddl="DROP TRIGGER "+trigger+"; CREATE TRIGGER "+trigger+" "+timing+" "+event+" ON "+table+" FOR EACH ROW SET @review_noop=1;";
        var changed=shell("{ gzip -dc '"+file+"'; echo '"+ddl+"'; } | gzip > '"+file+".new'; mv '"+file+".new' '"+file+"'; sha256sum '"+file+"' | cut -d ' ' -f 1 > '"+file+".sha256'");
        assertEquals(0,changed.getExitCode(),changed.getStderr());
        var restored=shell("export BACKUP_FILE='"+file+"' RESTORE_DB="+restoreDb()+"; bash /deadline-project/scripts/verify-restore.sh");
        assertNotEquals(0,restored.getExitCode(),"Restore accepted no-op "+trigger);
        assertFalse(restored.getStdout().contains("RESTORE_OK"));
        assertTrue(restored.getStderr().contains("TRIGGERDEF")
                && restored.getStderr().contains("schema invariant"),restored.getStderr());
    }

    @Test @DisplayName("REV-01 DATA-08 whitespace inside an SQL literal is a changed trigger body")
    void literalWhitespaceIsNotFormatting()throws Exception {
        String file=backup();
        String original=jdbc.queryForObject("SELECT ACTION_STATEMENT FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='tr_member_owner_insert'",String.class);
        String body=original.replace("NEW.role='OWNER'","NEW.role='OWN ER'");
        assertNotEquals(original,body,"Fixture must change the role literal");
        String ddl="DROP TRIGGER tr_member_owner_insert;\nDELIMITER //\nCREATE TRIGGER tr_member_owner_insert BEFORE INSERT ON group_member FOR EACH ROW\n"+body+"//\nDELIMITER ;\n";
        var changed=shell("{ gzip -dc '"+file+"'; cat <<'CHANGED_TRIGGER'\n"+ddl+"CHANGED_TRIGGER\n} | gzip > '"+file+".new'; mv '"+file+".new' '"+file+"'; sha256sum '"+file+"' | cut -d ' ' -f 1 > '"+file+".sha256'");
        assertEquals(0,changed.getExitCode(),changed.getStderr());
        var restored=shell("export BACKUP_FILE='"+file+"' RESTORE_DB="+restoreDb()+"; bash /deadline-project/scripts/verify-restore.sh");
        assertNotEquals(0,restored.getExitCode(),"A changed SQL role literal must not pass as formatting");
        assertFalse(restored.getStdout().contains("RESTORE_OK"));
        assertTrue(restored.getStderr().contains("tr_member_owner_insert"),restored.getStderr());
    }

    @Test @DisplayName("REV-01 BR-14 DATA-08 original V4 restore keeps rows, rolls back probes and preserves Owner guards")
    void intactRestoreAndRollback()throws Exception {
        int users=jdbc.queryForObject("SELECT COUNT(*) FROM app_user",Integer.class);
        int groups=jdbc.queryForObject("SELECT COUNT(*) FROM project_group",Integer.class);
        int members=jdbc.queryForObject("SELECT COUNT(*) FROM group_member",Integer.class);
        String file=backup(),db=restoreDb();
        var result=shell("export BACKUP_FILE='"+file+"' RESTORE_DB="+db+"; bash /deadline-project/scripts/verify-restore.sh");
        assertEquals(0,result.getExitCode(),result.getStderr());
        assertTrue(result.getStdout().contains("OWNER_BEHAVIOR_OK"));
        assertTrue(result.getStdout().contains("RESTORE_OK"));
        var dataSource=new DriverManagerDataSource(DB.getJdbcUrl().replace("/deadline_test","/"+db),DB.getUsername(),DB.getPassword());
        var copy=new JdbcTemplate(dataSource);
        assertEquals(users,copy.queryForObject("SELECT COUNT(*) FROM app_user",Integer.class));
        assertEquals(groups,copy.queryForObject("SELECT COUNT(*) FROM project_group",Integer.class));
        assertEquals(members,copy.queryForObject("SELECT COUNT(*) FROM group_member",Integer.class));
        assertEquals(1,copy.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND user_id=? AND role='OWNER'",Integer.class,group,actor));
        // Normal metadata edits still work; all reviewer writes are rolled back.
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try(var rename=connection.prepareStatement("UPDATE project_group SET name=? WHERE id=?")) {
                    rename.setString(1,"Allowed fixture rename");rename.setString(2,group);assertEquals(1,rename.executeUpdate());
                }
                for(String sql:new String[]{"UPDATE project_group SET owner_id=UUID() WHERE id=?","DELETE FROM group_member WHERE group_id=? AND role='OWNER'"}) {
                    try(var statement=connection.prepareStatement(sql)) {
                        statement.setString(1,group);
                        var error=assertThrows(java.sql.SQLException.class,statement::executeUpdate);
                        assertEquals("45000",error.getSQLState(),"V4 must reject before unrelated FK/unique errors");
                    }
                }
            } finally { connection.rollback(); }
        }
        assertEquals("Guard fixture",copy.queryForObject("SELECT name FROM project_group WHERE id=?",String.class,group));
    }
}
