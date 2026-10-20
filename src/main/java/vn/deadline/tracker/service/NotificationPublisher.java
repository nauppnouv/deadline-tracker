package vn.deadline.tracker.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.persistence.*;

@Service @Transactional
public class NotificationPublisher {
    private final NotificationRepository notifications;private final JdbcTemplate jdbc;private final Clock clock;private final EventPreferenceService preferences;
    public NotificationPublisher(NotificationRepository notifications,JdbcTemplate jdbc,Clock clock,EventPreferenceService preferences){this.notifications=notifications;this.jdbc=jdbc;this.clock=clock;this.preferences=preferences;}
    public void publish(NotificationEntity row) {
        if(!preferences.allows(row.recipientId,row.kind))return;
        notifications.saveAndFlush(row);
        // Same transaction as the business operation; only committed rows can be claimed by the sender.
        var now=Timestamp.from(clock.instant());var expires=Timestamp.from(clock.instant().plusSeconds(86400));
        for(var device:jdbc.queryForList("SELECT d.id,d.grant_epoch FROM push_device d JOIN push_preference p ON p.user_id=d.user_id WHERE d.user_id=? AND d.enabled=TRUE AND p.enabled=TRUE",row.recipientId))
            jdbc.update("INSERT INTO push_delivery(id,notification_id,device_id,grant_epoch,next_attempt_at,created_at,expires_at) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID().toString(),row.id,device.get("id"),device.get("grant_epoch"),now,now,expires);
    }
    public void assignment(String actor,String group,String task,String recipient,String eventKey) {
        if(recipient.equals(actor))return;
        var row=new NotificationEntity();row.id=UUID.randomUUID().toString();row.eventKey=eventKey;row.recipientId=recipient;row.groupId=group;row.taskId=task;row.kind="TASK_ASSIGNMENT";row.createdAt=clock.instant().truncatedTo(ChronoUnit.MICROS);publish(row);
    }
}
