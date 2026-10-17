package vn.deadline.tracker.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.persistence.*;
import vn.deadline.tracker.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @Transactional
public class WorkService {
    private final GroupAccess access; private final GroupRepository groups; private final MemberRepository members;
    private final TaskRepository tasks; private final TaskEventRepository events; private final Clock clock;
    public WorkService(GroupAccess access,GroupRepository groups,MemberRepository members,TaskRepository tasks,TaskEventRepository events,Clock clock) {
        this.access=access;this.groups=groups;this.members=members;this.tasks=tasks;this.events=events;this.clock=clock;
    }
    public Group createGroup(String actor,String name,String description) {
        access.active(actor); var domain=Group.create(name,description,actor);
        var row=new GroupEntity();row.id=domain.getId();row.name=domain.getName();row.description=domain.getDescription();row.ownerId=actor;row.createdAt=now();
        groups.saveAndFlush(row);
        // V4 inserts the Owner membership in the same DB statement/transaction.
        return domain;
    }
    @Transactional(readOnly=true) public Group group(String actor,String groupId) {return access.member(actor,groupId);}
    @Transactional(readOnly=true) public List<GroupEntity> groups(String actor) {
        access.active(actor); return groups.findAllById(members.findByUserId(actor).stream().map(m->m.groupId).toList()).stream().sorted(Comparator.comparing(g->g.name)).toList();
    }
    public void renameGroup(String actor,String groupId,String name,String description,long expectedVersion) {
        var domain=access.owner(actor,groupId);domain.rename(actor,name,description);
        var row=groups.findById(groupId).orElseThrow(RuleViolation::missing);
        if(row.version==null||row.version!=expectedVersion)throw RuleViolation.conflict();
        row.name=domain.getName();row.description=domain.getDescription();groups.flush();
    }
    public Task createTask(String actor,String groupId,String title,String description,String assignee,Instant due) {
        var group=access.member(actor,groupId);access.assignable(groupId,assignee);var domain=Task.create(group,actor,title,description,assignee,due,clock);
        var row=new TaskEntity();copy(domain,row);tasks.saveAndFlush(row);event(actor,null,domain,Task.Change.CREATED);events.flush();return domain(row);
    }
    @Transactional(readOnly=true) public Task task(String actor,String groupId,String taskId) {access.member(actor,groupId);return domain(row(groupId,taskId));}
    @Transactional(readOnly=true) public List<Task> tasks(String actor,String groupId) {access.member(actor,groupId);return tasks.findByGroupIdOrderByDueAtAscIdAsc(groupId).stream().map(this::domain).toList();}
    public Task changeStatus(String actor,String groupId,String taskId,TaskStatus next,long expectedVersion) {
        var group=access.member(actor,groupId);var row=row(groupId,taskId);version(row,expectedVersion);
        var before=domain(row);var updated=domain(row);
        if(updated.updateStatus(group,actor,next,clock)) {copy(updated,row);event(actor,before,updated,Task.Change.STATUS_CHANGED);tasks.flush();events.flush();}
        return domain(row);
    }
    public Task editTask(String actor,String groupId,String taskId,String title,String description,String assignee,Instant due,long expectedVersion) {
        var group=access.member(actor,groupId);var row=row(groupId,taskId);version(row,expectedVersion);
        if(!row.assigneeId.equals(assignee))access.assignable(groupId,assignee);
        var before=domain(row);var updated=domain(row);var change=updated.edit(group,actor,title,description,assignee,due,clock);
        if(change!=Task.Change.NONE) {copy(updated,row);event(actor,before,updated,change);tasks.flush();events.flush();}
        return domain(row);
    }
    private TaskEntity row(String groupId,String taskId) {return tasks.findByIdAndGroupId(taskId,groupId).orElseThrow(RuleViolation::missing);}
    private void version(TaskEntity row,long expected) {if(row.version==null || row.version!=expected) throw RuleViolation.conflict();}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private void copy(Task task,TaskEntity row) {
        row.id=task.getId();row.groupId=task.getGroupId();row.title=task.getTitle();row.description=task.getDescription();row.assigneeId=task.getAssigneeId();row.createdBy=task.getCreatedBy();
        row.dueAt=task.getDueAt();row.status=task.getStatus();row.completedAt=task.getCompletedAt();row.createdAt=task.getCreatedAt();row.updatedAt=task.getUpdatedAt();
    }
    private Task domain(TaskEntity row) {return Task.restore(row.id,row.groupId,row.title,row.description,row.assigneeId,row.createdBy,row.dueAt,row.status,row.completedAt,row.createdAt,row.updatedAt,row.version==null?0:row.version);}
    private void event(String actor,Task before,Task after,Task.Change change) {
        var e=new TaskEventEntity();e.id=UUID.randomUUID().toString();e.taskId=after.getId();e.actorId=actor;e.eventType=change.name();e.changedAt=now();
        e.oldStatus=before==null?null:before.getStatus().name();e.newStatus=after.getStatus().name();e.oldAssignee=before==null?null:before.getAssigneeId();e.newAssignee=after.getAssigneeId();
        e.oldDue=before==null?null:before.getDueAt();e.newDue=after.getDueAt();events.save(e);
    }
}
