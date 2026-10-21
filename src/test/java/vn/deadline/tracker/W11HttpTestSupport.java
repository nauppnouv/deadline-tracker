package vn.deadline.tracker;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.util.HtmlUtils;
import vn.deadline.tracker.service.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class W11HttpTestSupport extends DatabaseTest {
    static final String PASSWORD="Synthetic W11 regression passphrase 123!";
    @Value("${local.server.port}") int port;
    @Value("${app.attachments.root}") String attachmentRoot;
    @Autowired AccountService accounts;
    @Autowired WorkService work;
    @Autowired InvitationService invitations;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    record User(String id,String username) {}
    @BeforeAll void verifyDisposableMariaDb() throws Exception {
        try(var c=dataSource.getConnection()) {
            // Strip ALL query parameters before comparison or assertion output.
            assertEquals(DB.getJdbcUrl().split("\\?",2)[0],c.getMetaData().getURL().split("\\?",2)[0]);
        }
        String db=jdbc.queryForObject("SELECT DATABASE()",String.class);
        String principal=jdbc.queryForObject("SELECT CURRENT_USER()",String.class);
        String version=jdbc.queryForObject("SELECT VERSION()",String.class);
        assertEquals("deadline_test",db);assertTrue(principal.startsWith("deadline_test@"));
        assertTrue(version.startsWith("11.8.9"));assertFalse(DB.isShouldBeReused());
        System.out.printf("W11 isolation database=%s principal=%s version=%s container=%s jdbc=%s%n",db,principal,version,DB.getContainerId(),DB.getJdbcUrl().split("\\?",2)[0]);
    }
    User user() {String name="w11"+UUID.randomUUID().toString().replace("-", "").substring(0,20);return new User(accounts.register(name,"Synthetic W11 user",PASSWORD),name);}
    HttpClient login(User user) throws Exception {
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        try {assertEquals(302,c.send(request("/login").header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username="+encode(user.username())+"&password="+encode(PASSWORD)+"&_csrf="+encode(token(c,"/login")))).build(),HttpResponse.BodyHandlers.discarding()).statusCode());return c;}
        catch(Exception|AssertionError e){c.close();throw e;}
    }
    HttpRequest.Builder request(String path){return HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(30));}
    HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(request(path).GET().build(),HttpResponse.BodyHandlers.ofString());}
    int post(HttpClient c,String path)throws Exception {return c.send(request(path).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("_csrf="+encode(token(c,"/account")))).build(),HttpResponse.BodyHandlers.discarding()).statusCode();}
    String token(HttpClient c,String path)throws Exception {var r=get(c,path);assertEquals(200,r.statusCode());var m=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(r.body());assertTrue(m.find());return HtmlUtils.htmlUnescape(m.group(1));}
    static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
