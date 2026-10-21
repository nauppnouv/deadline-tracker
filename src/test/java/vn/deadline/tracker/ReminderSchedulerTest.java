package vn.deadline.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import vn.deadline.tracker.domain.RuleViolation;
import vn.deadline.tracker.persistence.NotificationRepository;
import vn.deadline.tracker.persistence.TaskRepository;
import vn.deadline.tracker.service.*;

import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(ReminderSchedulerTest.ClockConfig.class)
class ReminderSchedulerTest extends DatabaseTest {
    static final Instant NOW=Instant.parse("2030-01-01T00:00:00Z");
    static class MutableClock extends Clock {
        volatile Instant now=NOW;
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
    @TestConfiguration static class ClockConfig {
        @Bean @Primary MutableClock reminderClock(){return new MutableClock();}
    }
    @Autowired AccountService accounts;
    @Autowired WorkService work;
    @Autowired InvitationService invitations;
    @Autowired ReminderPreferenceService preferences;
    @Autowired ReminderScheduler scheduler;
    @Autowired NotificationService notifications;
    @Autowired NotificationRepository notificationRows;
    @Autowired TaskRepository taskRows;

    String user(){return accounts.register("r"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Reminder fixture","Synthetic reminder fixture passphrase 123!");}

    @Test void bothOffsetsAreOptInDeduplicatedAndStaleAfterTaskChange(){
        String actor=user(),group=work.createGroup(actor,"Reminder group","").getId();
        var day=work.createTask(actor,group,"24h reminder","",actor,NOW.plus(Duration.ofHours(24)));
        var hour=work.createTask(actor,group,"1h reminder","",actor,NOW.plus(Duration.ofHours(1)));
        preferences.update(actor,true,true,true);

        assertEquals(2,scheduler.runOnce(NOW));
        assertEquals(0,scheduler.runOnce(NOW));
        assertEquals(2,notifications.inbox(actor).size());
        var original=notifications.inbox(actor);

        var row=taskRows.findById(day.getId()).orElseThrow();
        row.dueAt=row.dueAt.plusSeconds(120);
        taskRows.saveAndFlush(row);
        assertEquals(1,notifications.inbox(actor).size());
        var stale=original.stream().filter(n->n.taskId.equals(day.getId())).findFirst().orElseThrow();
        assertThrows(RuleViolation.class,()->notifications.open(actor,stale.id));
        assertEquals(0,scheduler.runOnce(NOW));
        String newAssignee=user();invitations.join(newAssignee,invitations.issue(actor,group));
        var reassigned=taskRows.findById(hour.getId()).orElseThrow();reassigned.assigneeId=newAssignee;taskRows.saveAndFlush(reassigned);
        assertTrue(notifications.inbox(actor).isEmpty());
        var staleAssignee=original.stream().filter(n->n.taskId.equals(hour.getId())).findFirst().orElseThrow();
        assertThrows(RuleViolation.class,()->notifications.open(actor,staleAssignee.id));
    }

    @Test void disabledPreferencesProduceNoRowsAndDoneTasksAreNotReminded(){
        String actor=user(),group=work.createGroup(actor,"Opt out group","").getId();
        var task=work.createTask(actor,group,"No reminder","",actor,NOW.plus(Duration.ofHours(1)));
        assertEquals(0,scheduler.runOnce(NOW));
        assertTrue(notifications.inbox(actor).isEmpty());
        preferences.update(actor,true,false,true);
        taskRows.findById(task.getId()).ifPresent(row->{row.status=vn.deadline.tracker.domain.TaskStatus.DONE;row.completedAt=NOW;taskRows.saveAndFlush(row);});
        assertEquals(0,scheduler.runOnce(NOW));
        assertTrue(notifications.inbox(actor).isEmpty());
    }
}
