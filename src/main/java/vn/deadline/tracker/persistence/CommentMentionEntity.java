package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="comment_mention",uniqueConstraints=@UniqueConstraint(name="uq_comment_mention",columnNames={"comment_id","mentioned_user_id"}))
public class CommentMentionEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String commentId;
    @Column(length=36) public String groupId;
    @Column(length=36) public String mentionedUserId;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant createdAt;
}
