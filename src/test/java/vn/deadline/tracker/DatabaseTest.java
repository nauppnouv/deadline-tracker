package vn.deadline.tracker;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import java.util.UUID;

public abstract class DatabaseTest {
    protected static final MariaDBContainer DB = new MariaDBContainer(DockerImageName.parse("mariadb@sha256:f16d7a344413e519dff518f27bdb221388f84b0d274c97c4fb5f2cb0dc1b34fa").asCompatibleSubstituteFor("mariadb"))
        .withDatabaseName("deadline_test").withUsername("deadline_test")
        .withPassword(UUID.randomUUID().toString()).withCommand("--default-time-zone=+00:00");
    static { DB.start(); }
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
        registry.add("spring.session.jdbc.cleanup-cron",()->"-");
    }
}
