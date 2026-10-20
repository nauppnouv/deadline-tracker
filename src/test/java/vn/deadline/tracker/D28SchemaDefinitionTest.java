package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** D-28: definitions are checked independently of row checksums and object names. */
@ExtendWith(OutputCaptureExtension.class)
class D28SchemaDefinitionTest extends DatabaseTest {
    final SchemaStartupTest boot = new SchemaStartupTest();
    JdbcTemplate jdbc;
    String source, directory;

    @BeforeEach void fixture() throws Exception {
        jdbc=boot.fresh();
        source=jdbc.queryForObject("SELECT DATABASE()",String.class);
        directory="/tmp/codex_d28_"+UUID.randomUUID().toString().replace("-","");
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),directory+"/project/scripts");
        // A real Owner makes the +0 mutation byte-for-byte identical in the data manifest.
        jdbc.update("INSERT INTO app_user(id,username,display_name,password_hash,created_at) VALUES('d28-user','d28.fixture','Fixture','unusable',UTC_TIMESTAMP(6))");
        jdbc.update("INSERT INTO project_group(id,name,description,owner_id,created_at) VALUES('d28-group','Fixture','','d28-user',UTC_TIMESTAMP(6))");
    }
    String clause(String table,String name) {
        return jdbc.queryForObject("SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=? AND CONSTRAINT_NAME=?",String.class,table,name);
    }
    String generated() {
        return jdbc.queryForObject("SELECT GENERATION_EXPRESSION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='group_member' AND COLUMN_NAME='owner_marker'",String.class);
    }
    String mutation(String kind) {
        return switch(kind) {
            case "noop" -> "ALTER TABLE task DROP CONSTRAINT ck_task_status, ADD CONSTRAINT ck_task_status CHECK(1=1);";
            case "near" -> "ALTER TABLE task DROP CONSTRAINT ck_task_status, ADD CONSTRAINT ck_task_status CHECK(status IN ('TODO','IN_PROGRESS','DONE','INVALID'));";
            case "values" -> "ALTER TABLE group_member MODIFY owner_marker INT GENERATED ALWAYS AS (CASE WHEN role='OWNER' THEN 2 ELSE NULL END) VIRTUAL;";
            case "same" -> "ALTER TABLE group_member MODIFY owner_marker INT GENERATED ALWAYS AS (("+generated()+")+0) VIRTUAL;";
            case "stored" -> "ALTER TABLE group_member DROP INDEX uq_one_owner, DROP COLUMN owner_marker, ADD owner_marker INT GENERATED ALWAYS AS ("+generated()+") PERSISTENT, ADD CONSTRAINT uq_one_owner UNIQUE(group_id,owner_marker);";
            case "type" -> "ALTER TABLE group_member MODIFY owner_marker BIGINT GENERATED ALWAYS AS ("+generated()+") VIRTUAL;";
            case "newCheck" -> "ALTER TABLE task ADD CONSTRAINT ck_d28_unreviewed CHECK(1=1);";
            case "newGenerated" -> "ALTER TABLE task ADD codex_d28_unreviewed INT GENERATED ALWAYS AS (1) VIRTUAL;";
            case "collation" -> "ALTER TABLE task MODIFY status VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL;";
            case "length" -> "ALTER TABLE task MODIFY title VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;";
            case "default" -> "ALTER TABLE task ALTER COLUMN version SET DEFAULT 1;";
            case "triggerNoop" -> "CREATE OR REPLACE TRIGGER tr_member_owner_delete BEFORE DELETE ON group_member FOR EACH ROW SET @schema_probe=1;";
            default -> throw new IllegalArgumentException(kind);
        };
    }
    String object(String kind) {
        return switch(kind) {
            case "noop","near" -> "ck_task_status";
            case "newCheck" -> "ck_d28_unreviewed";
            case "newGenerated" -> "task.codex_d28_unreviewed";
            case "collation" -> "task.status";
            case "length" -> "task.title";
            case "default" -> "task.version";
            case "triggerNoop" -> "tr_member_owner_delete";
            default -> "group_member.owner_marker";
        };
    }
    void rejectStartup(String profile,String object,CapturedOutput output) {
        var error=assertThrows(Exception.class,()->{try(var ignored=boot.start(profile)){}},"Startup accepted "+object);
        String messages="";
        for(Throwable t=error;t!=null;t=t.getCause())messages+=t.getMessage()+"\n";
        assertTrue(messages.contains(object),messages);
        assertFalse(messages.contains(DB.getPassword()),"Failure must not disclose datasource secret");
        assertFalse(output.getAll().contains(DB.getPassword()),"Startup logs must not disclose datasource secret");
    }
    @ParameterizedTest @CsvSource({
        "test,noop","prod,noop","test,near","prod,near",
        "test,values","prod,values","test,same","prod,same","test,collation","prod,collation","test,length","prod,length","test,default","prod,default",
        "test,stored","prod,stored","test,type","prod,type",
        "test,newCheck","prod,newCheck","test,newGenerated","prod,newGenerated","test,triggerNoop","prod,triggerNoop"})
    @DisplayName("D-28 DATA-02 SEC-11 startup rejects changed or uncontracted definitions in both profiles")
    void startupRejects(String profile,String kind,CapturedOutput output) {
        jdbc.execute(mutation(kind));
        rejectStartup(profile,object(kind),output);
    }
    void recreateOriginal() {
        String check=clause("task","ck_task_status"),expression=generated();
        jdbc.execute("ALTER TABLE task DROP CONSTRAINT ck_task_status, ADD CONSTRAINT ck_task_status CHECK("+check+")");
        jdbc.execute("ALTER TABLE group_member MODIFY owner_marker INT GENERATED ALWAYS AS ("+expression+") VIRTUAL");
    }
    @ParameterizedTest @ValueSource(strings={"test","prod"})
    @DisplayName("D-28 DATA-02 recreating the original CHECK and generated expression is accepted")
    void exactRecreationStarts(String profile) {
        recreateOriginal();
        try(var context=boot.start(profile)){assertTrue(context.isActive());}
    }
    @Test @DisplayName("D-28 DATA-02 contract exactly covers every CHECK and generated column after V1 through V4")
    void completeContract() throws Exception {
        // Deliberately query metadata independently of schema-inventory.sql.
        var actual=new TreeSet<>(jdbc.queryForList("SELECT CONCAT('CHECKDEF|',TABLE_NAME,'|',CONSTRAINT_NAME,'|',SHA2(CHECK_CLAUSE,256)) FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()",String.class));
        actual.addAll(jdbc.queryForList("SELECT CONCAT('GENERATEDDEF|',TABLE_NAME,'|',COLUMN_NAME,'|',COLUMN_TYPE,'|',EXTRA,'|',SHA2(GENERATION_EXPRESSION,256)) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND IS_GENERATED='ALWAYS'",String.class));
        var triggerBodies=new TreeSet<>(jdbc.queryForList("SELECT CONCAT('TRIGGERDEF|',TRIGGER_NAME,'|',EVENT_OBJECT_TABLE,'|',ACTION_TIMING,'|',EVENT_MANIPULATION,'|',SHA2(ACTION_STATEMENT,256)) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND ACTION_STATEMENT IS NOT NULL",String.class));
        Files.write(Path.of("target/f02-trigger-contract-derived.txt"),triggerBodies,StandardCharsets.UTF_8);
        Files.write(Path.of("target/d28-derived-contract.txt"),actual,StandardCharsets.UTF_8);
        Files.writeString(Path.of("target/d28-metadata.txt"),"version="+jdbc.queryForObject("SELECT VERSION()",String.class)+"\n"+jdbc.queryForList("SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,EXTRA,GENERATION_EXPRESSION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND IS_GENERATED='ALWAYS'")+"\nCHECKS="+jdbc.queryForList("SELECT TABLE_NAME,CONSTRAINT_NAME,CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE()"),StandardCharsets.UTF_8);
        var expected=Files.readAllLines(Path.of("scripts/schema-required.txt")).stream().filter(s->s.startsWith("CHECKDEF|")||s.startsWith("GENERATEDDEF|")).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(actual,expected,"Missing or surplus definition contracts");
        assertEquals(15,actual.stream().filter(s->s.startsWith("CHECKDEF|")).count());
        assertEquals(1,actual.stream().filter(s->s.startsWith("GENERATEDDEF|")).count());
        var expectedTriggers=Files.readAllLines(Path.of("scripts/schema-required.txt")).stream().filter(s->s.startsWith("TRIGGERDEF|")).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(5,triggerBodies.size());
        assertEquals(triggerBodies,expectedTriggers,"Missing or surplus trigger body contracts");
    }
    @Test @DisplayName("D-33 DATA-02 exact contract covers charset and collation of every text column")
    void completeTextContract() throws Exception {
        var actual=new TreeSet<>(jdbc.queryForList("SELECT CONCAT('TEXTDEF|',TABLE_NAME,'|',COLUMN_NAME,'|',CHARACTER_SET_NAME,'|',COLLATION_NAME) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('app_user','project_group','group_member','task','invitation','task_event','task_comment','task_comment_revision','comment_mention','notification','task_attachment','task_pin','notification_preference','push_preference','push_device','push_delivery','SPRING_SESSION','SPRING_SESSION_ATTRIBUTES') AND CHARACTER_SET_NAME IS NOT NULL",String.class));
        Files.write(Path.of("target/d28-text-contract-derived.txt"),actual,StandardCharsets.UTF_8);
        var expected=Files.readAllLines(Path.of("scripts/schema-required.txt")).stream().filter(s->s.startsWith("TEXTDEF|")).collect(Collectors.toCollection(TreeSet::new));
        assertFalse(actual.isEmpty());
        assertEquals(actual,expected,"Missing or surplus text charset/collation contracts");
    }
    @Test @DisplayName("F-04 DATA-02 exact contract covers type, length, precision, nullability, default and extra for all columns")
    void completeColumnContract() throws Exception {
        var actual=new TreeSet<>(jdbc.queryForList("SELECT CONCAT('COLDEF|',TABLE_NAME,'|',COLUMN_NAME,'|',COLUMN_TYPE,'|',DATA_TYPE,'|',COALESCE(CAST(CHARACTER_MAXIMUM_LENGTH AS CHAR),'<NULL>'),'|',COALESCE(CAST(NUMERIC_PRECISION AS CHAR),'<NULL>'),'|',COALESCE(CAST(NUMERIC_SCALE AS CHAR),'<NULL>'),'|',COALESCE(CAST(DATETIME_PRECISION AS CHAR),'<NULL>'),'|',IS_NULLABLE,'|',REPLACE(COALESCE(COLUMN_DEFAULT,'<NULL>'),'|','<PIPE>'),'|',EXTRA) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('app_user','project_group','group_member','task','invitation','task_event','task_comment','task_comment_revision','comment_mention','notification','task_attachment','task_pin','notification_preference','push_preference','push_device','push_delivery','SPRING_SESSION','SPRING_SESSION_ATTRIBUTES')",String.class));
        Files.write(Path.of("target/f04-column-contract-derived.txt"),actual,StandardCharsets.UTF_8);
        var expected=Files.readAllLines(Path.of("scripts/schema-required.txt")).stream().filter(s->s.startsWith("COLDEF|")).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(152,actual.size());
        assertEquals(actual,expected,"Missing or surplus column definitions");
    }
    ExecResult shell(String command) throws Exception {
        return DB.execInContainer("bash","-lc","set -euo pipefail; export DB_HOST=127.0.0.1 DB_USER=root DB_NAME="+source+" DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR="+directory+"/backups BACKUP_KEEP=1; "+command);
    }
    String script(String name){return "bash "+directory+"/project/scripts/"+name+".sh";}
    String backup() throws Exception {
        var result=shell(script("backup"));
        assertEquals(0,result.getExitCode(),result.getStderr());
        return result.getStdout().lines().filter(s->s.startsWith("BACKUP_FILE=")).findFirst().orElseThrow().substring(12);
    }
    String artifacts() throws Exception {
        var result=shell("find \"$BACKUP_DIR\" -type f -exec sha256sum {} \\; | sort");
        assertEquals(0,result.getExitCode(),result.getStderr());return result.getStdout();
    }
    String snapshot() throws Exception {
        var result=shell("source "+directory+"/project/scripts/checksum.lib.sh; sql=(mariadb -h127.0.0.1 -uroot --batch --skip-column-names); export MYSQL_PWD=\"$DB_PASSWORD\"; for table in app_user project_group group_member task; do table_checksum \"$DB_NAME\" \"$table\"; done");
        assertEquals(0,result.getExitCode(),result.getStderr());return result.getStdout();
    }
    String tamper(String file,String ddl) throws Exception {
        String changed=directory+"/changed.sql.gz";
        DB.copyFileToContainer(Transferable.of((ddl+"\n").getBytes(StandardCharsets.UTF_8),0600),directory+"/mutation.sql");
        var result=shell("{ gzip -dc '"+file+"'; cat "+directory+"/mutation.sql; } | gzip > '"+changed+"'; sha256sum '"+changed+"' | cut -d ' ' -f 1 > '"+changed+".sha256'; cp '"+file+".tables.tsv' '"+changed+".tables.tsv'");
        assertEquals(0,result.getExitCode(),result.getStderr());return changed;
    }
    String target(){return "deadline_restore_codex_d28_"+UUID.randomUUID().toString().replace("-","");}
    ExecResult restore(String file,String db) throws Exception {
        return shell("export BACKUP_FILE='"+file+"' RESTORE_DB="+db+"; "+script("verify-restore"));
    }
    @ParameterizedTest @ValueSource(strings={"noop","near","values","same","collation","length","default","triggerNoop"})
    @DisplayName("D-28 DATA-08 UAT-14 restore rejects definition drift before row checksums; source and good backup unchanged")
    void restoreRejects(String kind) throws Exception {
        String file=backup(),beforeArtifacts=artifacts(),beforeRows=snapshot();
        String changed=tamper(file,mutation(kind)),db=target();
        var result=restore(changed,db);
        assertAll(()->assertNotEquals(0,result.getExitCode(),"Restore accepted "+kind),
            ()->assertFalse(result.getStdout().contains("RESTORE_OK")),
            ()->assertFalse(result.getStdout().contains("BACKUP_FILE=")),
            ()->assertTrue(result.getStderr().contains(object(kind)),result.getStderr()),
            ()->assertFalse(result.getStderr().contains("Mismatch:"),"Must fail on definition, not data checksum"),
            ()->assertEquals(beforeArtifacts,artifacts()),()->assertEquals(beforeRows,snapshot()));
        if(kind.equals("same")) {
            var resultSame=shell("source "+directory+"/project/scripts/checksum.lib.sh; sql=(mariadb -h127.0.0.1 -uroot --batch --skip-column-names); export MYSQL_PWD=\"$DB_PASSWORD\"; test \"$(table_checksum '"+source+"' group_member)\" = \"$(table_checksum '"+db+"' group_member)\"");
            assertEquals(0,resultSame.getExitCode(),"+0 fixture must have identical logical data: "+resultSame.getStderr());
        }
    }
    @ParameterizedTest @ValueSource(strings={"noop","same","newCheck","newGenerated","collation","length","default","triggerNoop"})
    @DisplayName("D-28 DATA-08 backup refuses drift or unknown definitions without publication or retention")
    void backupRejects(String kind) throws Exception {
        backup();String before=artifacts();jdbc.execute(mutation(kind));
        var result=shell(script("backup"));
        assertAll(()->assertNotEquals(0,result.getExitCode(),"Backup accepted "+kind),
            ()->assertFalse(result.getStdout().contains("BACKUP_FILE=")),
            ()->assertTrue(result.getStderr().contains(object(kind)),result.getStderr()),
            ()->assertEquals(before,artifacts(),"All files in the good set must survive unchanged"));
    }
    @Test @DisplayName("D-28 DATA-08 normal backup and restore accept exactly recreated definitions")
    void exactRecreationRestores() throws Exception {
        recreateOriginal();String file=backup();var result=restore(file,target());
        assertEquals(0,result.getExitCode(),result.getStderr());
        assertTrue(result.getStdout().contains("RESTORE_OK"));
    }
    @Test @DisplayName("D-28 DATA-08 even a changed dump cannot overwrite an existing restore target")
    void existingTargetUnchanged() throws Exception {
        String file=tamper(backup(),mutation("noop")),db=target();
        var created=shell("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -e 'CREATE DATABASE "+db+"; CREATE TABLE "+db+".sentinel(id INT); INSERT INTO "+db+".sentinel VALUES(28)'");
        assertEquals(0,created.getExitCode(),created.getStderr());
        var result=restore(file,db);
        assertNotEquals(0,result.getExitCode());assertTrue(result.getStderr().contains("already exists"));
        var sentinel=shell("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot --batch --skip-column-names -e 'SELECT id FROM "+db+".sentinel'");
        assertEquals(0,sentinel.getExitCode());assertEquals("28",sentinel.getStdout().trim());
    }
}
