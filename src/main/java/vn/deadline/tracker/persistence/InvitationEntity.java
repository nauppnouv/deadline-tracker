package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import vn.deadline.tracker.domain.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="invitation")
public class InvitationEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String groupId;
    @Column(length=64) public String tokenHash;
    @Column(length=36) public String createdBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant expiresAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant usedAt;
    @Column(length=36) public String usedBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant revokedAt;
    @Version public Long version;
    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getTokenHash() { return tokenHash; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public String getUsedBy() { return usedBy; }
    public Instant getRevokedAt() { return revokedAt; }
    public Long getVersion() { return version; }
}
