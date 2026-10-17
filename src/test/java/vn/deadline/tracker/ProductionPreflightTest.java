package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class ProductionPreflightTest {
    static final AtomicBoolean BEAN_CREATED=new AtomicBoolean();
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Minimal {
        @org.springframework.context.annotation.Bean Object marker(){BEAN_CREATED.set(true);return new Object();}
    }
    Map<String,Object> valid() {
        return new HashMap<>(Map.of("DB_PASSWORD","App fixture password 123!","DB_ROOT_PASSWORD","Root fixture password 456!",
            "SERVER_ADDRESS","127.0.0.1","server.forward-headers-strategy","native"));
    }
    org.springframework.context.ConfigurableApplicationContext start(Map<String,Object> settings,String profile) {
        BEAN_CREATED.set(false);
        var env=new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new MapPropertySource("preflight-fixture",settings));
        return new SpringApplicationBuilder(Minimal.class).environment(env).web(WebApplicationType.NONE).profiles(profile).run();
    }
    @ParameterizedTest @CsvSource({
        "DB_PASSWORD,<missing>","DB_PASSWORD,CHANGE_ME_DB_PASSWORD", "DB_PASSWORD,replace-with-a-long-random-local-database-password", "DB_PASSWORD,123456789012345",
        "DB_ROOT_PASSWORD,<missing>","DB_ROOT_PASSWORD,CHANGE_ME_ROOT_PASSWORD", "DB_ROOT_PASSWORD,replace-with-a-different-long-random-root-password", "DB_ROOT_PASSWORD,123456789012345", "DB_ROOT_PASSWORD,<same>",
        "SERVER_ADDRESS,<missing>","SERVER_ADDRESS,0.0.0.0","SERVER_ADDRESS,192.0.2.1","SERVER_ADDRESS,localhost",
        "server.address,0.0.0.0", "server.forward-headers-strategy,none", "server.forward-headers-strategy,framework", "server.forward-headers-strategy,<missing>",
        "spring.datasource.password,short"
    })
    @DisplayName("SEC-06/09/12 R2 prod preflight refuses unsafe configuration before creating beans")
    void rejects(String key,String value) {
        var settings=valid();if(value.equals("<missing>"))settings.remove(key);else settings.put(key,value.equals("<same>")?settings.get("DB_PASSWORD"):value);
        var error=assertThrows(Exception.class,()->{try(var ignored=start(settings,"prod")){fail("prod accepted unsafe "+key);}});
        assertFalse(BEAN_CREATED.get(),"Preflight must precede context/DB creation");
        assertTrue(error.getMessage().contains(key),error.getMessage());
        assertFalse(error.getMessage().contains("fixture password"),"Never echo passwords");
    }
    @Test @DisplayName("SEC-09/12 R2 prod accepts distinct 16 character passwords and loopback; default bind is loopback")
    void accepts() {
        var settings=valid();settings.put("DB_PASSWORD","abcdefghijklmnop");settings.put("DB_ROOT_PASSWORD","qrstuvwxyzABCDEF");
        try(var context=start(settings,"prod")){assertTrue(context.isActive());assertTrue(BEAN_CREATED.get());assertEquals("127.0.0.1",context.getEnvironment().getProperty("server.address"));}
        settings.put("SERVER_ADDRESS","::1");settings.put("server.address","::1");try(var context=start(settings,"prod")){assertTrue(context.isActive());}
    }
    @Test @DisplayName("SEC-12 R2 dev profile keeps local configuration independent of prod preflight")
    void dev() { try(var context=start(Map.of(),"dev")){assertTrue(context.isActive());} }
    @Test @DisplayName("SEC-12 R2 compose declares dev only and example secrets are visibly invalid")
    void examples()throws Exception {
        String compose=Files.readString(Path.of("docker-compose.yml"));assertTrue(compose.contains("DEV ONLY"));
        for(String line:Files.readAllLines(Path.of(".env.example")))if(line.startsWith("DB_PASSWORD=")||line.startsWith("DB_ROOT_PASSWORD="))assertTrue(line.split("=",2)[1].startsWith("CHANGE_ME_"));
    }
    @Test @DisplayName("SEC-09 R2 single forwarded headers copy block preserves literal loopback regex")
    void forwardedCopyBlock()throws Exception {
        String docs=Files.readString(Path.of("docs/DEPLOY.md"));
        for(String key:List.of("SERVER_FORWARD_HEADERS_STRATEGY=","SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES=","SERVER_TOMCAT_REDIRECT_CONTEXT_ROOT="))
            assertEquals(1,docs.split(java.util.regex.Pattern.quote(key),-1).length-1,key);
        var match=java.util.regex.Pattern.compile("SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES='([^']+)'").matcher(docs);assertTrue(match.find());
        var peers=java.util.regex.Pattern.compile(match.group(1));
        assertTrue(peers.matcher("127.0.0.1").matches());assertTrue(peers.matcher("::1").matches());
        assertFalse(peers.matcher("127a0b0c1").matches());assertFalse(peers.matcher("192.0.2.1").matches());
    }
}
