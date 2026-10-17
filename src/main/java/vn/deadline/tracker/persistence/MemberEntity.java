package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import vn.deadline.tracker.domain.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="group_member")
public class MemberEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String groupId;
    @Column(length=36) public String userId;
    @Column(length=10) public String role;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant joinedAt;
    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
