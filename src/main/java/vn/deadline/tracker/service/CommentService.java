package vn.deadline.tracker.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.domain.RuleViolation;
import vn.deadline.tracker.persistence.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.Instant;

@Service @Transactional
public class CommentService {
    public record TimelineEntry(String id,Instant at,String kind,String actor,String text,TaskCommentEntity comment) {}
    public record Page(List<TimelineEntry> entries,int page,int pages) {}
    private static final int PAGE_SIZE=20;
    private final GroupAccess access; private final TaskRepository tasks;
    private final TaskCommentRepository comments; private final TaskCommentRevisionRepository revisions; private final TaskEventRepository events;
    private final GroupRepository groups; private final CommentMentionRepository mentions; private final NotificationRepository notifications; private final Clock clock;private final NotificationPublisher publisher;private final TaskAttachmentRepository files;private final TaskPinRepository pins;
    public CommentService(GroupAccess access,TaskRepository tasks,TaskCommentRepository comments,TaskCommentRevisionRepository revisions,TaskEventRepository events,GroupRepository groups,CommentMentionRepository mentions,NotificationRepository notifications,Clock clock,NotificationPublisher publisher,TaskAttachmentRepository files,TaskPinRepository pins){
        this.access=access;this.tasks=tasks;this.comments=comments;this.revisions=revisions;this.events=events;this.groups=groups;this.mentions=mentions;this.notifications=notifications;this.clock=clock;this.publisher=publisher;this.files=files;this.pins=pins;
    }
    @Transactional(readOnly=true) public Page list(String actor,String group,String task,int page){
        task(actor,group,task);var all=new ArrayList<TimelineEntry>();
        comments.findByTaskIdOrderByCreatedAtDescIdDesc(task).stream().filter(c->c.hiddenAt==null)
            .forEach(c->all.add(new TimelineEntry(c.id,c.createdAt,"COMMENT",c.authorId,c.body,c)));
        events.findByTaskIdOrderByChangedAtDescIdDesc(task).forEach(e->all.add(new TimelineEntry(e.id,e.changedAt,"EVENT",e.actorId,e.eventType,null)));
        files.findByTaskIdOrderByCreatedAtDescIdDesc(task).stream().filter(f->f.hiddenAt==null&&"READY".equals(f.lifecycle)&&(f.commentId==null||comments.findByIdAndTaskId(f.commentId,task).filter(c->c.hiddenAt==null).isPresent()))
            .forEach(f->all.add(new TimelineEntry(f.id,f.createdAt,"FILE",f.uploaderId,f.displayName,null)));
        all.sort(Comparator.comparing(TimelineEntry::at).reversed().thenComparing(TimelineEntry::id,Comparator.reverseOrder()));
        int pages=Math.max(1,(all.size()+PAGE_SIZE-1)/PAGE_SIZE);int safe=Math.max(0,Math.min(page,pages-1));
        return new Page(all.subList(safe*PAGE_SIZE,Math.min(all.size(),(safe+1)*PAGE_SIZE)),safe,pages);
    }
    public String create(String actor,String group,String task,String body,List<String> mentionIds){
        task(actor,group,task);String text=body==null?"":body.strip();validate(text);
        var distinct=new LinkedHashSet<>(mentionIds==null?List.<String>of():mentionIds);
        if(distinct.size()>10)throw RuleViolation.invalid("Mỗi bình luận chỉ được nhắc tối đa 10 thành viên.");
        distinct.forEach(id->access.assignable(group,id));
        var now=now();var row=new TaskCommentEntity();row.id=UUID.randomUUID().toString();row.taskId=task;row.groupId=group;row.authorId=actor;row.body=text;row.createdAt=now;row.updatedAt=now;
        comments.saveAndFlush(row);
        var taskRow=tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);
        var recipients=new LinkedHashSet<String>();recipients.add(taskRow.assigneeId);recipients.add(taskRow.createdBy);recipients.addAll(distinct);
        for(String mentioned:distinct){var mention=new CommentMentionEntity();mention.id=UUID.randomUUID().toString();mention.commentId=row.id;mention.groupId=group;mention.mentionedUserId=mentioned;mention.createdAt=now;mentions.save(mention);}
        for(String recipient:recipients){if(recipient==null||recipient.equals(actor)||!access.canNotify(recipient,group))continue;
            var notification=new NotificationEntity();notification.id=UUID.randomUUID().toString();notification.eventKey=row.id;notification.recipientId=recipient;notification.groupId=group;notification.taskId=task;notification.commentId=row.id;notification.kind=distinct.contains(recipient)?"MENTION":"COMMENT";notification.createdAt=now;publisher.publish(notification);
        }
        notifications.flush();return row.id;
    }
    public void edit(String actor,String group,String task,String id,String body,long expected){edit(actor,group,task,id,body,expected,null);}
    public void edit(String actor,String group,String task,String id,String body,long expected,List<String> mentionIds){
        task(actor,group,task);var row=comments.findByIdAndTaskId(id,task).orElseThrow(RuleViolation::missing);
        if(row.hiddenAt!=null)throw RuleViolation.missing();if(!row.authorId.equals(actor))throw RuleViolation.forbidden();
        if(row.version==null||row.version!=expected)throw RuleViolation.conflict();String text=body==null?"":body.strip();validate(text);
        var revision=new TaskCommentRevisionEntity();revision.id=UUID.randomUUID().toString();revision.commentId=row.id;revision.editorId=actor;revision.body=row.body;revision.version=row.version;revision.revisedAt=now();revisions.save(revision);
        row.body=text;row.updatedAt=now();comments.flush();
        if(mentionIds!=null){
            var requested=new LinkedHashSet<>(mentionIds);if(requested.size()>10)throw RuleViolation.invalid("Mỗi bình luận chỉ được nhắc tối đa 10 thành viên.");requested.forEach(user->access.assignable(group,user));
            var old=mentions.findByCommentId(id);var previous=old.stream().map(m->m.mentionedUserId).collect(java.util.stream.Collectors.toSet());
            for(var m:old)if(!requested.contains(m.mentionedUserId))mentions.delete(m);
            String editEvent=UUID.randomUUID().toString();
            for(String user:requested)if(!previous.contains(user)){
                var m=new CommentMentionEntity();m.id=UUID.randomUUID().toString();m.commentId=id;m.groupId=group;m.mentionedUserId=user;m.createdAt=now();mentions.save(m);
                if(!user.equals(actor)){var n=new NotificationEntity();n.id=UUID.randomUUID().toString();n.eventKey=editEvent;n.recipientId=user;n.groupId=group;n.taskId=task;n.commentId=id;n.kind="MENTION";n.createdAt=now();publisher.publish(n);}
            }
        }
    }
    public void hide(String actor,String group,String task,String id){
        task(actor,group,task);var row=comments.findByIdAndTaskId(id,task).orElseThrow(RuleViolation::missing);
        if(row.hiddenAt!=null)throw RuleViolation.missing();var g=groups.findById(group).orElseThrow(RuleViolation::missing);
        if(!row.authorId.equals(actor)&&!g.ownerId.equals(actor))throw RuleViolation.forbidden();row.hiddenAt=now();row.hiddenBy=actor;pins.findByTaskIdAndCommentId(task,id).ifPresent(pins::delete);for(var file:files.findByTaskIdOrderByCreatedAtDescIdDesc(task))if(id.equals(file.commentId))pins.findByTaskIdAndAttachmentId(task,file.id).ifPresent(pins::delete);
    }
    @Transactional(readOnly=true) public List<TaskCommentRevisionEntity> revisions(String actor,String group,String task,String id){
        task(actor,group,task);var row=comments.findByIdAndTaskId(id,task).orElseThrow(RuleViolation::missing);
        if(row.hiddenAt!=null)throw RuleViolation.missing();if(!row.authorId.equals(actor))throw RuleViolation.forbidden();return revisions.findByCommentIdOrderByVersionAsc(id);
    }
    @Transactional(readOnly=true) public Map<String,Set<String>> mentionIds(String actor,String group,String task){task(actor,group,task);var result=new HashMap<String,Set<String>>();for(var c:comments.findByTaskIdOrderByCreatedAtDescIdDesc(task))if(c.hiddenAt==null)result.put(c.id,mentions.findByCommentId(c.id).stream().map(m->m.mentionedUserId).collect(java.util.stream.Collectors.toSet()));return result;}
    private void task(String actor,String group,String task){access.member(actor,group);tasks.findByIdAndGroupId(task,group).orElseThrow(RuleViolation::missing);}
    private void validate(String body){if(body.isBlank()||body.codePointCount(0,body.length())>4000)throw RuleViolation.invalid("Bình luận cần có nội dung và tối đa 4.000 ký tự Unicode.");}
    private java.time.Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
}
