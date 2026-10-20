package vn.deadline.tracker.persistence;
import jakarta.persistence.*;import java.time.Instant;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;
@Entity @Table(name="task_attachment")
public class TaskAttachmentEntity {
 @Id @Column(length=36) public String id;@Column(length=36) public String commentId;@Column(length=36) public String taskId;@Column(length=36) public String groupId;@Column(length=36) public String uploaderId;
 @Column(length=255) public String displayName;@Column(length=100) public String mediaType;public long sizeBytes;@Column(length=64) public String sha256;@Column(length=36) public String storageKey;
 @Column(length=10) public String lifecycle;@JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;@JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant hiddenAt;@Column(length=36) public String hiddenBy;
}
