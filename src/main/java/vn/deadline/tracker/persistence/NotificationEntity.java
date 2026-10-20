package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="notification",uniqueConstraints=@UniqueConstraint(name="uq_notification_recipient_event",columnNames={"event_key","recipient_id"}))
public class NotificationEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String eventKey;
    @Column(length=36) public String recipientId;
    @Column(length=36) public String groupId;
    @Column(length=36) public String taskId;
    @Column(length=36) public String commentId;
    @Column(length=20) public String kind;
    @Column(length=36) public String reminderAssigneeId;
    public Long reminderTaskVersion;
    public Integer reminderOffset;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant reminderDueAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant readAt;
}
