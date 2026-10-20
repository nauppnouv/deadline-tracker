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
        return new HashMap<>(Map.of("DB_PASSWORD","App fixture password 123!",
            "SERVER_ADDRESS","127.0.0.1","server.forward-headers-strategy","native"));
    }
    org.springframework.context.ConfigurableApplicationContext start(Map<String,Object> settings,String profile) {
        return start(settings,profile,null);
    }
    org.springframework.context.ConfigurableApplicationContext start(Map<String,Object> settings,String profile,PropertySource<?> extra) {
        BEAN_CREATED.set(false);
        var env=new StandardEnvironment();
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        env.getPropertySources().addFirst(new MapPropertySource("preflight-fixture",settings));
        if(extra!=null)env.getPropertySources().addFirst(extra);
        return new SpringApplicationBuilder(Minimal.class).environment(env).web(WebApplicationType.NONE).profiles(profile).run();
    }
    @ParameterizedTest @CsvSource({
        "DB_PASSWORD,<missing>","DB_PASSWORD,CHANGE_ME_DB_PASSWORD", "DB_PASSWORD,replace-with-a-long-random-local-database-password", "DB_PASSWORD,123456789012345",
        "SERVER_ADDRESS,<missing>","SERVER_ADDRESS,0.0.0.0","SERVER_ADDRESS,192.0.2.1","SERVER_ADDRESS,localhost",
        "server.address,0.0.0.0", "server.forward-headers-strategy,none", "server.forward-headers-strategy,framework", "server.forward-headers-strategy,<missing>",
        "spring.datasource.password,short", "spring.datasource.password,CHANGE_ME_OVERRIDE_PASSWORD",
        "spring.datasource.password,replace-with-a-long-random-local-database-password",
        "spring.datasource.password,replace-with-a-different-long-random-root-password"
    })
    @DisplayName("SEC-06/09/12 R2 prod preflight refuses unsafe configuration before creating beans")
    void rejects(String key,String value) {
        var settings=valid();if(value.equals("<missing>"))settings.remove(key);else settings.put(key,value.equals("<same>")?settings.get("DB_PASSWORD"):value);
        var error=assertThrows(Exception.class,()->{try(var ignored=start(settings,"prod")){fail("prod accepted unsafe "+key);}});
        assertFalse(BEAN_CREATED.get(),"Preflight must precede context/DB creation");
        assertTrue(error.getMessage().contains(key),error.getMessage());
        assertFalse(error.getMessage().contains("fixture password"),"Never echo passwords");
    }
    @Test @DisplayName("SEC-09/12 P3-01 prod accepts a 16 character app password and loopback without root secret")
    void accepts() {
        var settings=valid();settings.put("DB_PASSWORD","abcdefghijklmnop");
        try(var context=start(settings,"prod")){assertTrue(context.isActive());assertTrue(BEAN_CREATED.get());assertEquals("127.0.0.1",context.getEnvironment().getProperty("server.address"));}
        settings.put("SERVER_ADDRESS","::1");settings.put("server.address","::1");try(var context=start(settings,"prod")){assertTrue(context.isActive());}
    }
    @Test @DisplayName("SEC-12 P3-01 prod starts without any root secret")
    void startsWithoutRootSecret() {
        try(var context=start(valid(),"prod")) {
            assertTrue(context.isActive());assertTrue(BEAN_CREATED.get());
            assertNull(context.getEnvironment().getProperty("DB_ROOT_PASSWORD"));
        }
    }
    @Test @DisplayName("SEC-12 P3-01 preflight never resolves a root secret even when a source offers it")
    void neverReadsRootSecret() {
        var rootSource=new PropertySource<Object>("root-secret-access-canary") {
            @Override public Object getProperty(String name) {
                if(name.equals("DB_ROOT_PASSWORD"))throw new AssertionError("Web attempted to resolve root secret");
                return null;
            }
        };
        try(var context=start(valid(),"prod",rootSource)) {assertTrue(context.isActive());assertTrue(BEAN_CREATED.get());}
    }
    @ParameterizedTest @CsvSource({"DB_USER,root","spring.datasource.username,root","spring.datasource.username,ROOT"})
    @DisplayName("SEC-12 P3-01 effective runtime username root is refused before beans without root secret")
    void rejectsRootRuntimeUsername(String key,String value) {
        var settings=valid();settings.put(key,value);
        var error=assertThrows(Exception.class,()->{try(var ignored=start(settings,"prod")){fail("root runtime accepted");}});
        assertFalse(BEAN_CREATED.get());assertTrue(error.getMessage().contains("spring.datasource.username"));
    }
    @ParameterizedTest @CsvSource({"prod","ops"})
    @DisplayName("P3-02 web and ops refuse automatic Flyway migration before beans")
    void rejectsMigrationInRuntime(String profile) {
        var settings=valid();settings.put("spring.flyway.enabled","true");
        var error=assertThrows(Exception.class,()->{try(var ignored=start(settings,profile)){fail("runtime migration accepted");}});
        assertFalse(BEAN_CREATED.get());assertTrue(error.getMessage().contains("spring.flyway.enabled"));
    }
    @ParameterizedTest @CsvSource({"prod,MIGRATION_DB_PASSWORD","ops,MIGRATION_DB_PASSWORD","prod,spring.flyway.password","prod,spring.flyway.user","ops,MIGRATION_DB_USER"})
    @DisplayName("P3-02 runtime refuses migration credentials even with Flyway disabled")
    void rejectsRuntimeMigrationCredentials(String profile,String key) {
        var settings=valid();settings.put("spring.flyway.enabled","false");settings.put(key,"Migration fixture must remain separate 123!");
        var error=assertThrows(Exception.class,()->{try(var ignored=start(settings,profile)){fail("migration credentials accepted");}});
        assertFalse(BEAN_CREATED.get());assertTrue(error.getMessage().contains(key));
        assertFalse(error.getMessage().contains("Migration fixture"));
    }
    @ParameterizedTest @CsvSource({"prod","ops"})
    @DisplayName("P3-02 prod and ops default to no Flyway auto-migration")
    void runtimeProfilesDisableFlyway(String profile) {
        try(var context=start(valid(),profile)) {
            assertFalse(context.getEnvironment().getProperty("spring.flyway.enabled",Boolean.class,true));
        }
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
