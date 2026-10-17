package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import vn.deadline.tracker.persistence.*;
import vn.deadline.tracker.service.WorkService;
import vn.deadline.tracker.domain.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PersistenceTest extends DatabaseTest {
    @Autowired WorkService work;
    @Autowired AccountRepository accounts;
    @Autowired TaskRepository tasks;
    @Autowired TaskEventRepository events;
    @Autowired InvitationStore invitationStore;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    String owner,member,outsider,group;
    @BeforeEach void seed() {
        owner=user(); member=user(); outsider=user();
        group=work.createGroup(owner,"Database test","").getId();
        jdbc.update("INSERT INTO group_member (id,group_id,user_id,role,joined_at) VALUES (?,?,?,'MEMBER',?)",UUID.randomUUID().toString(),group,member,java.sql.Timestamp.from(Instant.now()));
    }
    String user() {
        var a=new AccountEntity(); a.id=UUID.randomUUID().toString();a.username="u"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        a.displayName="Same name"; a.passwordHash="disabled-test-fixture"; a.createdAt=Instant.now(); a.enabled=true;
        return accounts.saveAndFlush(a).id;
    }
    Task task() { return work.createTask(owner,group,"Persisted","",member,Instant.now().plusSeconds(7200)); }
    @Test @DisplayName("DATA-01/02 UAT-02 unique memberships, owner and foreign keys enforced by MariaDB")
    void constraints() {
        assertThrows(Exception.class,()->jdbc.update("INSERT INTO group_member(id,group_id,user_id,role,joined_at) VALUES(?,?,?,'MEMBER',NOW())",UUID.randomUUID().toString(),group,member));
        assertThrows(Exception.class,()->jdbc.update("INSERT INTO group_member(id,group_id,user_id,role,joined_at) VALUES(?,?,?,'OWNER',NOW())",UUID.randomUUID().toString(),group,outsider));
        assertThrows(Exception.class,()->jdbc.update("UPDATE project_group SET owner_id=NULL WHERE id=?",group));
        assertThrows(Exception.class,()->jdbc.update("UPDATE project_group SET owner_id=? WHERE id=?","missing-user",group));
        assertThrows(Exception.class,()->work.createTask(owner,group,"X","",outsider,Instant.now().plusSeconds(60)));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1",Integer.class)>0);
        var t=task();
        assertThrows(Exception.class,()->jdbc.update("UPDATE task SET assignee_id=? WHERE id=?",outsider,t.getId()));
        assertThrows(Exception.class,()->jdbc.update("UPDATE task SET status='INVALID' WHERE id=?",t.getId()));
        assertThrows(Exception.class,()->jdbc.update("UPDATE task SET title='' WHERE id=?",t.getId()));
    }
    @Test @DisplayName("DATA-03 UAT-04 transaction rollback leaves no group or owner membership")
    void groupRollback() {
        int groups=jdbc.queryForObject("SELECT COUNT(*) FROM project_group",Integer.class);
        int members=jdbc.queryForObject("SELECT COUNT(*) FROM group_member",Integer.class);
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(status->{work.createGroup(owner,"Rollback","");throw new IllegalStateException("forced rollback");}));
        assertEquals(groups,jdbc.queryForObject("SELECT COUNT(*) FROM project_group",Integer.class));
        assertEquals(members,jdbc.queryForObject("SELECT COUNT(*) FROM group_member",Integer.class));
    }
    @Test @DisplayName("DATA-03 UAT-07 atomic task and event, idempotency, rollback")
    void eventAtomicity() {
        var t=task(); long before=events.count();
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(status->{work.changeStatus(member,group,t.getId(),TaskStatus.DONE,t.getVersion());throw new IllegalStateException("forced rollback");}));
        assertEquals(TaskStatus.TODO,work.task(owner,group,t.getId()).getStatus());assertEquals(before,events.count());
        var done=work.changeStatus(member,group,t.getId(),TaskStatus.DONE,t.getVersion());
        assertNotNull(done.getCompletedAt());assertEquals(before+1,events.count());
        work.changeStatus(member,group,t.getId(),TaskStatus.DONE,done.getVersion()); assertEquals(before+1,events.count());
    }
    @Test @DisplayName("DATA-04 UAT-11 stale forms and previous assignee cannot overwrite reassignment")
    void staleWrite() {
        var t=task(); var changed=work.editTask(owner,group,t.getId(),"Updated","",owner,t.getDueAt(),t.getVersion());
        assertEquals(TaskStatus.TODO,changed.getStatus());
        assertThrows(RuleViolation.class,()->work.changeStatus(member,group,t.getId(),TaskStatus.DONE,t.getVersion()));
        assertThrows(RuleViolation.class,()->work.editTask(owner,group,t.getId(),"Stale","",member,t.getDueAt(),t.getVersion()));
        assertEquals(owner,work.task(owner,group,t.getId()).getAssigneeId());
    }
    @Test @DisplayName("DATA-06/10 UAT-09/18 reload overdue and edit without resetting deadline")
    void reloadPastDue() {
        var t=task(); Instant past=Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update("UPDATE task SET due_at=? WHERE id=?",java.sql.Timestamp.from(past),t.getId());
        var loaded=work.task(member,group,t.getId()); assertEquals(past,loaded.getDueAt());
        var updated=work.editTask(owner,group,t.getId(),"Now overdue","",owner,past,loaded.getVersion());
        assertEquals(past,updated.getDueAt());assertTrue(updated.isOverdue(Instant.now()));
        assertThrows(RuleViolation.class,()->work.task(outsider,group,t.getId()));
    }
    @Test @DisplayName("DATA-04 actual optimistic locking with two independent transactions")
    void actualOptimisticLock() throws Exception {
        var t=task();var ready=new java.util.concurrent.CountDownLatch(2);var go=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var writes=new ArrayList<java.util.concurrent.Future<Boolean>>();
            for(int i=0;i<2;i++) writes.add(pool.submit(()->{try {new TransactionTemplate(transactions).execute(status->{var row=tasks.findById(t.getId()).orElseThrow();ready.countDown();try {go.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}row.title=UUID.randomUUID().toString();tasks.flush();return null;});return true;}catch(org.springframework.dao.OptimisticLockingFailureException e){return false;}}));
            assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));go.countDown();
            assertEquals(1,writes.stream().map(f->{try{return f.get()?1:0;}catch(Exception e){throw new RuntimeException(e);}}).mapToInt(Integer::intValue).sum());
        }
    }
    @Test @DisplayName("DATA-05 database conditional claim is atomic for one invitation and two users")
    void invitationClaimPrimitive() throws Exception {
        String invite=UUID.randomUUID().toString();String secondOutsider=user();
        jdbc.update("INSERT INTO invitation(id,group_id,token_hash,created_by,created_at,expires_at,version) VALUES(?,?,?,?,NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY),0)",invite,group,UUID.randomUUID().toString().replace("-","").repeat(2),owner);
        var ready=new java.util.concurrent.CountDownLatch(2);var go=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var claims=new ArrayList<java.util.concurrent.Future<Integer>>();
            for(String candidate:List.of(outsider,secondOutsider)) claims.add(pool.submit(()->{ready.countDown();go.await();return invitationStore.claim(invite,candidate)==InvitationStore.Claim.JOINED?1:0;}));
            assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));go.countDown();
            assertEquals(1,claims.get(0).get()+claims.get(1).get());
        }
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=? AND user_id IN (?,?)",Integer.class,group,outsider,secondOutsider));
    }
}
