package vn.deadline.tracker;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.utility.MountableFile;import vn.deadline.tracker.service.*;
import java.nio.file.*;import java.time.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class BackupRestoreTest extends DatabaseTest {
    @Autowired AccountService accounts;@Autowired WorkService work;
    String env(){return "export DB_HOST=127.0.0.1 DB_PORT=3306 DB_USER=root DB_NAME=deadline_test DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR=/tmp/deadline-backups BACKUP_KEEP=2; ";}
    org.testcontainers.containers.Container.ExecResult run(String command)throws Exception{return DB.execInContainer("bash","-lc",env()+command);}
    @Test @DisplayName("DATA-08 UAT-14 real dump/restore, table rows+checksums, FK, permissions and retention")
    void restoreAndGuards()throws Exception{
        assertTrue(Files.exists(Path.of("scripts/backup.sh")),"Write backup implementation after RED");assertTrue(Files.exists(Path.of("scripts/verify-restore.sh")));
        String user=accounts.register("b"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Backup fixture","Backup test fixture passphrase 123!");String group=work.createGroup(user,"Backup group","").getId();var task=work.createTask(user,group,"Backup task","",user,Instant.now().plusSeconds(3600));
        String outsider=accounts.register("b"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Outside backup group","Backup outside fixture passphrase 123!");
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),"/deadline-project/scripts");
        var backup=run("bash /deadline-project/scripts/backup.sh");assertEquals(0,backup.getExitCode(),backup.getStderr());
        String file=backup.getStdout().lines().filter(s->s.startsWith("BACKUP_FILE=")).findFirst().orElseThrow().substring("BACKUP_FILE=".length());assertTrue(file.matches("/tmp/deadline-backups/deadline-[A-Za-z0-9T.Z-]+\\.sql\\.gz"));
        assertEquals("600",run("stat -c '%a' '"+file+"'").getStdout().strip());
        String restore="deadline_restore_"+UUID.randomUUID().toString().replace("-","");
        long restoreStart=System.nanoTime();var verified=run("export RESTORE_DB="+restore+" BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh");long restoreMillis=(System.nanoTime()-restoreStart)/1_000_000;
        assertEquals(0,verified.getExitCode(),verified.getStderr());assertTrue(verified.getStdout().contains("RESTORE_OK"));
        Files.createDirectories(Path.of("target"));Files.writeString(Path.of("target/restore-verification.txt"),"RESTORE_STARTED_UTC="+Instant.now().minusMillis(restoreMillis)+"\nRESTORE_ELAPSED_MS="+restoreMillis+" (trial data only; excludes application boot)\n"+verified.getStdout());
        var grant=run("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -e \"GRANT ALL ON \\`"+restore+"\\`.* TO 'deadline_test'@'%'\"");assertEquals(0,grant.getExitCode(),grant.getStderr());
        try(var restored=new org.springframework.boot.builder.SpringApplicationBuilder(DeadlineTrackerApplication.class).web(org.springframework.boot.WebApplicationType.NONE).profiles("test").initializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("restored-database",Map.of("spring.datasource.url",DB.getJdbcUrl().replace("/deadline_test","/"+restore),"spring.datasource.username",DB.getUsername(),"spring.datasource.password",DB.getPassword(),"spring.session.jdbc.cleanup-cron","-")))).run()){
            var restoredWork=restored.getBean(WorkService.class);
            assertEquals("Backup task",restoredWork.task(user,group,task.getId()).getTitle());
            assertEquals(vn.deadline.tracker.domain.RuleViolation.Kind.NOT_FOUND,assertThrows(vn.deadline.tracker.domain.RuleViolation.class,()->restoredWork.task(outsider,group,task.getId())).getKind());
            var restoredJdbc=restored.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
            String fresh=restoredWork.createGroup(user,"Created after restore","").getId();
            assertEquals(1,restoredJdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND user_id=? AND role='OWNER'",Integer.class,fresh,user));
            assertThrows(org.springframework.dao.DataAccessException.class,()->restoredJdbc.update("UPDATE group_member SET role='MEMBER' WHERE group_id=? AND user_id=?",fresh,user));
        }
        Files.writeString(Path.of("target/restore-verification.txt"),"APP_RESTORED_OK owner reads task; outsider rejected NOT_FOUND\n",java.nio.file.StandardOpenOption.APPEND);
        assertNotEquals(0,run("export RESTORE_DB=deadline_test BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh").getExitCode());
        assertNotEquals(0,run("export RESTORE_DB="+restore+" BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh").getExitCode());
        assertNotEquals(0,run("cp '"+file+"' /tmp/bad-backup.sql.gz; cp '"+file+".sha256' /tmp/bad-backup.sql.gz.sha256; printf tamper >> /tmp/bad-backup.sql.gz; export RESTORE_DB=deadline_restore_bad BACKUP_FILE=/tmp/bad-backup.sql.gz; bash /deadline-project/scripts/verify-restore.sh").getExitCode());
        assertEquals("Backup task",work.task(user,group,task.getId()).getTitle());
        assertEquals(0,run("touch /tmp/deadline-backups/keep.txt; for n in 1 2 3; do bash /deadline-project/scripts/backup.sh >/dev/null || exit 1; done; test -f /tmp/deadline-backups/keep.txt").getExitCode());
        assertEquals("2",run("find /tmp/deadline-backups -maxdepth 1 -name 'deadline-*.sql.gz' -type f | wc -l").getStdout().strip());
    }
}
