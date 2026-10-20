package vn.deadline.tracker.ops;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.deadline.tracker.service.SchemaInvariantVerifier;
import java.util.Map;

/** A separate process entry point: no application context, HTTP, sessions or ops runner. */
public final class MigrationCommand {
    private MigrationCommand() { }
    public static void execute(Map<String,String> environment) throws Exception {
        String url=required(environment,"MIGRATION_DB_URL");
        String user=required(environment,"MIGRATION_DB_USER");
        String password=required(environment,"MIGRATION_DB_PASSWORD");
        // Same-host database only; remote transport/TLS needs a separate design.
        if(!url.matches("jdbc:mariadb://(?:127\\.0\\.0\\.1|\\[::1\\])(?::[0-9]{1,5})?/[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException("MIGRATION_DB_URL must name one numeric loopback MariaDB host/schema without credentials or options");
        if(!user.matches("[a-zA-Z0-9_]+")||"root".equalsIgnoreCase(user))
            throw new IllegalArgumentException("MIGRATION_DB_USER must be a dedicated non-root schema account");
        if(password.codePointCount(0,password.length())<16||password.strip().startsWith("CHANGE_ME_")||password.strip().startsWith("replace-with-"))
            throw new IllegalArgumentException("MIGRATION_DB_PASSWORD must be at least 16 characters, not an example");
        Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration")
            .cleanDisabled(true).baselineOnMigrate(false).outOfOrder(false).validateOnMigrate(true).load().migrate();
        new SchemaInvariantVerifier(new JdbcTemplate(new DriverManagerDataSource(url,user,password))).afterPropertiesSet();
    }
    private static String required(Map<String,String> environment,String key) {
        String value=environment.get(key);
        if(value==null||value.isBlank())throw new IllegalArgumentException(key+" is required for migration only");
        return value;
    }
}
