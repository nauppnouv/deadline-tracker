package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.deadline.tracker.domain.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TaskDomainTest {
    static final Instant NOW=Instant.parse("2026-10-17T13:00:00Z");
    static final Clock CLOCK=Clock.fixed(NOW, ZoneOffset.UTC);
    Group group;
    @BeforeEach void setup() { group=Group.create("Project","","owner"); group.addMember("creator"); group.addMember("worker"); group.addMember("other"); }
    Task task() { return Task.create(group,"creator","  Database  ",null,"worker",NOW.plusSeconds(3600),CLOCK); }
    @Test @DisplayName("US-03 UAT-04 BR-01/02/03/04/05 valid task and invalid inputs")
    void createValidAndRejectBadInput() {
        var t=task(); assertEquals("Database",t.getTitle()); assertEquals("",t.getDescription());
        assertEquals(TaskStatus.TODO,t.getStatus()); assertEquals("worker",t.getAssigneeId()); assertEquals("creator",t.getCreatedBy());
        assertThrows(RuleViolation.class,()->Task.create(group,"outsider","X","","worker",NOW.plusSeconds(60),CLOCK));
        for (String title:Arrays.asList(null,"","  ","x".repeat(151))) assertThrows(RuleViolation.class,()->Task.create(group,"creator",title,"","worker",NOW.plusSeconds(60),CLOCK));
        assertThrows(RuleViolation.class,()->Task.create(group,"creator","X","x".repeat(2001),"worker",NOW.plusSeconds(60),CLOCK));
        assertThrows(RuleViolation.class,()->Task.create(group,"creator","X","","outsider",NOW.plusSeconds(60),CLOCK));
        for (Instant due:Arrays.asList(null,NOW,NOW.minusNanos(1))) assertThrows(RuleViolation.class,()->Task.create(group,"creator","X","","worker",due,CLOCK));
    }
    @Test @DisplayName("US-05 UAT-05 BR-06 only current assignee changes status")
    void assigneeOnly() {
        var t=task();
        for (String actor:List.of("owner","creator","other","outsider")) assertThrows(RuleViolation.class,()->t.updateStatus(group,actor,TaskStatus.DONE,CLOCK));
        assertThrows(RuleViolation.class,()->t.updateStatus(group,"worker",null,CLOCK));
        assertTrue(t.updateStatus(group,"worker",TaskStatus.IN_PROGRESS,CLOCK));
        assertEquals(TaskStatus.TODO,task().getStatus());
    }
    @ParameterizedTest @EnumSource(TaskStatus.class)
    @DisplayName("US-05 UAT-07 BR-07/20 all transitions and idempotent same status")
    void transitions(TaskStatus initial) {
        for (TaskStatus next:TaskStatus.values()) {
            var t=task(); t.updateStatus(group,"worker",initial,CLOCK);
            assertEquals(initial!=next,t.updateStatus(group,"worker",next,CLOCK));
            assertEquals(next,t.getStatus());
            assertEquals(next==TaskStatus.DONE ? NOW : null,t.getCompletedAt());
            assertFalse(t.updateStatus(group,"worker",next,CLOCK));
        }
    }
    @Test @DisplayName("US-06 UAT-06 BR-08/09 reassign resets and revokes previous assignee")
    void reassign() {
        var t=task(); t.updateStatus(group,"worker",TaskStatus.IN_PROGRESS,CLOCK);
        assertThrows(RuleViolation.class,()->t.edit(group,"other","X","","other",t.getDueAt(),CLOCK));
        assertEquals(Task.Change.REASSIGNED,t.edit(group,"owner","X","","owner",t.getDueAt(),CLOCK));
        assertEquals(TaskStatus.TODO,t.getStatus());
        assertThrows(RuleViolation.class,()->t.updateStatus(group,"worker",TaskStatus.DONE,CLOCK));
        assertTrue(t.updateStatus(group,"owner",TaskStatus.DONE,CLOCK));
        assertThrows(RuleViolation.class,()->t.edit(group,"owner","X","","other",t.getDueAt(),CLOCK));
        assertEquals(Task.Change.UPDATED,t.edit(group,"creator","Renamed","","owner",t.getDueAt(),CLOCK));
    }
    @Test @DisplayName("US-06 UAT-09/18 K-2 past due can load/edit/reassign without changing deadline")
    void pastDueAndMinutePrecision() {
        var old=Task.restore("task-id",group.getId(),"Old","","worker","creator",NOW.minusSeconds(70),TaskStatus.IN_PROGRESS,null,NOW.minusSeconds(500),NOW.minusSeconds(100),4);
        assertEquals(Task.Change.UPDATED,old.edit(group,"creator","Updated","description","worker",old.getDueAt().minusSeconds(10),CLOCK));
        assertEquals(NOW.minusSeconds(70),old.getDueAt());
        assertEquals(Task.Change.REASSIGNED,old.edit(group,"owner",old.getTitle(),"","other",old.getDueAt(),CLOCK));
        assertEquals(TaskStatus.TODO,old.getStatus());
        assertThrows(RuleViolation.class,()->old.edit(group,"owner","X","","other",NOW.minusSeconds(3600),CLOCK));
    }
    @Test @DisplayName("US-06 BR-09 same assignee and same fields do not reset or create events")
    void unchangedDoesNotReset() {
        var t=task(); t.updateStatus(group,"worker",TaskStatus.IN_PROGRESS,CLOCK);
        assertEquals(Task.Change.NONE,t.edit(group,"creator",t.getTitle(),t.getDescription(),"worker",t.getDueAt(),CLOCK));
        assertEquals(TaskStatus.IN_PROGRESS,t.getStatus());
        assertThrows(RuleViolation.class,()->t.edit(group,"creator","Changed","","outsider",t.getDueAt(),CLOCK));
        assertEquals("Database",t.getTitle());
        var otherGroup=Group.create("Other","","worker");
        assertThrows(RuleViolation.class,()->t.updateStatus(otherGroup,"worker",TaskStatus.DONE,CLOCK));
    }
    @Test @DisplayName("US-04 UAT-08 BR-10/11 exact deadline and 24h boundaries")
    void deadlineBoundaries() {
        var t=task(); Instant due=t.getDueAt();
        assertFalse(t.isOverdue(due.minusNanos(1))); assertFalse(t.isOverdue(due)); assertTrue(t.isOverdue(due.plusNanos(1)));
        assertTrue(t.isDueSoon(due)); assertTrue(t.isDueSoon(due.minusSeconds(86400)));
        assertFalse(t.isDueSoon(due.minusSeconds(86400).minusNanos(1))); assertFalse(t.isDueSoon(due.plusNanos(1)));
        t.updateStatus(group,"worker",TaskStatus.DONE,CLOCK);
        assertFalse(t.isOverdue(due.plusSeconds(1))); assertFalse(t.isDueSoon(due));
    }
    @Test @DisplayName("US-04 UAT-12 BR-19 combined filtering and unfiltered group totals")
    void filtersAndProgress() {
        var a=task(); var b=task(); b.updateStatus(group,"worker",TaskStatus.DONE,CLOCK);
        assertTrue(new TaskFilter("worker",TaskStatus.TODO,TaskFilter.Due.SOON).matches(a,NOW));
        assertFalse(new TaskFilter("other",null,TaskFilter.Due.ALL).matches(a,NOW));
        assertFalse(new TaskFilter(null,TaskStatus.DONE,TaskFilter.Due.ALL).matches(a,NOW));
        assertEquals(50,Progress.of(List.of(a,b),NOW).percent());
        assertEquals(0,Progress.of(List.of(),NOW).percent());
        assertEquals(2,Progress.of(List.of(a,b),NOW).total());
    }
}
