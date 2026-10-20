package vn.deadline.tracker.service;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import vn.deadline.tracker.persistence.*;
import java.nio.charset.StandardCharsets;import java.time.*;import java.time.temporal.ChronoUnit;import java.util.*;
@Component
public class ReminderScheduler {
 private record DueTask(String id,String group,String assignee,Instant due,long version){}
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;private final NotificationPreferenceRepository preferences;private final NotificationRepository notifications;private final GroupAccess access;private final Clock clock;private final NotificationPublisher publisher;
 public ReminderScheduler(org.springframework.jdbc.core.JdbcTemplate jdbc,NotificationPreferenceRepository preferences,NotificationRepository notifications,GroupAccess access,Clock clock,NotificationPublisher publisher){this.jdbc=jdbc;this.preferences=preferences;this.notifications=notifications;this.access=access;this.clock=clock;this.publisher=publisher;}
 @Scheduled(fixedDelayString="${app.reminders.interval-ms:60000}") @Transactional public void scheduled(){runOnce(clock.instant());}
 @Transactional public int runOnce(Instant instant){Instant now=instant.truncatedTo(ChronoUnit.SECONDS);int count=0;count+=window(now,Duration.ofHours(24),true);count+=window(now,Duration.ofHours(1),false);return count;}
 private int window(Instant now,Duration offset,boolean day){Instant start=now.plus(offset),end=start.plusSeconds(60);var tasks=jdbc.query("SELECT id,group_id,assignee_id,due_at,version FROM task WHERE status<>'DONE' AND due_at>=? AND due_at<?",(rs,n)->new DueTask(rs.getString(1),rs.getString(2),rs.getString(3),rs.getTimestamp(4).toInstant(),rs.getLong(5)),java.sql.Timestamp.from(start),java.sql.Timestamp.from(end));int created=0;
  for(var task:tasks){var pref=preferences.findById(task.assignee()).orElse(null);if(pref==null||!pref.remindersEnabled||(day&&!pref.remind24h)||(!day&&!pref.remind1h)||!access.canNotify(task.assignee(),task.group()))continue;
   String material=task.id()+"|"+task.due()+"|"+task.version()+"|"+offset.toSeconds()+"|"+task.assignee();String key=UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();if(notifications.existsByEventKeyAndRecipientId(key,task.assignee()))continue;
   var row=new NotificationEntity();row.id=UUID.randomUUID().toString();row.eventKey=key;row.recipientId=task.assignee();row.groupId=task.group();row.taskId=task.id();row.kind="REMINDER";row.reminderAssigneeId=task.assignee();row.reminderDueAt=task.due();row.reminderTaskVersion=task.version();row.createdAt=now;row.reminderOffset=(int)offset.toSeconds();publisher.publish(row);created++;
  }
  notifications.flush();return created;
 }
}
