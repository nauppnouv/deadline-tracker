package vn.deadline.tracker.domain;
import java.time.Instant;
import java.util.List;
public record Progress(int total,long todo,long inProgress,long done,long overdue,int percent) {
    public static Progress of(List<Task> tasks,Instant now) {
        long done=tasks.stream().filter(t->t.getStatus()==TaskStatus.DONE).count();
        return new Progress(tasks.size(),tasks.stream().filter(t->t.getStatus()==TaskStatus.TODO).count(),
            tasks.stream().filter(t->t.getStatus()==TaskStatus.IN_PROGRESS).count(),done,
            tasks.stream().filter(t->t.isOverdue(now)).count(),tasks.isEmpty()?0:(int)Math.round(done*100.0/tasks.size()));
    }
}
