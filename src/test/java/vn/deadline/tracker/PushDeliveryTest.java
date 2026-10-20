package vn.deadline.tracker;

import jakarta.servlet.http.Cookie;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.deadline.tracker.service.*;
import vn.deadline.tracker.domain.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"app.push.interval-ms=86400000","app.reminders.interval-ms=86400000"})
@AutoConfigureMockMvc @Import(PushDeliveryTest.Fakes.class)
class PushDeliveryTest extends DatabaseTest {
    static final Instant NOW=Instant.parse("2031-03-01T00:00:00Z");
    static final String PASSWORD="Synthetic push fixture passphrase 123!";
    static final KeyPair KEYS=keys();
    static final String PUBLIC=Base64.getUrlEncoder().withoutPadding().encodeToString(((org.bouncycastle.jce.interfaces.ECPublicKey)KEYS.getPublic()).getQ().getEncoded(false));
    static final String AUTH=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
    static KeyPair keys(){try{Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());var g=KeyPairGenerator.getInstance("EC","BC");g.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));return g.generateKeyPair();}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static class TestClock extends Clock {Instant value=NOW;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return value;}}
    static class FakeTransport implements PushTransport {List<Message> sent=new ArrayList<>();int status=201;String retry;boolean fail;public Result send(Message m)throws Exception{sent.add(m);if(fail)throw new java.io.IOException("Synthetic network interruption");return new Result(status,retry);}}
    @TestConfiguration static class Fakes {
        @Bean @Primary TestClock pushClock(){return new TestClock();}
        @Bean @Primary FakeTransport fakePush(){return new FakeTransport();}
        @Bean @Primary PushSettings syntheticPushSettings()throws Exception {
            byte[] scalar=((org.bouncycastle.jce.interfaces.ECPrivateKey)KEYS.getPrivate()).getD().toByteArray();byte[] fixed=new byte[32];System.arraycopy(scalar,Math.max(0,scalar.length-32),fixed,Math.max(0,32-scalar.length),Math.min(32,scalar.length));
            Path file=Files.createTempFile("w11-synthetic-vapid-",".key");Files.writeString(file,Base64.getUrlEncoder().withoutPadding().encodeToString(fixed));file.toFile().deleteOnExit();
            return new PushSettings(true,PUBLIC,file.toString(),"mailto:synthetic@example.invalid");
        }
    }
    @Autowired AccountService accounts;@Autowired WorkService work;@Autowired InvitationService invitations;
    @Autowired CommentService comments;@Autowired PushDeviceService devices;@Autowired PushDeliveryWorker worker;
    @Autowired NotificationService inbox;@Autowired SessionStore sessions;@Autowired ReminderPreferenceService preferences;@Autowired ReminderScheduler reminders;
    @Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;@Autowired MockMvc mvc;
    @Autowired FakeTransport fake;@Autowired TestClock clock;@Autowired PushSettings settings;
    String owner,recipient,other,group,task,device,username;
    String name(){return "p"+UUID.randomUUID().toString().replace("-","").substring(0,20);}
    String endpoint(){return "https://fcm.googleapis.com/fcm/send/synthetic-"+UUID.randomUUID();}
    Cookie login(String username)throws Exception{return mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",username).param("password",PASSWORD)).andExpect(status().is3xxRedirection()).andReturn().getResponse().getCookie("SESSION");}
    @BeforeEach void setup(){clock.value=NOW;fake.sent.clear();fake.status=201;fake.retry=null;fake.fail=false;owner=accounts.register(name(),"Push owner",PASSWORD);username=name();recipient=accounts.register(username,"Push recipient",PASSWORD);other=accounts.register(name(),"Other account",PASSWORD);group=work.createGroup(owner,"Push group","").getId();invitations.join(recipient,invitations.issue(owner,group));task=work.createTask(owner,group,"Push task","",recipient,NOW.plusSeconds(86400)).getId();device=devices.enable(recipient,endpoint(),PUBLIC,AUTH);}
    @AfterEach void cancel(){jdbc.update("UPDATE push_delivery SET state='CANCELLED' WHERE device_id IN (SELECT id FROM push_device WHERE user_id=?) AND state IN ('PENDING','LEASED')",recipient);devices.disableAll(recipient);}
    String comment(){return comments.create(owner,group,task,"Synthetic private body must not enter desktop payload",List.of(recipient));}
    String delivery(String comment){return jdbc.queryForObject("SELECT o.id FROM push_delivery o JOIN notification n ON n.id=o.notification_id WHERE n.comment_id=? AND o.device_id=?",String.class,comment,device);}
    String state(String id){return jdbc.queryForObject("SELECT state FROM push_delivery WHERE id=?",String.class,id);}
    @Autowired EventPreferenceService eventPreferences;
    @Test void eventPreferencesSuppressNewInboxAndCancelPendingDesktop()throws Exception {
        String first=comment(),pending=delivery(first);int before=inbox.inbox(recipient).size();
        eventPreferences.update(recipient,true,true,false);
        worker.runOnce();assertEquals("CANCELLED",state(pending));assertTrue(fake.sent.isEmpty());
        String suppressed=comment();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE comment_id=? AND recipient_id=?",Integer.class,suppressed,recipient));
        assertEquals(before,inbox.inbox(recipient).size());
        Cookie cookie=login(username);
        mvc.perform(post("/account/events").cookie(cookie).param("mentions","true")).andExpect(status().isForbidden());
        mvc.perform(post("/account/events").cookie(cookie).with(csrf()).param("mentions","true")).andExpect(status().is3xxRedirection());
        assertTrue(eventPreferences.get(recipient).mentions());assertFalse(eventPreferences.get(recipient).comments());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE comment_id=? AND recipient_id=?",Integer.class,comment(),recipient));
    }
    @Test void rollbackHasNoInboxOrDeliveryAndCommitSendsOnce(){
        final String[] rolled={null};new TransactionTemplate(manager).executeWithoutResult(tx->{rolled[0]=comment();tx.setRollbackOnly();});
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE comment_id=?",Integer.class,rolled[0]));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM push_delivery o JOIN notification n ON n.id=o.notification_id WHERE n.comment_id=?",Integer.class,rolled[0]));assertTrue(fake.sent.isEmpty());
        String comment=comment(),id=delivery(comment);assertEquals("PENDING",state(id));assertEquals(1,worker.runOnce());assertEquals("ACCEPTED",state(id));assertEquals(0,worker.runOnce());assertEquals(1,fake.sent.size());UUID.fromString(fake.sent.getFirst().notificationId());
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).execute(tx->worker.runOnce()));
    }
    @Test void retryAfterLeaseExpiryAndNetworkFailureRemainBounded(){
        String id=delivery(comment());fake.status=429;fake.retry="999999";assertEquals(1,worker.runOnce());assertEquals("PENDING",state(id));
        assertEquals(NOW.plusSeconds(3600),jdbc.queryForObject("SELECT next_attempt_at FROM push_delivery WHERE id=?",Timestamp.class,id).toInstant());
        clock.value=NOW.plusSeconds(3601);fake.fail=true;worker.runOnce();assertEquals("PENDING",state(id));
        jdbc.update("UPDATE push_delivery SET state='LEASED',lease_token=?,lease_until=? WHERE id=?",UUID.randomUUID().toString(),Timestamp.from(clock.value.plusSeconds(120)),id);
        assertEquals(0,worker.runOnce());clock.value=clock.value.plusSeconds(121);fake.fail=false;fake.status=201;worker.runOnce();assertEquals("ACCEPTED",state(id));
        assertEquals(3,fake.sent.size());assertEquals(60,PushDeliveryWorker.retrySeconds("-2",NOW));assertEquals(60,PushDeliveryWorker.retrySeconds("nonsense",NOW));
    }
    @Test void expiredProviderRevokesOnlyItsGrantAndConfigurationErrorsKeepInbox(){
        for(int status:List.of(401,403,404,410,302,503)){
            device=devices.enable(recipient,endpoint(),PUBLIC,AUTH);String id=delivery(comment());fake.status=status;worker.runOnce();
            assertEquals(status==401||status==403?"CONFIG_ERROR":status==404||status==410?"EXPIRED":status==503?"PENDING":"FAILED",state(id));
            if(status==404||status==410)assertEquals(0,jdbc.queryForObject("SELECT enabled FROM push_device WHERE id=?",Integer.class,device));
            jdbc.update("UPDATE push_delivery SET state='CANCELLED' WHERE id=?",id);devices.disable(recipient,device);
        }
        assertFalse(inbox.inbox(recipient).isEmpty());
    }
    @Test void everyAttemptRechecksGrantAccountMembershipAndPreferences(){
        long oldEpoch=devices.sessionEpoch(recipient);String id=delivery(comment());sessions.invalidateAll(username);assertThrows(RuleViolation.class,()->devices.enableForSession(recipient,endpoint(),PUBLIC,AUTH,oldEpoch));worker.runOnce();assertEquals("CANCELLED",state(id));assertTrue(fake.sent.isEmpty());
        device=devices.enable(recipient,endpoint(),PUBLIC,AUTH);id=delivery(comment());devices.disableAll(recipient);worker.runOnce();assertEquals("CANCELLED",state(id));
        device=devices.enable(recipient,endpoint(),PUBLIC,AUTH);id=delivery(comment());jdbc.update("UPDATE app_user SET enabled=FALSE WHERE id=?",recipient);worker.runOnce();assertEquals("CANCELLED",state(id));jdbc.update("UPDATE app_user SET enabled=TRUE WHERE id=?",recipient);
        preferences.update(recipient,true,true,true);reminders.runOnce(NOW);String reminderId=jdbc.queryForObject("SELECT o.id FROM push_delivery o JOIN notification n ON n.id=o.notification_id WHERE n.kind='REMINDER' AND o.device_id=?",String.class,device);
        preferences.update(recipient,false,true,true);worker.runOnce();assertEquals("CANCELLED",state(reminderId));assertTrue(fake.sent.isEmpty());
    }
    @Test void deviceOwnershipCsrfLogoutAccountSwitchAndOpaqueDeepLink()throws Exception {
        Cookie cookie=login(username);String newEndpoint=endpoint();String json="{\"endpoint\":\""+newEndpoint+"\",\"p256dh\":\""+PUBLIC+"\",\"auth\":\""+AUTH+"\",\"actorId\":\""+other+"\"}";
        mvc.perform(post("/api/push/devices").contentType("application/json").content(json)).andExpect(status().isForbidden());
        mvc.perform(get("/api/push/config")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/push/devices").cookie(cookie).with(csrf().asHeader()).contentType("application/json").content(json)).andExpect(status().isOk());
        String registered=jdbc.queryForObject("SELECT id FROM push_device WHERE endpoint=? AND user_id=?",String.class,newEndpoint,recipient);
        mvc.perform(post("/api/push/devices/"+registered+"/disable").cookie(login(jdbc.queryForObject("SELECT username FROM app_user WHERE id=?",String.class,other))).with(csrf())).andExpect(status().isNotFound());
        var config=mvc.perform(get("/api/push/config").cookie(cookie)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(config.contains(newEndpoint));assertFalse(config.contains(AUTH));
        String comment=comment(),notification=jdbc.queryForObject("SELECT id FROM notification WHERE comment_id=? AND recipient_id=?",String.class,comment,recipient);
        mvc.perform(get("/notifications/"+notification).cookie(cookie)).andExpect(status().isOk());assertNull(jdbc.queryForObject("SELECT read_at FROM notification WHERE id=?",Timestamp.class,notification));
        mvc.perform(get("/notifications/"+notification)).andExpect(redirectedUrl(vn.deadline.tracker.security.ReturnPath.login("/notifications/"+notification,"continue")));
        mvc.perform(post("/login").cookie(cookie).with(csrf()).param("username",jdbc.queryForObject("SELECT username FROM app_user WHERE id=?",String.class,other)).param("password",PASSWORD)).andExpect(status().is3xxRedirection());
        assertEquals(0,jdbc.queryForObject("SELECT enabled FROM push_device WHERE id=?",Integer.class,registered));
        cookie=login(username);mvc.perform(post("/logout").cookie(cookie).with(csrf())).andExpect(status().is3xxRedirection());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM push_device WHERE user_id=? AND enabled=TRUE",Integer.class,recipient));
    }
    @Test void ssrfPolicyRejectsPrivateMixedDnsAndRebindingBeforeSocket()throws Exception {
        var policy=new PushEndpointPolicy();
        for(String bad:List.of("http://fcm.googleapis.com/x","https://127.0.0.1/x","https://fcm.googleapis.com.evil.invalid/x","https://fcm.googleapis.com:8443/x","https://user@fcm.googleapis.com/x","https://fcm.googleapis.com/x#fragment","https://169.254.169.254/latest"))assertThrows(IllegalArgumentException.class,()->policy.endpoint(bad));
        for(String bad:List.of("127.0.0.1","10.0.0.1","100.64.0.1","192.0.2.1","198.18.0.1","203.0.113.1","::1","fe80::1","fc00::1","2001:db8::1","2002:7f00:1::"))assertFalse(policy.publicAddress(InetAddress.getByName(bad)),bad);
        assertThrows(UnknownHostException.class,()->policy.checked(new InetAddress[]{InetAddress.getByName("8.8.8.8"),InetAddress.getByName("127.0.0.1")}));
        var blocked=new PushEndpointPolicy(){@Override public InetAddress[] resolve(String host)throws UnknownHostException{return checked(new InetAddress[]{InetAddress.getByName("127.0.0.1")});}};
        assertThrows(Exception.class,()->new ProviderPushTransport(settings,blocked).send(new PushTransport.Message(endpoint(),PUBLIC,AUTH,UUID.randomUUID().toString())));
        class Encoder extends nl.martijndwars.webpush.AbstractPushService<Encoder>{Encoder(){super(KEYS,"mailto:synthetic@example.invalid");}nl.martijndwars.webpush.HttpRequest encode()throws Exception{return prepareRequest(new nl.martijndwars.webpush.Notification(endpoint(),PUBLIC,AUTH,"{\"id\":\"opaque\"}"),nl.martijndwars.webpush.Encoding.AES128GCM);}}
        var encrypted=new Encoder().encode();assertTrue(encrypted.getHeaders().keySet().stream().anyMatch(k->k.equalsIgnoreCase("Authorization")));assertFalse(new String(encrypted.getBody(),java.nio.charset.StandardCharsets.UTF_8).contains("opaque"));
    }
}
