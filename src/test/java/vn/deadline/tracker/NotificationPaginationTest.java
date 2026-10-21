package vn.deadline.tracker;

import java.net.http.HttpClient;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.web.util.HtmlUtils;
import vn.deadline.tracker.service.NotificationService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "server.address=127.0.0.1",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector=vn.deadline.tracker.NotificationPaginationTest$SqlCapture"})
class NotificationPaginationTest extends W11HttpTestSupport {
    static final Instant WHEN=Instant.parse("2030-01-01T00:00:00Z");
    public static class SqlCapture implements StatementInspector {
        static final List<String> SQL=new CopyOnWriteArrayList<>();
        public String inspect(String sql){SQL.add(sql.toLowerCase(Locale.ROOT));return sql;}
    }
    @Autowired NotificationService notifications;
    User actor,other;String group,task;
    @BeforeEach void seed(){actor=user();other=user();group=work.createGroup(actor.id(),"Inbox pagination","").getId();invitations.join(other.id(),invitations.issue(actor.id(),group));task=task(group,actor.id());}
    String task(String g,String owner){return work.createTask(owner,g,"Inbox task","",owner,Instant.now().plusSeconds(86400)).getId();}
    String add(String recipient,String g,String t,Instant at,String kind){String id=UUID.randomUUID().toString();jdbc.update("INSERT INTO notification(id,event_key,recipient_id,group_id,task_id,kind,created_at) VALUES(?,?,?,?,?,?,?)",id,UUID.randomUUID().toString(),recipient,g,t,kind,Timestamp.from(at));return id;}
    List<String> add(int n){var ids=new ArrayList<String>();for(int i=0;i<n;i++)ids.add(add(actor.id(),group,task,WHEN,"COMMENT"));ids.sort(Comparator.reverseOrder());return ids;}
    List<String> ids(String html){var m=Pattern.compile("action=\"/notifications/([0-9a-f-]{36})/open\"").matcher(html);var ids=new ArrayList<String>();while(m.find())ids.add(m.group(1));return ids;}
    String next(String html){var m=Pattern.compile("href=\"(/notifications\\?before=[^\"]+)\"").matcher(html);return m.find()?HtmlUtils.htmlUnescape(m.group(1)):null;}
    String page(HttpClient c,String path)throws Exception{var r=get(c,path);assertEquals(200,r.statusCode());return r.body();}

