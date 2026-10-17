package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import vn.deadline.tracker.domain.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="app_user")
public class AccountEntity {
    @Id @Column(length=36) public String id;
    @Column(length=44) public String username;
    @Column(length=80) public String displayName;
    @Column(length=255) public String passwordHash;
    public boolean enabled;
    public boolean mustChangePassword;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
    @Version public Long version;
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public boolean getEnabled() { return enabled; }
    public boolean getMustChangePassword() { return mustChangePassword; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
