package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.images.builder.Transferable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackupFailureTest extends DatabaseTest {
    String directory;
    @BeforeEach void scripts() {
        directory="/tmp/backup-failure-"+UUID.randomUUID();
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),"/deadline-project/scripts");
        String wrapper="""
            #!/usr/bin/env bash
            for argument in "$@"; do
              [[ "$argument" == *information_schema.SCHEMATA* ]] && continue
              if [[ -n "${FAIL_SQL:-}" && "$argument" == *"$FAIL_SQL"* ]]; then
                echo 'Injected database metadata failure' >&2
                exit 42
              fi
            done
            exec /usr/bin/mariadb "$@"
            """;
        DB.copyFileToContainer(Transferable.of(wrapper.getBytes(StandardCharsets.UTF_8),0700),directory+"/bin/mariadb");
    }
    ExecResult run(String command)throws Exception {
        return DB.execInContainer("bash","-lc","export DB_HOST=127.0.0.1 DB_USER=root DB_NAME=deadline_test DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR="+directory+"/backups BACKUP_KEEP=1; "+command);
    }
    String backup()throws Exception {
        var result=run("bash /deadline-project/scripts/backup.sh");
        assertEquals(0,result.getExitCode(),result.getStderr());
        return result.getStdout().lines().filter(s->s.startsWith("BACKUP_FILE=")).findFirst().orElseThrow().substring(12);
    }
    @ParameterizedTest @ValueSource(strings={"SHOW TABLES","SELECT COUNT(*)","SELECT SHA2(","information_schema.COLUMNS"})
    @DisplayName("P7-01 DATA-08 UAT-14 failed backup query never publishes or prunes the preceding backup")
    void backupQueryFailure(String query)throws Exception {
        String good=backup();
        var failed=run("export PATH="+directory+"/bin:$PATH FAIL_SQL='"+query+"'; bash /deadline-project/scripts/backup.sh");
        assertTrue(failed.getStderr().contains("Injected database metadata failure"),failed.getStderr());
        assertAll(
            ()->assertNotEquals(0,failed.getExitCode(),"Backup must fail closed"),
            ()->assertFalse(failed.getStdout().contains("BACKUP_FILE=")),
            ()->assertEquals(0,run("test -s '"+good+"' && test -s '"+good+".tables.tsv'").getExitCode(),"Keep preceding backup"));
    }
    @ParameterizedTest @ValueSource(strings={"SHOW TABLES","SELECT COUNT(*) FROM", "SELECT SHA2(","information_schema.COLUMNS","information_schema.KEY_COLUMN_USAGE","LEFT JOIN"})
    @DisplayName("P7-01 DATA-08 UAT-14 failed restore inventory, checksum or FK query cannot report RESTORE_OK")
    void restoreQueryFailure(String query)throws Exception {
        String good=backup();
        var failed=run("export PATH="+directory+"/bin:$PATH FAIL_SQL='"+query+"' RESTORE_DB=deadline_restore_"+UUID.randomUUID().toString().replace("-","")+" BACKUP_FILE='"+good+"'; bash /deadline-project/scripts/verify-restore.sh");
        assertTrue(failed.getStderr().contains("Injected database metadata failure"),failed.getStderr());
        assertAll(()->assertNotEquals(0,failed.getExitCode(),"Restore must fail closed"),()->assertFalse(failed.getStdout().contains("RESTORE_OK")));
    }
    @Test @DisplayName("P7-02 DATA-08 logical checksum rejects changed data even with matching row count and valid dump SHA")
    void changedDataCannotPass()throws Exception {
        String table="backup_fixture_"+UUID.randomUUID().toString().replace("-","");
        assertEquals(0,run("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot deadline_test -e \"CREATE TABLE "+table+" (id INT PRIMARY KEY, value VARCHAR(40)); INSERT INTO "+table+" VALUES(1,'backup-original-marker')\"").getExitCode());
        String good=backup(),tampered=directory+"/tampered.sql.gz";
        var changed=run("gzip -dc '"+good+"' | sed 's/backup-original-marker/backup-modified-marker/g' | gzip > '"+tampered+"'; sha256sum '"+tampered+"' | cut -d ' ' -f 1 > '"+tampered+".sha256'; cp '"+good+".tables.tsv' '"+tampered+".tables.tsv'; export RESTORE_DB=deadline_restore_changed_"+UUID.randomUUID().toString().replace("-","")+" BACKUP_FILE='"+tampered+"'; bash /deadline-project/scripts/verify-restore.sh");
        assertNotEquals(0,changed.getExitCode());
        assertTrue(changed.getStderr().contains("Mismatch: "+table),changed.getStderr());
        assertFalse(changed.getStdout().contains("RESTORE_OK"));
    }
}
