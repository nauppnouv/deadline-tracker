package vn.deadline.tracker.domain;
import java.time.Instant;
public record TaskFilter(String assigneeId,TaskStatus status,Due due) {
    public enum Due { ALL, OVERDUE, SOON }
    public boolean matches(Task task,Instant now) {
        return (assigneeId==null || assigneeId.isBlank() || assigneeId.equals(task.getAssigneeId()))
            && (status==null || status==task.getStatus())
            && (due==null || due==Due.ALL || (due==Due.OVERDUE ? task.isOverdue(now) : task.isDueSoon(now)));
    }
}
