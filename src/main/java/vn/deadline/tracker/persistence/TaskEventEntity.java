package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import vn.deadline.tracker.domain.TaskStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="task_event")
public class TaskEventEntity {
    @Id @Column(length=36) public String id;
    @Column(length=36) public String taskId;
    @Column(length=36) public String actorId;
    @Column(length=20) public String eventType;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant changedAt;
    @Column(length=20) public String oldStatus;
    @Column(length=20) public String newStatus;
    @Column(length=36) public String oldAssignee;
    @Column(length=36) public String newAssignee;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant oldDue;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) public Instant newDue;
    public String getId() { return id; }
    public String getTaskId() { return taskId; }
    public String getActorId() { return actorId; }
    public String getEventType() { return eventType; }
    public Instant getChangedAt() { return changedAt; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getOldAssignee() { return oldAssignee; }
    public String getNewAssignee() { return newAssignee; }
    public Instant getOldDue() { return oldDue; }
    public Instant getNewDue() { return newDue; }
}
