package vn.deadline.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeadlineTrackerApplication {
    public static void main(String[] args) {
        if(args.length>0&&"migrate".equals(args[0])) {
            if(args.length!=1)throw new IllegalArgumentException("Use: java -jar app.jar migrate; credentials via MIGRATION_DB_* only");
            try {
                vn.deadline.tracker.ops.MigrationCommand.execute(System.getenv());
                System.out.println("MIGRATION_OK");
            } catch(Exception error) {
                // Do not echo JDBC URLs, credentials or driver exception messages.
                throw new IllegalStateException("Migration failed; web was not started. Inspect the isolated DB with the operator.");
            }
            return;
        }
        var context=SpringApplication.run(DeadlineTrackerApplication.class, args);
        if(context.getEnvironment().acceptsProfiles(org.springframework.core.env.Profiles.of("ops")))context.close();
    }
}