    @Test void multiplePagesUseIdTieBreakerWithoutLossOrDuplicates()throws Exception {
        var expected=add(47);try(var c=login(actor)){
            String html=page(c,"/notifications");assertEquals(expected.subList(0,20),ids(html));
            var all=new ArrayList<>(ids(html));String link=next(html);assertNotNull(link);
            html=page(c,link);assertEquals(expected.subList(20,40),ids(html));all.addAll(ids(html));
            link=next(html);assertNotNull(link);html=page(c,link);all.addAll(ids(html));
            assertNull(next(html));assertEquals(expected,all);assertEquals(47,new HashSet<>(all).size());
        }
    }
    @Test void databaseRetrievalIsBoundedAndUnreadUsesCount()throws Exception {
        add(250);try(var c=login(actor)){
            SqlCapture.SQL.clear();var html=page(c,"/notifications");
            var selects=SqlCapture.SQL.stream().filter(s->s.contains("from notification ")).toList();
            assertEquals(2,selects.size(),"One bounded page SELECT and one COUNT, no full history or per-item lookup");
            assertTrue(selects.stream().anyMatch(s->s.contains("count(")&&s.contains("read_at is null")),selects.toString());
            assertTrue(selects.stream().filter(s->!s.contains("count(")).allMatch(s->s.contains("limit ?")||s.contains("fetch first")),selects.toString());
            assertEquals(20,ids(html).size());assertTrue(html.contains("250 chưa đọc"));
            assertEquals(20,notifications.inbox(actor.id()).size(),"Service itself must return a bounded page");
        }
    }
    @Test void newerInsertsBetweenPagesDoNotShiftExistingCursor()throws Exception {
        var expected=add(41);try(var c=login(actor)){
            String first=page(c,"/notifications"),link=next(first);assertNotNull(link);
            String newer=add(actor.id(),group,task,WHEN.plusSeconds(1),"COMMENT");
            String tied=add(actor.id(),group,task,WHEN,"COMMENT");
            jdbc.update("UPDATE notification SET id=? WHERE id=?","ffffffff-ffff-ffff-ffff-ffffffffffff",tied);
            var all=new ArrayList<>(ids(first));String html=page(c,link);all.addAll(ids(html));
            html=page(c,next(html));all.addAll(ids(html));assertEquals(expected,all);
            var refreshed=ids(page(c,"/notifications"));assertTrue(refreshed.contains(newer));assertTrue(refreshed.contains("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        }finally{jdbc.update("DELETE FROM notification WHERE id=?","ffffffff-ffff-ffff-ffff-ffffffffffff");}
    }
    @Test void recipientCannotReadOrOpenAnotherUsersNotification()throws Exception {
        String own=add(actor.id(),group,task,WHEN,"COMMENT"),foreign=add(other.id(),group,task,WHEN,"COMMENT");
        try(var c=login(actor)){
            assertEquals(List.of(own),ids(page(c,"/notifications")));
            assertEquals(404,get(c,"/notifications/"+foreign).statusCode());
            assertEquals(404,post(c,"/notifications/"+foreign+"/read"));assertEquals(404,post(c,"/notifications/"+foreign+"/open"));
            assertNull(jdbc.queryForObject("SELECT read_at FROM notification WHERE id=?",Timestamp.class,foreign));
        }
    }
    @Test void staleRemindersAreFilteredBeforeLimitAndUnreadCount()throws Exception {
        var expected=add(23);var stale=new ArrayList<String>();
        for(String change:List.of("due","version","assignee","done","group")){
            String t=task(group,actor.id()),id=add(actor.id(),group,t,WHEN.plusSeconds(1),"REMINDER");
            jdbc.update("UPDATE notification n JOIN task t ON n.task_id=t.id SET n.reminder_due_at=t.due_at,n.reminder_assignee_id=t.assignee_id,n.reminder_task_version=t.version WHERE n.id=?",id);
            switch(change){
                case "due"->jdbc.update("UPDATE task SET due_at=DATE_ADD(due_at,INTERVAL 1 MINUTE) WHERE id=?",t);
                case "version"->jdbc.update("UPDATE task SET version=version+1 WHERE id=?",t);
                case "assignee"->jdbc.update("UPDATE task SET assignee_id=? WHERE id=?",other.id(),t);
                case "done"->jdbc.update("UPDATE task SET status='DONE',completed_at=UTC_TIMESTAMP(6) WHERE id=?",t);
                default->{String g=work.createGroup(actor.id(),"Other reminder group","").getId();jdbc.update("UPDATE notification SET group_id=? WHERE id=?",g,id);}
            }stale.add(id);
        }
        String fresh=add(actor.id(),group,task,WHEN.minusSeconds(1),"REMINDER");
        jdbc.update("UPDATE notification n JOIN task t ON n.task_id=t.id SET n.reminder_due_at=t.due_at,n.reminder_assignee_id=t.assignee_id,n.reminder_task_version=t.version WHERE n.id=?",fresh);
        try(var c=login(actor)){
            String html=page(c,"/notifications");assertEquals(expected.subList(0,20),ids(html));assertTrue(html.contains("24 chưa đọc"));
            var second=ids(page(c,next(html)));assertEquals(4,second.size());assertTrue(second.contains(fresh));
            for(String id:stale){assertEquals(404,get(c,"/notifications/"+id).statusCode());assertEquals(404,post(c,"/notifications/"+id+"/read"));}
            assertEquals(302,post(c,"/notifications/"+fresh+"/read"));assertTrue(page(c,"/notifications").contains("23 chưa đọc"));
        }
    }
    @Test void revokedMembershipHidesHistoryAndRejectsActions()throws Exception {
        String g=work.createGroup(other.id(),"Revocation fixture","").getId();invitations.join(actor.id(),invitations.issue(other.id(),g));
        String t=task(g,other.id()),id=add(actor.id(),g,t,WHEN.plusSeconds(1),"COMMENT");var expected=add(21);
        var member=jdbc.queryForMap("SELECT * FROM group_member WHERE group_id=? AND user_id=?",g,actor.id());
        // No membership-removal feature exists yet. Emulate operational revocation ONLY in disposable DB.
        jdbc.execute((ConnectionCallback<Void>)connection->{try(var s=connection.createStatement()){s.execute("SET FOREIGN_KEY_CHECKS=0");try(var p=connection.prepareStatement("DELETE FROM group_member WHERE id=?")){p.setString(1,(String)member.get("id"));p.executeUpdate();}finally{s.execute("SET FOREIGN_KEY_CHECKS=1");}}return null;});
        try(var c=login(actor)){
            String html=page(c,"/notifications");assertEquals(expected.subList(0,20),ids(html));assertTrue(html.contains("21 chưa đọc"));
            assertEquals(404,get(c,"/notifications/"+id).statusCode());assertEquals(404,post(c,"/notifications/"+id+"/open"));
            assertNull(jdbc.queryForObject("SELECT read_at FROM notification WHERE id=?",Timestamp.class,id));
        }finally{jdbc.update("INSERT INTO group_member(id,group_id,user_id,role,joined_at) VALUES(?,?,?,?,?)",member.get("id"),g,actor.id(),member.get("role"),member.get("joined_at"));}
    }
    @Test void malformedCursorIsRejectedAndCursorNeverSelectsRecipient()throws Exception {
        add(2);try(var c=login(actor)){
            for(String cursor:List.of("garbage","a".repeat(513),Base64.getUrlEncoder().withoutPadding().encodeToString((WHEN+"|wrong-id").getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                assertEquals(400,get(c,"/notifications?before="+cursor).statusCode());
            String foreign=add(other.id(),group,task,WHEN.plusSeconds(1),"COMMENT");
            String cursor=Base64.getUrlEncoder().withoutPadding().encodeToString((WHEN.plusSeconds(1)+"|"+foreign).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(2,ids(page(c,"/notifications?before="+cursor)).size());assertFalse(ids(page(c,"/notifications?before="+cursor)).contains(foreign));
        }
    }
}
