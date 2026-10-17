package vn.deadline.tracker;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import vn.deadline.tracker.service.*;
import vn.deadline.tracker.persistence.*;
import vn.deadline.tracker.domain.*;
import java.time.*;import java.util.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Import(InvitationTest.Clocks.class)
class InvitationTest extends DatabaseTest {
    static final Instant NOW=Instant.parse("2026-10-17T13:00:00Z");
    static class TestClock extends Clock {volatile Instant now=NOW;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now;}}
    @TestConfiguration static class Clocks {@Bean @Primary TestClock invitationClock(){return new TestClock();}}
    @Autowired InvitationService invites;@Autowired WorkService work;@Autowired AccountService accounts;
    @Autowired InvitationRepository rows;@Autowired MemberRepository members;@Autowired TestClock clock;
    String owner,member,outsider,group;
    String user(){return accounts.register("u"+UUID.randomUUID().toString().replace("-","").substring(0,20),"Same name","Test fixture passphrase 123!");}
    @BeforeEach void seed(){clock.now=NOW;owner=user();member=user();outsider=user();group=work.createGroup(owner,"Private group","").getId();}
    @Test @DisplayName("US-13 SEC-07 BR-16 token 256 bits, hashed-only, owner permission")
    void issue(){
        String token=invites.issue(owner,group);assertEquals(32,Base64.getUrlDecoder().decode(token).length);
        assertNotEquals(token,invites.issue(owner,group));
        assertTrue(rows.findAll().stream().noneMatch(i->i.tokenHash.equals(token)));
        assertEquals("Private group",invites.preview(member,token).groupName());
        assertThrows(RuleViolation.class,()->invites.issue(member,group));
        assertThrows(RuleViolation.class,()->invites.preview(member,"not-a-token"));
    }
    @Test @DisplayName("US-14 UAT-02/03 BR-17 join, already-member idempotency and used token rejection")
    void join(){
        String token=invites.issue(owner,group);var row=rows.findByTokenHash(InvitationService.hash(token)).orElseThrow();
        assertEquals(group,invites.join(owner,token));assertNull(rows.findById(row.id).orElseThrow().usedAt);
        assertEquals(group,invites.join(member,token));assertTrue(members.existsByGroupIdAndUserId(group,member));
        assertThrows(RuleViolation.class,()->invites.join(outsider,token));
        assertThrows(RuleViolation.class,()->invites.join(member,token));
    }
    @Test @DisplayName("US-13 UAT-03 BR-16 expires exactly at 24h, revoke owner only")
    void expiryAndRevoke(){
        String token=invites.issue(owner,group);clock.now=NOW.plusSeconds(86400).minusNanos(1);assertNotNull(invites.preview(member,token));
        clock.now=NOW.plusSeconds(86400);assertThrows(RuleViolation.class,()->invites.join(member,token));
        clock.now=NOW;String other=invites.issue(owner,group);String id=rows.findByTokenHash(InvitationService.hash(other)).orElseThrow().id;
        assertThrows(RuleViolation.class,()->invites.revoke(member,group,id));invites.revoke(owner,group,id);
        assertThrows(RuleViolation.class,()->invites.preview(member,other));
        assertFalse(members.existsByGroupIdAndUserId(group,member));
    }
    @Test @DisplayName("US-14 DATA-05 SEC-07 two concurrent people use actual token service: one joins")
    void concurrentJoin()throws Exception{
        String token=invites.issue(owner,group);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var results=new ArrayList<Future<Boolean>>();
            for(String candidate:List.of(member,outsider))results.add(pool.submit(()->{ready.countDown();go.await();try{invites.join(candidate,token);return true;}catch(RuleViolation expected){return false;}}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();assertEquals(1,(results.get(0).get()?1:0)+(results.get(1).get()?1:0));
        }
        assertEquals(2,members.findByGroupIdOrderByJoinedAtAsc(group).size());
    }
}
