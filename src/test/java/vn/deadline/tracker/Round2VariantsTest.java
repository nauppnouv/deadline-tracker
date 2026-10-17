package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.images.builder.Transferable;
import vn.deadline.tracker.service.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="server.address=127.0.0.1")
@AutoConfigureMockMvc
class Round2VariantsTest extends DatabaseTest {
    @Autowired AccountService accounts; @Autowired WorkService work; @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc; @Autowired PasswordEncoder encoder;
    @Value("${local.server.port}") int port;
    static final String PASSWORD="Round two synthetic passphrase 123!";
    String name,actor,group,path,dir; jakarta.servlet.http.Cookie cookie;
    @BeforeEach void fixture() throws Exception {
        name="v"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        actor=accounts.register(name,"Round two",PASSWORD);
        group=work.createGroup(actor,"Round two group","").getId();path="/groups/"+group+"/settings";
        cookie=mvc.perform(post("/login").with(csrf()).param("username",name).param("password",PASSWORD)).andReturn().getResponse().getCookie("SESSION");
        dir="/tmp/round2-"+UUID.randomUUID();
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),"/deadline-project/scripts");
    }
    String env(){return "export DB_HOST=127.0.0.1 DB_USER=root DB_NAME=deadline_test DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR="+dir+"/backups BACKUP_KEEP=1; ";}
    org.testcontainers.containers.Container.ExecResult shell(String s)throws Exception{return DB.execInContainer("bash","-lc",env()+s);}
    String backup()throws Exception {
        var r=shell("bash /deadline-project/scripts/backup.sh");assertEquals(0,r.getExitCode(),r.getStderr());
        return r.getStdout().lines().filter(s->s.startsWith("BACKUP_FILE=")).findFirst().orElseThrow().substring(12);
    }
    String restoreDb(){return "deadline_restore_r2_"+UUID.randomUUID().toString().replace("-","");}
    JdbcTemplate connect(String db)throws Exception {
        var r=shell("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -e \"GRANT ALL ON \\`"+db+"\\`.* TO 'deadline_test'@'%'\"");assertEquals(0,r.getExitCode(),r.getStderr());
        return new JdbcTemplate(new DriverManagerDataSource(DB.getJdbcUrl().replace("/deadline_test","/"+db),DB.getUsername(),DB.getPassword()));
    }
    void wrapper(String executable,String contents)throws Exception {
        DB.copyFileToContainer(Transferable.of(("#!/usr/bin/env bash\n"+contents+"\n").getBytes(StandardCharsets.UTF_8),0700),dir+"/bin/"+executable);
    }
    @Test @DisplayName("R2 DATA-08 restore should detect missing Owner triggers even when logical rows are unchanged")
    void missingTriggerRestore()throws Exception {
        String file=backup();
        // A valid SQL dump with unchanged logical data but no triggers (not invalid SQL).
        var strip=shell("MYSQL_PWD=\"$DB_PASSWORD\" mariadb-dump -h127.0.0.1 -uroot --single-transaction --routines --skip-triggers --hex-blob deadline_test | gzip > '"+file+"'; sha256sum '"+file+"' | cut -d ' ' -f 1 > '"+file+".sha256'");
        assertEquals(0,strip.getExitCode(),strip.getStderr());
        String db=restoreDb();
        var r=shell("export RESTORE_DB="+db+" BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh");
        System.out.println("R2 missing triggers restore exit="+r.getExitCode()+" output="+r.getStdout()+" stderr="+r.getStderr());
        assertNotEquals(0,r.getExitCode(),"Restoration must detect removed integrity triggers");
        assertFalse(r.getStdout().contains("RESTORE_OK"));
        assertTrue(r.getStderr().contains("tr_group_create_owner"),r.getStderr());
    }
    @Test @DisplayName("R2 DATA-08 backup principal without TRIGGER must fail rather than publish incomplete schema")
    void missingTriggerPrivilege()throws Exception {
        String precedingGood=backup();
        String backupUser="b"+UUID.randomUUID().toString().replace("-","").substring(0,16);
        String pwd=UUID.randomUUID().toString();
        var grant=shell("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -e \"CREATE USER '"+backupUser+"'@'127.0.0.1' IDENTIFIED BY '"+pwd+"'; GRANT SELECT, SHOW VIEW ON deadline_test.* TO '"+backupUser+"'@'127.0.0.1'; GRANT SELECT ON mysql.proc TO '"+backupUser+"'@'127.0.0.1'\"");assertEquals(0,grant.getExitCode(),grant.getStderr());
        var b=shell("export DB_USER="+backupUser+" DB_PASSWORD='"+pwd+"'; bash /deadline-project/scripts/backup.sh");
        System.out.println("R2 no TRIGGER privilege backup exit="+b.getExitCode()+" stderr="+b.getStderr());
        assertNotEquals(0,b.getExitCode(),"No TRIGGER privilege must fail before publication");
        assertFalse(b.getStdout().contains("BACKUP_FILE="));
        assertEquals(0,shell("test -s '"+precedingGood+"' && test -s '"+precedingGood+".sha256' && test -s '"+precedingGood+".tables.tsv'").getExitCode());
        var grantTrigger=shell("MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -e \"GRANT TRIGGER ON deadline_test.* TO '"+backupUser+"'@'127.0.0.1'\"");
        assertEquals(0,grantTrigger.getExitCode(),grantTrigger.getStderr());
        var permitted=shell("export DB_USER="+backupUser+" DB_PASSWORD='"+pwd+"'; bash /deadline-project/scripts/backup.sh");
        assertEquals(0,permitted.getExitCode(),permitted.getStderr());
        assertTrue(permitted.getStdout().contains("BACKUP_FILE="));
    }
    @Test @DisplayName("UAT-14 DATA-08 R2-01 dump silently skipping triggers keeps the last good backup")
    void missingDumpTriggers()throws Exception {
        String good=backup();
        wrapper("mariadb-dump","exec /usr/bin/mariadb-dump \"$@\" --skip-triggers");
        var result=shell("export PATH="+dir+"/bin:$PATH; bash /deadline-project/scripts/backup.sh");
        assertNotEquals(0,result.getExitCode());assertFalse(result.getStdout().contains("BACKUP_FILE="));
        assertTrue(result.getStderr().contains("CREATE TRIGGER"),result.getStderr());
        assertEquals(0,shell("test -s '"+good+"' && test -s '"+good+".sha256' && test -s '"+good+".tables.tsv'").getExitCode());
    }
    @Test @DisplayName("UAT-14 DATA-08 R2-01 retention keeps the verified new set despite future old mtime")
    void retentionClockSkew()throws Exception {
        String old=backup();
        assertEquals(0,shell("touch -d '2035-01-01' '"+old+"'").getExitCode());
        String latest=backup();
        assertEquals(0,shell("test -s '"+latest+"' && test -s '"+latest+".sha256' && test -s '"+latest+".tables.tsv'").getExitCode());
    }
    @Test @DisplayName("UAT-14 DATA-08 R2-01 concurrent retention must never remove all verified backups")
    void concurrentRetention()throws Exception {
        String good=backup();assertEquals(0,shell("touch -d '2000-01-01' '"+good+"'").getExitCode());
        // Hold both retention inventories at a barrier to reproduce cross-deletion.
        // With serialization only one reaches find; bounded wait then allows it through.
        wrapper("find","""
            set -e
            /usr/bin/find "$@" > "BARRIER/${BACKUP_JOB}.inventory"
            touch "BARRIER/${BACKUP_JOB}.ready"
            other=A; [[ "$BACKUP_JOB" == A ]] && other=B
            for ((n=0;n<250;n++)); do
              [[ -f "BARRIER/$other.ready" ]] && break
              sleep 0.02
            done
            cat "BARRIER/${BACKUP_JOB}.inventory"
            """.replace("BARRIER",dir));
        var concurrent=shell("export PATH="+dir+"/bin:$PATH; BACKUP_JOB=A bash /deadline-project/scripts/backup.sh > "+dir+"/A.out 2> "+dir+"/A.err & a=$!; BACKUP_JOB=B bash /deadline-project/scripts/backup.sh > "+dir+"/B.out 2> "+dir+"/B.err & b=$!; wait $a; ra=$?; wait $b; rb=$?; printf 'A=%s B=%s\\n' $ra $rb; cat "+dir+"/A.out "+dir+"/B.out "+dir+"/A.err "+dir+"/B.err");
        assertEquals(0,concurrent.getExitCode(),concurrent.getStderr());
        var count=shell("/usr/bin/find "+dir+"/backups -maxdepth 1 -name 'deadline-*.sql.gz' -type f | wc -l");
        int retained=Integer.parseInt(count.getStdout().strip());
        System.out.println("R2 concurrent retention retained="+retained+" "+concurrent.getStdout());
        assertTrue(retained>=1,"Concurrent retention removed every verified backup: "+concurrent.getStdout());
        String next=backup();assertEquals(0,shell("test -s '"+next+"'").getExitCode(),"Lock must be released after successful backup");
    }
    @ParameterizedTest @ValueSource(strings={"tr_group_create_owner","tr_member_owner_update"})
    @DisplayName("UAT-14 DATA-02/08 R2-01 inventory alone is insufficient: restore probes trigger behavior")
    void noopTriggerRestore(String trigger)throws Exception {
        String file=backup();
        String ddl="DROP TRIGGER "+trigger+"; CREATE TRIGGER "+trigger+(trigger.equals("tr_group_create_owner")?" AFTER INSERT ON project_group":" BEFORE UPDATE ON group_member")+" FOR EACH ROW SET @r2_noop=1;";
        var changed=shell("{ gzip -dc '"+file+"'; echo '"+ddl+"'; } | gzip > '"+file+".new'; mv '"+file+".new' '"+file+"'; sha256sum '"+file+"' | cut -d ' ' -f 1 > '"+file+".sha256'");
        assertEquals(0,changed.getExitCode(),changed.getStderr());
        var result=shell("export RESTORE_DB="+restoreDb()+" BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh");
        assertNotEquals(0,result.getExitCode());assertFalse(result.getStdout().contains("RESTORE_OK"));
        assertTrue(result.getStdout().contains("SCHEMA_OK"),result.getStderr());
        assertTrue(result.getStderr().contains("Restore probe:"),result.getStderr());
    }
    @ParameterizedTest @ValueSource(strings={"fk_task_assignee","ck_task_status","uq_one_owner"})
    @DisplayName("UAT-14 DATA-02/08 R2-01 restore inventories FK CHECK and unique even with unchanged rows")
    void missingConstraintRestore(String constraint)throws Exception {
        String file=backup(),table=constraint.equals("uq_one_owner")?"group_member":"task";
        var changed=shell("{ gzip -dc '"+file+"'; echo 'ALTER TABLE "+table+" DROP CONSTRAINT "+constraint+";'; } | gzip > '"+file+".new'; mv '"+file+".new' '"+file+"'; sha256sum '"+file+"' | cut -d ' ' -f 1 > '"+file+".sha256'");
        assertEquals(0,changed.getExitCode(),changed.getStderr());
        var result=shell("export RESTORE_DB="+restoreDb()+" BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh");
        assertNotEquals(0,result.getExitCode());assertFalse(result.getStdout().contains("RESTORE_OK"));
        assertTrue(result.getStderr().contains(constraint),result.getStderr());
    }
    @ParameterizedTest @ValueSource(strings={"application/*","*/*"})
    @DisplayName("SEC-14 R2-04 wildcard Content-Type is rejected without mutating data")
    void wildcardType(String type)throws Exception {
        var client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
        var page=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Cookie","SESSION="+cookie.getValue()).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,page.statusCode());
        String version=HtmlForms.version(page.body(),path);
        var csrf=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page.body());assertTrue(csrf.find());
        String token=org.springframework.web.util.HtmlUtils.htmlUnescape(csrf.group(1));
        String body="name=Wildcard-mutated&description=&version="+version+"&_csrf="+URLEncoder.encode(token,StandardCharsets.UTF_8);
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Cookie","SESSION="+cookie.getValue()).header("Content-Type",type).POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals("Round two group",work.group(actor,group).getName());
        System.out.println("R2 wildcard="+type+" status="+response.statusCode()+" data unchanged");
        assertEquals(400,response.statusCode());
    }
}
