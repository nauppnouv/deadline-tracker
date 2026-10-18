package vn.deadline.tracker.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/** Shared metadata contract: required objects and exact CHECK/generated definitions. */
@Component
@Profile({"prod","test"})
@DependsOnDatabaseInitialization
public class SchemaInvariantVerifier implements InitializingBean {
    private final JdbcTemplate jdbc;
    public SchemaInvariantVerifier(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void afterPropertiesSet() throws Exception {
        String sql=new ClassPathResource("db/invariants/schema-inventory.sql").getContentAsString(StandardCharsets.UTF_8);
        var actual=new HashSet<>(jdbc.queryForList(sql,String.class));
        var required=new HashSet<>(new ClassPathResource("db/invariants/schema-required.txt").getContentAsString(StandardCharsets.UTF_8)
            .lines().filter(s->!s.isBlank()&&!s.startsWith("#")).toList());
        var missing=required.stream().filter(s->!actual.contains(s)).map(SchemaInvariantVerifier::label).sorted().toList();
        if(!missing.isEmpty())throw new IllegalStateException("Missing schema invariants: "+String.join(", ",missing));
        var unexpected=actual.stream().filter(SchemaInvariantVerifier::definition).filter(s->!required.contains(s))
            .map(SchemaInvariantVerifier::label).sorted().toList();
        if(!unexpected.isEmpty())throw new IllegalStateException("Unexpected schema invariants: "+String.join(", ",unexpected));
    }
    private static boolean definition(String row) { return row.startsWith("CHECKDEF|")||row.startsWith("GENERATEDDEF|"); }
    private static String label(String row) {
        if(!definition(row))return row;
        String[] parts=row.split("\\|",4);
        return parts[1]+"."+parts[2]+" ["+row+"]";
    }
}
