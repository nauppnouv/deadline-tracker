package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.domain.RuleViolation;
import vn.deadline.tracker.persistence.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service @Transactional
public class NotificationService {
    private final GroupAccess access;private final NotificationRepository notifications;private final TaskRepository tasks;private final Clock clock;
    public NotificationService(GroupAccess access,NotificationRepository notifications,TaskRepository tasks,Clock clock){this.access=access;this.notifications=notifications;this.tasks=tasks;this.clock=clock;}
    @Transactional(readOnly=true) public List<NotificationEntity> inbox(String actor){
        access.active(actor);return notifications.findByRecipientIdOrderByCreatedAtDescIdDesc(actor).stream().filter(n->access.hasMembership(actor,n.groupId)).filter(n->!stale(n)).toList();
    }
    @Transactional(readOnly=true) public long unread(String actor){access.active(actor);return notifications.findByRecipientIdOrderByCreatedAtDescIdDesc(actor).stream().filter(n->n.readAt==null&&access.hasMembership(actor,n.groupId)&&!stale(n)).count();}
    public String open(String actor,String id){
        var n=notifications.findByIdAndRecipientId(id,actor).orElseThrow(RuleViolation::missing);access.member(actor,n.groupId);if(stale(n))throw RuleViolation.missing();
        if(n.readAt==null)n.readAt=clock.instant().truncatedTo(ChronoUnit.MICROS);
        return "/groups/"+n.groupId+"/tasks/"+n.taskId+(n.commentId==null?"":"#comment-"+n.commentId);
    }
    public void read(String actor,String id){open(actor,id);}
    @Transactional(readOnly=true) public void view(String actor,String id){access.active(actor);var n=notifications.findByIdAndRecipientId(id,actor).orElseThrow(RuleViolation::missing);access.member(actor,n.groupId);if(stale(n))throw RuleViolation.missing();}
    private boolean stale(NotificationEntity n){
        if(!"REMINDER".equals(n.kind))return false;
        var task=tasks.findByIdAndGroupId(n.taskId,n.groupId).orElse(null);
        return task==null||task.status==vn.deadline.tracker.domain.TaskStatus.DONE
            ||!Objects.equals(task.assigneeId,n.reminderAssigneeId)
            ||!Objects.equals(task.dueAt,n.reminderDueAt)
            ||!Objects.equals(task.version,n.reminderTaskVersion);
    }
}
