package vn.deadline.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeadlineTrackerApplication {
    public static void main(String[] args) {
        var context=SpringApplication.run(DeadlineTrackerApplication.class, args);
        if(context.getEnvironment().acceptsProfiles(org.springframework.core.env.Profiles.of("ops")))context.close();
    }
}
