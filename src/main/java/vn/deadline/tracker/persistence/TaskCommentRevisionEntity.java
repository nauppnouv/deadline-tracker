package vn.deadline.tracker.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="task_comment_revision")
public class TaskCommentRevisionEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String commentId;
    @Column(length=36) public String editorId;
    @Column(length=4000) public String body;
    public long version;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant revisedAt;
}
