package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import vn.deadline.tracker.domain.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="project_group")
public class GroupEntity {
    @Id @Column(length=36) public String id;
    @Column(length=100) public String name;
    @Column(length=1000) public String description;
    @Column(length=36) public String ownerId;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
    @Version public Long version;
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
