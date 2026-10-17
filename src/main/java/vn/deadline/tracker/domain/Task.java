package vn.deadline.tracker.domain;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public final class Task {
    public enum Change { NONE, CREATED, UPDATED, REASSIGNED, STATUS_CHANGED }
    private final String id,groupId,createdBy;
    private final Instant createdAt;
    private final long version;
    private String title,description,assigneeId;
    private Instant dueAt,completedAt,updatedAt;
    private TaskStatus status;
    private Task(String id,String groupId,String title,String description,String assigneeId,String createdBy,Instant dueAt,TaskStatus status,Instant completedAt,Instant createdAt,Instant updatedAt,long version) {
        this.id=id; this.groupId=groupId; this.title=Inputs.required(title,150,"Tên công việc");
        this.description=Inputs.optional(description,2000,"Mô tả"); this.assigneeId=assigneeId; this.createdBy=createdBy;
        this.dueAt=Objects.requireNonNull(dueAt); this.status=Objects.requireNonNull(status);
        this.completedAt=completedAt; this.createdAt=createdAt; this.updatedAt=updatedAt; this.version=version;
    }
    public static Task create(Group group,String actor,String title,String description,String assignee,Instant due,Clock clock) {
        group.requireMember(actor);
        requireAssignee(group,assignee); requireFuture(due,clock.instant());
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        return new Task(UUID.randomUUID().toString(),group.getId(),title,description,assignee,actor,due,TaskStatus.TODO,null,now,now,0);
    }
    public static Task restore(String id,String groupId,String title,String description,String assignee,String creator,Instant due,TaskStatus status,Instant completed,Instant created,Instant updated,long version) {
        return new Task(id,groupId,title,description,assignee,creator,due,status,completed,created,updated,version);
    }
    private void authorize(Group group,String actor) {
        if (!groupId.equals(group.getId())) throw RuleViolation.missing();
        group.requireMember(actor);
    }
    private static void requireAssignee(Group group,String assignee) {
        if (assignee==null || !group.isMember(assignee)) throw RuleViolation.invalid("Người phụ trách phải là thành viên trong nhóm.");
    }
    private static void requireFuture(Instant due,Instant now) {
        if (due==null || !due.isAfter(now)) throw RuleViolation.invalid("Hạn nộp phải ở tương lai.");
    }
    public boolean updateStatus(Group group,String actor,TaskStatus next,Clock clock) {
        authorize(group,actor);
        if (!assigneeId.equals(actor)) throw RuleViolation.forbidden();
        if (next==null) throw RuleViolation.invalid("Trạng thái không hợp lệ.");
        if (next==status) return false;
        status=next; updatedAt=clock.instant().truncatedTo(ChronoUnit.MICROS);
        completedAt=next==TaskStatus.DONE ? updatedAt : null;
        return true;
    }
    public Change edit(Group group,String actor,String title,String description,String assignee,Instant due,Clock clock) {
        authorize(group,actor);
        if (!group.isOwner(actor) && !createdBy.equals(actor)) throw RuleViolation.forbidden();
        String newTitle=Inputs.required(title,150,"Tên công việc");
        String newDescription=Inputs.optional(description,2000,"Mô tả");
        requireAssignee(group,assignee);
        if (due==null) throw RuleViolation.invalid("Cần có hạn nộp.");
        boolean reassigned=!assigneeId.equals(assignee);
        boolean changedDue=!dueAt.truncatedTo(ChronoUnit.MINUTES).equals(due.truncatedTo(ChronoUnit.MINUTES));
        if (status==TaskStatus.DONE && (reassigned || changedDue)) throw RuleViolation.invalid("Hãy mở lại công việc trước khi đổi hạn hoặc giao lại.");
        if (changedDue) requireFuture(due,clock.instant());
        if (!reassigned && !changedDue && this.title.equals(newTitle) && this.description.equals(newDescription)) return Change.NONE;
        this.title=newTitle; this.description=newDescription;
        if (changedDue) dueAt=due;
        if (reassigned) { assigneeId=assignee; status=TaskStatus.TODO; completedAt=null; }
        updatedAt=clock.instant().truncatedTo(ChronoUnit.MICROS);
        return reassigned ? Change.REASSIGNED : Change.UPDATED;
    }
    public boolean isOverdue(Instant now) { return status!=TaskStatus.DONE && now.isAfter(dueAt); }
    public boolean isDueSoon(Instant now) { return status!=TaskStatus.DONE && !dueAt.isBefore(now) && !dueAt.isAfter(now.plusSeconds(86400)); }
    public String getId(){return id;} public String getGroupId(){return groupId;}
    public String getTitle(){return title;} public String getDescription(){return description;}
    public String getAssigneeId(){return assigneeId;} public String getCreatedBy(){return createdBy;}
    public Instant getDueAt(){return dueAt;} public TaskStatus getStatus(){return status;}
    public Instant getCompletedAt(){return completedAt;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;}
}
