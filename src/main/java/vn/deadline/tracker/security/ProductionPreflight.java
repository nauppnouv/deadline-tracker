package vn.deadline.tracker.security;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import java.net.InetAddress;
import java.util.Set;

/** Runs after config data is loaded, before beans, database access or listeners. */
public final class ProductionPreflight implements EnvironmentPostProcessor,Ordered {
    private static final Set<String> OLD_EXAMPLES=Set.of(
        "replace-with-a-long-random-local-database-password",
        "replace-with-a-different-long-random-root-password");
    @Override public int getOrder(){return Ordered.LOWEST_PRECEDENCE;}
    @Override public void postProcessEnvironment(ConfigurableEnvironment env,SpringApplication application) {
        if(env.acceptsProfiles(Profiles.of("prod","ops"))) {
            if(!Boolean.FALSE.equals(env.getProperty("spring.flyway.enabled",Boolean.class)))
                throw invalid("spring.flyway.enabled","must be false in runtime; use the separate migrate command");
            for(String key:Set.of("MIGRATION_DB_URL","MIGRATION_DB_USER","MIGRATION_DB_PASSWORD",
                    "spring.flyway.url","spring.flyway.user","spring.flyway.password"))
                if(env.getProperty(key)!=null)throw invalid(key,"belongs only to the separate migration process");
        }
        if(!env.acceptsProfiles(Profiles.of("prod")))return;
        password("DB_PASSWORD",env.getProperty("DB_PASSWORD"));
        password("spring.datasource.password",env.getProperty("spring.datasource.password"));
        // Root credentials belong to DB provisioning, never to the web process.
        // This catches configuration mistakes; it does not audit DB privileges.
        if("root".equalsIgnoreCase(env.getProperty("spring.datasource.username","").strip()))
            throw invalid("spring.datasource.username","root runtime principal forbidden; use a schema account");
        loopback("SERVER_ADDRESS",env.getProperty("SERVER_ADDRESS"));
        loopback("server.address",env.getProperty("server.address"));
        if(!"native".equalsIgnoreCase(env.getProperty("server.forward-headers-strategy","")))
            throw invalid("server.forward-headers-strategy","must be native for the documented loopback proxy");
    }
    private static String password(String key,String value) {
        if(value==null||value.codePointCount(0,value.length())<16||value.strip().startsWith("CHANGE_ME_")||OLD_EXAMPLES.contains(value.strip()))
            throw invalid(key,"required; at least 16 characters; example placeholders forbidden");
        return value;
    }
    private static void loopback(String key,String value) {
        // Accept numeric loopback only: no DNS lookup or hostname trust at startup.
        if(value!=null&&(value.equals("::1")||value.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) {
            try {if(InetAddress.getByName(value).isLoopbackAddress())return;}catch(java.net.UnknownHostException ignored) { }
        }
        throw invalid(key,"explicit numeric loopback address required (127.0.0.1 or ::1)");
    }
    private static IllegalStateException invalid(String key,String rule) {
        return new IllegalStateException("Prod preflight: "+key+" "+rule); // Never include secret values.
    }
}
