package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.domain.RuleViolation;
import vn.deadline.tracker.persistence.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import org.springframework.data.domain.PageRequest;
@Service @Transactional
public class NotificationService {
    private final GroupAccess access;private final NotificationRepository notifications;private final TaskRepository tasks;private final Clock clock;
    public NotificationService(GroupAccess access,NotificationRepository notifications,TaskRepository tasks,Clock clock){this.access=access;this.notifications=notifications;this.tasks=tasks;this.clock=clock;}
    @Transactional(readOnly=true) public List<NotificationEntity> inbox(String actor){
        return page(actor,null).items();
    }
    public static final int PAGE_SIZE=20;
    public record InboxPage(List<NotificationEntity> items,String nextCursor) {}
    private record Cursor(Instant at,String id) {}
    @Transactional(readOnly=true) public InboxPage page(String actor,String before){
        access.active(actor);
        var cursor=decode(before);
        var rows=notifications.inboxPage(actor,cursor==null?null:cursor.at(),cursor==null?null:cursor.id(),PageRequest.of(0,PAGE_SIZE+1));
        boolean more=rows.size()>PAGE_SIZE;
        var items=List.copyOf(rows.subList(0,Math.min(PAGE_SIZE,rows.size())));
        String next=null;
        if(more){var last=items.getLast();next=Base64.getUrlEncoder().withoutPadding().encodeToString((last.createdAt+"|"+last.id).getBytes(StandardCharsets.UTF_8));}
        return new InboxPage(items,next);
    }
    private Cursor decode(String before){
        if(before==null)return null;
        try{
            if(before.length()>256||!before.matches("[A-Za-z0-9_-]+"))throw new IllegalArgumentException();
            String[] parts=new String(Base64.getUrlDecoder().decode(before),StandardCharsets.UTF_8).split("\\|",-1);
            if(parts.length!=2||!parts[1].matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException();
            Instant at=Instant.parse(parts[0]);
            if(at.isBefore(Instant.parse("1000-01-01T00:00:00Z"))||!at.isBefore(Instant.parse("+10000-01-01T00:00:00Z"))||!at.equals(at.truncatedTo(ChronoUnit.MICROS)))throw new IllegalArgumentException();
            return new Cursor(at,parts[1]);
        }catch(RuntimeException e){throw RuleViolation.invalid("Con trỏ thông báo không hợp lệ.");}
    }
    @Transactional(readOnly=true) public long unread(String actor){access.active(actor);return notifications.unreadVisible(actor);}
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
