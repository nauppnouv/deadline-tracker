package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackupVerifiedRetentionTest extends DatabaseTest {
    @Autowired JdbcTemplate jdbc;
    String directory;
    @BeforeEach void fixture() throws Exception {
        directory="/tmp/f07-"+UUID.randomUUID().toString().replace("-","");
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),directory+"/project/scripts");
        String id=UUID.randomUUID().toString(),group=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO app_user(id,username,display_name,password_hash,created_at) VALUES(?,?,?,'unusable',UTC_TIMESTAMP(6))",id,"f07."+id.replace("-","").substring(0,20),"F07 fixture");
        jdbc.update("INSERT INTO project_group(id,name,description,owner_id,created_at) VALUES(?,'F07 fixture','',?,UTC_TIMESTAMP(6))",group,id);
    }
    ExecResult shell(String command)throws Exception {
        return DB.execInContainer("bash","-lc","export DB_HOST=127.0.0.1 DB_USER=root DB_NAME=deadline_test DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR="+directory+"/backups BACKUP_KEEP=1; "+command);
    }
    String script(String name){return "bash "+directory+"/project/scripts/"+name+".sh";}
    String field(ExecResult result,String key){return result.getStdout().lines().filter(s->s.startsWith(key+"=")).findFirst().orElseThrow(()->new AssertionError("Missing "+key+" in script output")).substring(key.length()+1);}
    String backup()throws Exception {var r=shell(script("backup"));assertEquals(0,r.getExitCode(),r.getStderr());return field(r,"BACKUP_FILE");}
    ExecResult restore(String file)throws Exception {return restore(file,"");}
    ExecResult restore(String file,String prefix)throws Exception {
        return shell(prefix+" export BACKUP_FILE='"+file+"' RESTORE_DB=deadline_restore_f07_"+UUID.randomUUID().toString().replace("-","")+"; "+script("verify-restore"));
    }
    String verify(String file)throws Exception {var r=restore(file);assertEquals(0,r.getExitCode(),r.getStderr());assertTrue(r.getStdout().contains("RESTORE_OK"));return field(r,"VERIFIED_BACKUP_FILE");}
    String hashes(String file)throws Exception {
        var r=shell("set -e; for f in '"+file+"' '"+file+".sha256' '"+file+".tables.tsv'; do sha256sum \"$f\"; done");
        assertEquals(0,r.getExitCode(),"All three files of last restore-verified set must remain: "+r.getStderr());return r.getStdout();
    }
    void wrapper(String name,String body)throws Exception {
        DB.copyFileToContainer(Transferable.of(("#!/usr/bin/env bash\nset -euo pipefail\n"+body+"\n").getBytes(StandardCharsets.UTF_8),0700),directory+"/bin/"+name);
    }
    String pointer()throws Exception {var r=shell("cat '"+directory+"/backups/.deadline-verified/current'");assertEquals(0,r.getExitCode(),r.getStderr());return r.getStdout();}

    @ParameterizedTest @ValueSource(strings={"before","after","after-error","after-unreadable"})
    @DisplayName("F-08 DATA-08 UAT-14 interrupted pointer publication preserves a resolvable full restore and automatic retry")
    void interruptedPointerPublicationKeepsCurrent(String phase)throws Exception {
        String pinned=verify(backup()),oldHashes=hashes(pinned),oldPointer=pointer(),candidate=backup();
        var candidateDigests=hashes(candidate).lines().map(line->line.substring(0,64)).toList();
        String marker=directory+"/pointer-renamed";
        String signal="kill -TERM \"$PPID\"; sleep 0.15; exit 0";
        String publication=phase.equals("before")?signal:
            "/usr/bin/mv \"$@\"; touch '"+marker+"'; "+(phase.equals("after-error")?"exit 42":signal);
        wrapper("mv","for arg in \"$@\"; do if [[ \"$arg\" == */current ]]; then "+publication+"; fi; done\nexec /usr/bin/mv \"$@\"");
        if(phase.equals("after-unreadable")) {
            // An I/O-like read failure during EXIT cleanup must retain the
            // generation whose publication cannot be ruled out.
            wrapper("cat","for arg in \"$@\"; do if [[ \"$arg\" == */current && -f '"+marker+"' ]]; then exit 44; fi; done\nexec /usr/bin/cat \"$@\"");
        }
        var interrupted=restore(candidate,"export PATH="+directory+"/bin:$PATH;");
        assertEquals(phase.equals("after-error")?42:143,interrupted.getExitCode(),interrupted.getStderr());
        assertFalse(interrupted.getStdout().contains("RESTORE_OK"));
        assertFalse(interrupted.getStdout().contains("VERIFIED_BACKUP_FILE="));
        assertEquals(oldHashes,hashes(pinned),"Previous full-restore input must remain intact");
        String store=directory+"/backups/.deadline-verified";
        var resolved=shell("source '"+directory+"/project/scripts/verified-backup.lib.sh'; current_verified_backup '"+store+"'");
        assertEquals(0,resolved.getExitCode(),"F-08 current must reference an intact generation after interruption: "+resolved.getStderr());
        String current=resolved.getStdout().strip();
        if(phase.equals("before")) {
            assertEquals(oldPointer,pointer());assertEquals(pinned,current);
            assertEquals("1",shell("find '"+store+"' -maxdepth 1 -type d -name 'verified-*' | wc -l").getStdout().strip());
        } else {
            assertNotEquals(oldPointer,pointer());
            assertEquals(candidateDigests,hashes(current).lines().map(line->line.substring(0,64)).toList());
        }
        assertEquals(0,shell("test ! -e '"+directory+"/backups/.deadline-backup.lock'; test -z \"$(find '"+store+"' -maxdepth 1 \\( -name '.pending-*' -o -name '.current-*' \\))\"").getExitCode());
        String currentHashes=hashes(current);backup();
        assertEquals(currentHashes,hashes(current));assertEquals(oldHashes,hashes(pinned));
        var retry=restore(current);assertEquals(0,retry.getExitCode(),retry.getStderr());
        assertTrue(retry.getStdout().contains("OWNER_BEHAVIOR_OK"));assertTrue(retry.getStdout().contains("RESTORE_OK"));
        System.out.println("F08 phase="+phase+" interrupted_exit="+interrupted.getExitCode()+" current_valid=true old_all3_preserved=true automatic_backup_and_restore_retry=true");
    }

    @ParameterizedTest @ValueSource(strings={"noop","check"})
    @DisplayName("F-07 DATA-08 UAT-14 corrupt emitted dump cannot evict last successful full restore at KEEP=1")
    void badDumpKeepsLastRestoredSet(String kind)throws Exception {
        String good=backup();var verified=restore(good);assertEquals(0,verified.getExitCode(),verified.getStderr());
        assertTrue(verified.getStdout().contains("OWNER_BEHAVIOR_OK"));
        // Before the fix there is no protected copy. Still reach the actual
        // retention failure instead of failing only on a new output marker.
        String protectedFile=verified.getStdout().lines().filter(s->s.startsWith("VERIFIED_BACKUP_FILE=")).map(s->s.substring(21)).findFirst().orElse(good);
        String before=hashes(protectedFile);
        String ddl=kind.equals("noop")?"CREATE OR REPLACE TRIGGER tr_group_create_owner AFTER INSERT ON project_group FOR EACH ROW SET @f07_noop=1":"ALTER TABLE task DROP CONSTRAINT ck_task_status, ADD CONSTRAINT ck_task_status CHECK(1=1)";
        wrapper("mariadb-dump","/usr/bin/mariadb-dump \"$@\"\nprintf '\\nDELIMITER //\\n%s//\\nDELIMITER ;\\n' \""+ddl+"\"");
        var emitted=shell("export PATH="+directory+"/bin:$PATH; "+script("backup"));
        assertEquals(0,emitted.getExitCode(),"Candidate creation remains possible without granting backup principal restore DDL");
        String bad=field(emitted,"BACKUP_FILE");
        assertEquals(before,hashes(protectedFile),"Candidate retention must preserve the full restore-verified set");
        var denied=restore(bad);assertNotEquals(0,denied.getExitCode());assertFalse(denied.getStdout().contains("RESTORE_OK"));
        assertFalse(denied.getStdout().contains("VERIFIED_BACKUP_FILE="));
        assertTrue(denied.getStderr().contains("Missing schema invariant:"),denied.getStderr());
        assertTrue(denied.getStderr().contains(kind.equals("noop")?"tr_group_create_owner":"ck_task_status"),denied.getStderr());
        assertEquals(before,hashes(protectedFile));
        var recovered=restore(protectedFile);assertEquals(0,recovered.getExitCode(),recovered.getStderr());
        assertTrue(recovered.getStdout().contains("OWNER_BEHAVIOR_OK"));assertTrue(recovered.getStdout().contains("RESTORE_OK"));
        System.out.println("F07 "+kind+" candidate_published=true prior_full_restore_copy_preserved=true bad_restore_schema_rejected=true protected_copy_restores=true");
    }

    @Test @DisplayName("F-07 last full restore copy is separate from candidate retention and rotates only after full verification")
    void successfulPromotionAndProtectedRestore()throws Exception {
        String first=backup(),protectedFirst=verify(first),before=hashes(protectedFirst);
        assertTrue(protectedFirst.startsWith(directory+"/backups/.deadline-verified/verified-"));
        assertEquals("600",shell("stat -c '%a' '"+protectedFirst+"'").getStdout().strip());
        for(int n=0;n<3;n++)backup();
        assertEquals(before,hashes(protectedFirst));
        assertEquals("1",shell("find '"+directory+"/backups' -maxdepth 1 -name 'deadline-*.sql.gz' -type f | wc -l").getStdout().strip());
        String protectedNext=verify(protectedFirst);assertNotEquals(protectedFirst,protectedNext);
        assertEquals("1",shell("find '"+directory+"/backups/.deadline-verified' -maxdepth 1 -type d -name 'verified-*' | wc -l").getStdout().strip());
        assertEquals(0,shell("test ! -e '"+protectedFirst+"'; test -s '"+protectedNext+"'; test ! -d '"+protectedNext.substring(0,protectedNext.lastIndexOf('/'))+"/.deadline-verified'").getExitCode());
        var finalRestore=restore(protectedNext);assertEquals(0,finalRestore.getExitCode(),finalRestore.getStderr());
    }

    @ParameterizedTest @ValueSource(strings={"cp","mv"})
    @DisplayName("F-07 failed protected-copy or current-pointer publication retains previous verified bytes")
    void failedPromotionRetainsPrevious(String executable)throws Exception {
        String pinned=verify(backup()),before=hashes(pinned),current=pointer(),candidate=backup();
        String body=executable.equals("cp")?"exit 42":"for a in \"$@\"; do [[ \"$a\" == */current ]] && exit 42; done\nexec /usr/bin/mv \"$@\"";
        wrapper(executable,body);
        var failed=restore(candidate,"export PATH="+directory+"/bin:$PATH;");
        assertNotEquals(0,failed.getExitCode());assertFalse(failed.getStdout().contains("RESTORE_OK"));
        assertFalse(failed.getStdout().contains("VERIFIED_BACKUP_FILE="));
        assertEquals(before,hashes(pinned));assertEquals(current,pointer());
        assertEquals(0,shell("test ! -e '"+directory+"/backups/.deadline-backup.lock'; test -z \"$(find '"+directory+"/backups/.deadline-verified' -maxdepth 1 -name '.pending-*' -o -name '.current-*')\"").getExitCode());
        verify(candidate);
    }

    @Test @DisplayName("F-07 backup and full restore promotion serialize on the same lock")
    void backupCannotPruneWhileRestorePromotes()throws Exception {
        String pinned=verify(backup()),before=hashes(pinned),candidate=backup();
        DB.copyFileToContainer(Transferable.of(before.getBytes(StandardCharsets.UTF_8),0600),directory+"/protected-before.sha256");
        wrapper("cp","touch '"+directory+"/copy-entered'\nfor ((n=0;n<400;n++)); do [[ -f '"+directory+"/copy-release' ]] && exec /usr/bin/cp \"$@\"; sleep 0.05; done\nexit 43");
        var result=shell("set -e; export BACKUP_FILE='"+candidate+"' RESTORE_DB=deadline_restore_f07_lock_"+UUID.randomUUID().toString().replace("-","")+"; PATH="+directory+"/bin:$PATH "+script("verify-restore")+" > '"+directory+"/restore.out' 2> '"+directory+"/restore.err' & job=$!; for ((n=0;n<400;n++)); do [[ -f '"+directory+"/copy-entered' ]] && break; sleep 0.05; done; test -f '"+directory+"/copy-entered'; set +e; "+script("backup")+" > '"+directory+"/overlap.out' 2> '"+directory+"/overlap.err'; overlap=$?; set -e; test \"$overlap\" != 0; ! grep -q BACKUP_FILE= '"+directory+"/overlap.out'; sha256sum -c '"+directory+"/protected-before.sha256' >/dev/null; touch '"+directory+"/copy-release'; wait \"$job\"; grep -q RESTORE_OK '"+directory+"/restore.out'; echo 'OVERLAP_REFUSED=true RESTORE_COMPLETED=true'");
        assertEquals(0,result.getExitCode(),result.getStderr());assertTrue(result.getStdout().contains("OVERLAP_REFUSED=true"));
        assertNotEquals(pinned,verify(candidate));
    }

    @Test @DisplayName("F-07 malformed current pointer fails closed and never writes outside verified store")
    void invalidPointerDoesNotEscapeStore()throws Exception {
        String pinned=verify(backup()),before=hashes(pinned),candidate=backup();
        assertEquals(0,shell("printf '../../outside\\n' > '"+directory+"/backups/.deadline-verified/current'; printf untouched > '"+directory+"/outside'").getExitCode());
        var denied=restore(candidate);assertNotEquals(0,denied.getExitCode());assertFalse(denied.getStdout().contains("RESTORE_OK"));
        assertEquals(before,hashes(pinned));assertEquals("untouched",shell("cat '"+directory+"/outside'").getStdout());
    }

    @Test @DisplayName("F-07 bootstrap never prunes existing candidates before first successful full-restore receipt")
    void bootstrapDefersRetention()throws Exception {
        String first=backup(),before=hashes(first);backup();
        var third=shell(script("backup"));assertEquals(0,third.getExitCode(),third.getStderr());
        assertTrue(third.getStdout().contains("RETENTION_DEFERRED"));assertEquals(before,hashes(first));
        assertEquals("3",shell("find '"+directory+"/backups' -maxdepth 1 -name 'deadline-*.sql.gz' -type f | wc -l").getStdout().strip());
        verify(field(third,"BACKUP_FILE"));backup();
        assertEquals("1",shell("find '"+directory+"/backups' -maxdepth 1 -name 'deadline-*.sql.gz' -type f | wc -l").getStdout().strip());
    }

    @ParameterizedTest @ValueSource(strings={"bytes","receipt"})
    @DisplayName("F-07 damaged protected bytes or empty receipt hashes block candidate publication and retention")
    void damagedProtectedSetFailsClosed(String kind)throws Exception {
        String pinned=verify(backup()),candidate=backup(),before=hashes(candidate);
        String mutation=kind.equals("bytes")?"printf tamper >> '"+pinned+"'":"printf 'deadline-full-restore-v1\\n\\n\\n\\n2026-10-19T00:00:00Z\\n' > '"+pinned.substring(0,pinned.lastIndexOf('/'))+"/verification.txt'";
        assertEquals(0,shell(mutation).getExitCode());
        var denied=shell(script("backup"));assertNotEquals(0,denied.getExitCode());
        assertFalse(denied.getStdout().contains("BACKUP_FILE="));assertEquals(before,hashes(candidate));
    }
}
