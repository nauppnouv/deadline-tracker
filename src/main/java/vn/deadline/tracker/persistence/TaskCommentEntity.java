package vn.deadline.tracker.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="task_comment")
public class TaskCommentEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String taskId;
    @Column(length=36) public String groupId;
    @Column(length=36) public String authorId;
    @Column(length=4000) public String body;
    @Version public Long version;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant updatedAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant hiddenAt;
    @Column(length=36) public String hiddenBy;
}
