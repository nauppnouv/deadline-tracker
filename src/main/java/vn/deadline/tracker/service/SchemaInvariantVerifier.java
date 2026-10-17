package vn.deadline.tracker.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/** Flyway checks migrations; this checks that required objects still exist after restore. */
@Component
@Profile({"prod","test"})
@DependsOnDatabaseInitialization
public class SchemaInvariantVerifier implements InitializingBean {
    private final JdbcTemplate jdbc;
    public SchemaInvariantVerifier(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void afterPropertiesSet() throws Exception {
        String sql=new ClassPathResource("db/invariants/schema-inventory.sql").getContentAsString(StandardCharsets.UTF_8);
        var actual=new HashSet<>(jdbc.queryForList(sql,String.class));
        var missing=new ClassPathResource("db/invariants/schema-required.txt").getContentAsString(StandardCharsets.UTF_8)
            .lines().filter(s->!s.isBlank()&&!s.startsWith("#")).filter(s->!actual.contains(s)).toList();
        if(!missing.isEmpty())throw new IllegalStateException("Missing schema invariants: "+String.join(", ",missing));
    }
}
