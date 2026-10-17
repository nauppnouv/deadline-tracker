package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.utility.MountableFile;
import vn.deadline.tracker.service.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc
class RestoredSessionTest extends DatabaseTest {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired WorkService work;
    @Autowired InvitationService invites;
    @Test @DisplayName("P7-02 SEC-06 UAT-01/13/14 restore must not revive a logged-out cookie")
    void loggedOutCookieStaysInvalidAfterRestore()throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","");
        String username="r"+suffix.substring(0,20), password="Synthetic restored session passphrase 123!";
        String actor=accounts.register(username,"Restored owner",password);
        String group=work.createGroup(actor,"Restored session group","").getId();
        String member=accounts.register("m"+suffix.substring(0,20),"Restored member",password);
        invites.join(member,invites.issue(actor,group));
        var login=mvc.perform(post("/login").with(csrf()).param("username",username).param("password",password)).andReturn();
        assertEquals(302,login.getResponse().getStatus());
        var cookie=login.getResponse().getCookie("SESSION");assertNotNull(cookie);
        assertEquals(200,mvc.perform(get("/account").cookie(cookie)).andReturn().getResponse().getStatus());
        DB.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts").toAbsolutePath()),"/deadline-project/scripts");
        String env="export DB_HOST=127.0.0.1 DB_USER=root DB_NAME=deadline_test DB_PASSWORD=\"${MARIADB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}\" BACKUP_DIR=/tmp/session-backup-"+suffix+"; ";
        var backup=DB.execInContainer("bash","-lc",env+"bash /deadline-project/scripts/backup.sh");
        assertEquals(0,backup.getExitCode(),backup.getStderr());
        String file=backup.getStdout().lines().filter(s->s.startsWith("BACKUP_FILE=")).findFirst().orElseThrow().substring(12);
        assertEquals(302,mvc.perform(post("/logout").cookie(cookie).with(csrf())).andReturn().getResponse().getStatus());
        assertEquals(302,mvc.perform(get("/account").cookie(cookie)).andReturn().getResponse().getStatus());
        String restoredDb="deadline_restore_session_"+suffix;
        var verified=DB.execInContainer("bash","-lc",env+"export RESTORE_DB="+restoredDb+" BACKUP_FILE='"+file+"'; bash /deadline-project/scripts/verify-restore.sh");
        if(verified.getExitCode()!=0) {
            String select="SELECT id,group_id,user_id,role,DATE_FORMAT(joined_at,'%Y-%m-%d %H:%i:%s.%f'),owner_marker FROM group_member ORDER BY id";
            var source=DB.execInContainer("bash","-lc",env+"MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -N -B deadline_test -e \""+select+"\"");
            var copy=DB.execInContainer("bash","-lc",env+"MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -N -B "+restoredDb+" -e \""+select+"\"");
            java.nio.file.Files.writeString(Path.of("target/p7-02-checksum-diagnostic.txt"),
                "sourceExit="+source.getExitCode()+" restoredExit="+copy.getExitCode()+"\n"+
                "SOURCE\n"+source.getStdout()+"RESTORED\n"+copy.getStdout());
        }
        assertEquals(0,verified.getExitCode(),verified.getStderr());
        var grant=DB.execInContainer("bash","-lc",env+"MYSQL_PWD=\"$DB_PASSWORD\" mariadb -h127.0.0.1 -uroot -e \"GRANT ALL ON \\`"+restoredDb+"\\`.* TO 'deadline_test'@'%'\"");
        assertEquals(0,grant.getExitCode(),grant.getStderr());
        try(var restored=new SpringApplicationBuilder(DeadlineTrackerApplication.class).profiles("test")
            .initializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("restored-session-db",Map.of(
                "spring.datasource.url",DB.getJdbcUrl().replace("/deadline_test","/"+restoredDb),
                "spring.datasource.username",DB.getUsername(),"spring.datasource.password",DB.getPassword(),
                "server.address","127.0.0.1","server.port","0","spring.session.jdbc.cleanup-cron","-")))).run()) {
            var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            String port=restored.getEnvironment().getProperty("local.server.port");assertNotNull(port);
            var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/account"))
                .header("Cookie","SESSION="+cookie.getValue()).GET().build(),HttpResponse.BodyHandlers.discarding());
            assertAll(
                ()->assertEquals(302,response.statusCode(),"Old logged-out cookie must not authenticate after restore"),
                ()->assertEquals(0,restored.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?",Integer.class,username)));
            assertEquals("Restored session group",restored.getBean(WorkService.class).group(actor,group).getName());
        }
    }
}
