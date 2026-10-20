package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import vn.deadline.tracker.service.*;
import java.io.ByteArrayInputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="server.address=127.0.0.1")
class ChunkedRequestTest extends DatabaseTest {
    @Value("${local.server.port}") int port;
    @Autowired AccountService accounts; @Autowired WorkService work;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    HttpClient client; String actor,path,uploadPath,taskId;
    String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    String token(String path)throws Exception {
        var html=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString()).body();
        var matcher=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);assertTrue(matcher.find(),"CSRF input required");
        return org.springframework.web.util.HtmlUtils.htmlUnescape(matcher.group(1));
    }
    String version()throws Exception {
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());return HtmlForms.version(response.body(),path);
    }
    int send(String path,String body,String type)throws Exception {
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        var publisher=HttpRequest.BodyPublishers.ofInputStream(()->new ByteArrayInputStream(bytes));
        assertEquals(-1,publisher.contentLength(),"Real chunked body, unknown content length");
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type",type).POST(publisher).build(),HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    String multipart(String boundary,String name,String content)throws Exception{return "--"+boundary+"\r\nContent-Disposition: form-data; name=\"_csrf\"\r\n\r\n"+encode(token(path))+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+name+"\"\r\nContent-Type: text/plain\r\n\r\n"+content+"\r\n--"+boundary+"--\r\n";}
    int sendMultipartChunked(String route,String body,String boundary)throws Exception{
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);var publisher=HttpRequest.BodyPublishers.ofInputStream(()->new ByteArrayInputStream(bytes));assertEquals(-1,publisher.contentLength());
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+route)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(publisher).build(),HttpResponse.BodyHandlers.ofString());return response.statusCode();
    }
    @BeforeEach void login()throws Exception {
        String name="c"+UUID.randomUUID().toString().replace("-","").substring(0,20),password="Chunked fixture passphrase 123!";
        actor=accounts.register(name,"Chunked owner",password);path="/groups/"+work.createGroup(actor,"Chunked original","").getId()+"/settings";
        String group=path.split("/")[2];var task=work.createTask(actor,group,"Upload test","",actor,java.time.Instant.now().plusSeconds(3600));taskId=task.getId();uploadPath="/groups/"+group+"/tasks/"+taskId+"/attachments";
        client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).followRedirects(HttpClient.Redirect.NEVER).build();
        assertEquals(302,send("/login","username="+encode(name)+"&password="+encode(password)+"&_csrf="+encode(token("/login")),"application/x-www-form-urlencoded"));
    }
    @Test @DisplayName("P7-04 SEC-14 actual chunked oversized body with valid query parameters cannot update group")
    void oversizedBinary()throws Exception {
        String query="?name=Should-not-save&description=&version="+version()+"&_csrf="+encode(token(path));
        assertEquals(413,send(path+query,"x".repeat(17000),"application/octet-stream"));
        assertEquals("Chunked original",work.group(actor,path.split("/")[2]).getName());
    }
    @Test @DisplayName("P7-04 SEC-14 chunked forms at 16383/16384 accepted, 16385 rejected; UTF-8 and CSRF preserved")
    void formBoundaries()throws Exception {
        for(int size:new int[]{16383,16384,16385}) {
            String body="name="+encode("Nhóm thử")+"&description="+encode("Mô tả có dấu")+"&version="+version()+"&_csrf="+encode(token(path))+"&padding=";
            body+="x".repeat(size-body.getBytes(StandardCharsets.UTF_8).length);
            assertEquals(size<=16384?302:413,send(path,body,"application/x-www-form-urlencoded; charset=UTF-8"),"body bytes="+size);
        }
        assertEquals("Nhóm thử",work.group(actor,path.split("/")[2]).getName());
        assertEquals(403,send(path,"name=x&description=&version="+version(),"application/x-www-form-urlencoded"));
        assertEquals(403,send(path,"name=x&description=&version="+version()+"&_csrf=invalid","application/x-www-form-urlencoded"));
    }
    @Test @DisplayName("P7-04 SEC-14 unsupported small request content type is rejected before mutation")
    void unsupportedType()throws Exception {
        String query="?name=Should-not-save&description=&version="+version()+"&_csrf="+encode(token(path));
        assertEquals(415,send(path+query,"x","application/octet-stream"));
        assertEquals("Chunked original",work.group(actor,path.split("/")[2]).getName());
    }
    @Test @DisplayName("W1.1 M3 actual chunked multipart larger than legacy form limit streams through guarded route")
    void chunkedMultipartUpload()throws Exception{
        String boundary="W11-"+UUID.randomUUID().toString().replace("-","");String body=multipart(boundary,"sample.txt","synthetic text ".repeat(1800));
        assertTrue(body.getBytes(StandardCharsets.UTF_8).length>16384);assertEquals(302,sendMultipartChunked(uploadPath,body,boundary));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM task_attachment WHERE task_id=? AND lifecycle='READY'",Integer.class,taskId));
        assertEquals(27000L,jdbc.queryForObject("SELECT size_bytes FROM task_attachment WHERE task_id=?",Long.class,taskId));
    }
    @Test void chunkedSubscriptionJsonRetainsCsrfTypeAndSizeGuards()throws Exception {
        String url="http://127.0.0.1:"+port+"/api/push/devices";String csrfToken=token("/account");
        byte[] json="{}".getBytes(StandardCharsets.UTF_8);
        var chunked=HttpRequest.BodyPublishers.ofInputStream(()->new ByteArrayInputStream(json));assertEquals(-1,chunked.contentLength());
        var response=client.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrfToken).POST(chunked).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(400,response.statusCode());assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertEquals(403,send("/api/push/devices","{}","application/json"));
        assertEquals(413,send("/api/push/devices","x".repeat(16385),"application/json"));
        assertEquals(415,send("/api/push/devices","{}","application/octet-stream"));
        assertEquals(415,send("/api/push/disable-all","{}","application/json"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM push_device WHERE user_id=?",Integer.class,actor));
    }
}
