package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MigrationGuardTest {
    @TempDir Path repo;
    static final String MIGRATIONS="src/main/resources/db/migration/";
    record Result(int code,String text) {}
    Result command(List<String> args,Map<String,String> env)throws Exception {
        var builder=new ProcessBuilder(args).directory(repo.toFile()).redirectErrorStream(true);
        builder.environment().remove("MIGRATION_BASELINE_REF");
        builder.environment().putAll(env);
        var process=builder.start();String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        return new Result(process.waitFor(),output);
    }
    void git(String... args)throws Exception {
        var command=new ArrayList<>(List.of("git"));command.addAll(List.of(args));
        var result=command(command,Map.of());assertEquals(0,result.code(),result.text());
    }
    @BeforeEach void fixture()throws Exception {
        Files.createDirectories(repo.resolve("scripts"));
        for(String name:List.of("check.sh","check-migrations.sh")){
            Path source=Path.of("scripts",name);if(Files.exists(source))Files.copy(source,repo.resolve("scripts/"+name));
        }
        Files.copy(Path.of(".gitleaks.toml"),repo.resolve(".gitleaks.toml"));
        // Isolate the Git gate through the real check.sh; other gates run in full check.
        Files.writeString(repo.resolve("scripts/mvn.sh"),"#!/usr/bin/env bash\nexit 0\n");
        Files.createDirectories(repo.resolve(MIGRATIONS));
        Files.writeString(repo.resolve(MIGRATIONS+"V1__base.sql"),"CREATE TABLE fixture(id INT);\n");
        git("init","-b","baseline");git("config","user.name","Migration fixture");git("config","user.email","fixture@example.invalid");
        git("add",".");git("commit","-m","baseline");git("update-ref","refs/remotes/origin/main","HEAD");
    }
    Result check(String ref,boolean failDiff)throws Exception {
        String bash=Files.exists(Path.of("C:/Program Files/Git/bin/bash.exe"))?"C:/Program Files/Git/bin/bash.exe":"bash";
        var env=new HashMap<String,String>();env.put("RUN_DEPENDENCY_AUDIT","0");env.put("AUDIT_SKIP_REASON","isolated migration guard fixture");
        if(ref!=null)env.put("MIGRATION_BASELINE_REF",ref);
        String script=failDiff?"git() { if [[ $1 == diff ]]; then echo injected-git-diff-error >&2; return 42; else command git \"$@\"; fi; }; export -f git; bash scripts/check.sh":"bash scripts/check.sh";
        return command(List.of(bash,"-c",script),env);
    }
    @ParameterizedTest @ValueSource(strings={"committed","dirty","staged","deleted","renamed"})
    @DisplayName("DATA-01 R2-03 real Git baseline rejects old migration M D R including committed edits")
    void changedOldMigration(String change)throws Exception {
        Path old=repo.resolve(MIGRATIONS+"V1__base.sql");
        switch(change){
            case "deleted" -> git("rm",MIGRATIONS+"V1__base.sql");
            case "renamed" -> git("mv",MIGRATIONS+"V1__base.sql",MIGRATIONS+"V1__renamed.sql");
            default -> Files.writeString(old,"-- changed old migration\n",StandardOpenOption.APPEND);
        }
        if(!change.equals("dirty")){git("add",".");if(!change.equals("staged"))git("commit","-m","change old migration");}
        var result=check(null,false);assertNotEquals(0,result.code(),result.text());
        assertTrue(result.text().contains("FAIL"),result.text());
    }
    @ParameterizedTest @ValueSource(strings={"staged","committed"})
    @DisplayName("REV-02 DATA-01 changed migration index cannot hide behind baseline working bytes")
    void indexDiffersFromWorkingTree(String state)throws Exception {
        Path old=repo.resolve(MIGRATIONS+"V1__base.sql");
        byte[] baseline=Files.readAllBytes(old);
        Files.writeString(old,"-- indexed historical modification\n",StandardOpenOption.APPEND);
        git("add",MIGRATIONS+"V1__base.sql");
        if(state.equals("committed"))git("commit","-m","indexed historical edit");
        Files.write(old,baseline);
        assertEquals(0,command(List.of("git","diff","--quiet","origin/main","--",MIGRATIONS),Map.of()).code(),"Worktree must match pinned baseline");
        assertEquals(1,command(List.of("git","diff","--cached","--quiet","origin/main","--",MIGRATIONS),Map.of()).code(),"Index must still contain modified V1");
        var result=check(null,false);
        assertNotEquals(0,result.code(),"Guard accepted changed "+state+" index with clean baseline working bytes: "+result.text());
        assertTrue(result.text().contains("index"),result.text());
        assertTrue(result.text().contains("FAIL"),result.text());
    }

    @Test @DisplayName("REV-02 DATA-01 Git index diff error fails closed independently of worktree diff")
    void indexGitFailure()throws Exception {
        String bash=Files.exists(Path.of("C:/Program Files/Git/bin/bash.exe"))?"C:/Program Files/Git/bin/bash.exe":"bash";
        String script="""
            git() {
              if [[ $1 == diff ]]; then
                for argument in "$@"; do
                  if [[ "$argument" == --cached ]]; then echo injected-index-diff-error >&2; return 42; fi
                done
              fi
              command git "$@"
            }
            export -f git
            bash scripts/check.sh
            """;
        var result=command(List.of(bash,"-c",script),Map.of("RUN_DEPENDENCY_AUDIT","0","AUDIT_SKIP_REASON","isolated migration guard fixture"));
        assertNotEquals(0,result.code(),"Index diff failure must reject check: "+result.text());
        assertTrue(result.text().contains("injected-index-diff-error"),result.text());
    }
    @Test @DisplayName("DATA-01 R2-03 only new V5 can be added against an existing baseline")
    void newMigration()throws Exception {
        Files.writeString(repo.resolve(MIGRATIONS+"V5__new.sql"),"CREATE TABLE newer(id INT);\n");git("add",".");git("commit","-m","new migration");
        var result=check("origin/main",false);assertEquals(0,result.code(),result.text());
    }
    @ParameterizedTest @ValueSource(strings={"missing-ref",""})
    @DisplayName("DATA-01 R2-03 absent or empty baseline ref fails explicitly")
    void missingBaseline(String ref)throws Exception {
        var result=check(ref,false);assertNotEquals(0,result.code(),result.text());assertTrue(result.text().contains("FAIL"),result.text());
    }
    @Test @DisplayName("DATA-01 R2-03 Git diff error cannot silently pass")
    void gitFailure()throws Exception {
        var result=check("baseline",true);assertNotEquals(0,result.code(),result.text());assertTrue(result.text().contains("injected-git-diff-error"),result.text());
    }
    @Test @DisplayName("DATA-01 R2-03 explicit none reports BỎ QUA instead of claiming immutability")
    void explicitNone()throws Exception {
        var result=check("none",false);assertEquals(0,result.code(),result.text());assertTrue(result.text().contains("BỎ QUA"),result.text());
        assertFalse(result.text().contains("OK immutable committed migrations"),result.text());
    }
}
