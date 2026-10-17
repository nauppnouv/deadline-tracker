package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import vn.deadline.tracker.domain.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="task")
public class TaskEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String groupId;
    @Column(length=150) public String title;
    @Column(length=2000) public String description;
    @Column(length=36) public String assigneeId;
    @Column(length=36) public String createdBy;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant dueAt;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length=20) public TaskStatus status;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant completedAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant updatedAt;
    @Version public Long version;
    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAssigneeId() { return assigneeId; }
    public String getCreatedBy() { return createdBy; }
    public Instant getDueAt() { return dueAt; }
    public TaskStatus getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
