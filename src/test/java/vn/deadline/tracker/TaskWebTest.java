package vn.deadline.tracker;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;import vn.deadline.tracker.service.*;import vn.deadline.tracker.domain.*;import vn.deadline.tracker.persistence.*;
import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class TaskWebTest extends DatabaseTest {
    record User(String id,String name,Cookie cookie){}
    @Autowired MockMvc mvc;@Autowired AccountService accounts;@Autowired WorkService work;@Autowired InvitationService invites;
    @Autowired TaskEventRepository events;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;@Autowired AttachmentService attachmentService;
    static final String PASS="A separate web test passphrase 123!";
    User owner,creator,worker,foreign;String group;
    User user()throws Exception{String n="u"+UUID.randomUUID().toString().replace("-","").substring(0,20);String id=accounts.register(n,"Person",PASS);
        var r=mvc.perform(post("/login").with(csrf()).with(req->{req.setRemoteAddr(UUID.randomUUID().toString());return req;}).param("username",n).param("password",PASS)).andExpect(redirectedUrl("/groups")).andReturn();return new User(id,n,r.getResponse().getCookie("SESSION"));}
    @BeforeEach void seed()throws Exception{owner=user();creator=user();worker=user();foreign=user();group=work.createGroup(owner.id(),"Web tasks","").getId();
        invites.join(creator.id(),invites.issue(owner.id(),group));invites.join(worker.id(),invites.issue(owner.id(),group));work.createGroup(foreign.id(),"Other group","");}
    String date(Instant i){return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(i);}
    Task task(){return work.createTask(creator.id(),group,"Original","",worker.id(),Instant.now().plusSeconds(7200));}
    String url(Task t){return "/groups/"+group+"/tasks/"+t.getId();}
    String formVersion(User actor,Task task,boolean edit)throws Exception {
        String page=url(task)+(edit?"/edit":"");
        var response=mvc.perform(get(page).cookie(actor.cookie())).andExpect(status().isOk()).andReturn().getResponse();
        return HtmlForms.version(response.getContentAsString(),url(task)+(edit?"/edit":"/status"));
    }
    @Test @DisplayName("US-16 UAT-04 create form validates fields, preserves values, ignores forged creator/status")
    void createAndValidation()throws Exception{
        var opened=mvc.perform(get("/groups/"+group+"/tasks/new").cookie(creator.cookie())).andExpect(status().isOk()).andReturn().getResponse();
        String createVersion=HtmlForms.version(opened.getContentAsString(),"/groups/"+group+"/tasks");
        var invalid=mvc.perform(post("/groups/"+group+"/tasks").cookie(creator.cookie()).with(csrf()).param("title"," ").param("description","Keep my description").param("assigneeId",worker.id()).param("dueAt",date(Instant.now().plusSeconds(3600))).param("version",createVersion))
            .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.containsString("Keep my description"))).andReturn().getResponse();
        String retryVersion=HtmlForms.version(invalid.getContentAsString(),"/groups/"+group+"/tasks");
        assertEquals(0,work.tasks(owner.id(),group).size());
        var r=mvc.perform(post("/groups/"+group+"/tasks").cookie(creator.cookie()).with(csrf()).param("title","Created through web").param("description","").param("assigneeId",worker.id()).param("dueAt",date(Instant.now().plusSeconds(3600))).param("version",retryVersion).param("createdBy",foreign.id()).param("actorId",owner.id()).param("status","DONE").param("role","OWNER"))
            .andExpect(status().is3xxRedirection()).andReturn();
        String id=r.getResponse().getRedirectedUrl().substring(r.getResponse().getRedirectedUrl().lastIndexOf('/')+1);var t=work.task(owner.id(),group,id);
        assertEquals(creator.id(),t.getCreatedBy());assertEquals(TaskStatus.TODO,t.getStatus());
        mvc.perform(get(url(t)).cookie(worker.cookie())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Created through web")));
    }
    @Test @DisplayName("US-16 UAT-05/07 only assignee changes status; creator alone is not assignee")
    void statusPermission()throws Exception{
        var t=task();String statusVersion=formVersion(worker,t,false);
        for(User u:List.of(owner,creator))mvc.perform(post(url(t)+"/status").cookie(u.cookie()).with(csrf()).param("status","DONE").param("version",statusVersion)).andExpect(status().isForbidden());
        mvc.perform(get(url(t)+"/edit").cookie(worker.cookie())).andExpect(status().isForbidden());
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","DONE").param("version",statusVersion)).andExpect(redirectedUrl(url(t)));
        var completed=work.task(owner.id(),group,t.getId());assertNotNull(completed.getCompletedAt());long before=events.count();
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","DONE").param("version",formVersion(worker,t,false))).andExpect(redirectedUrl(url(t)));assertEquals(before,events.count());
    }
    @Test @DisplayName("US-16 UAT-06/11 reassign resets task and stale edit preserves submitted values")
    void editAndConflict()throws Exception{
        var t=task();String oldVersion=formVersion(creator,t,true),ownerVersion=formVersion(owner,t,true);
        mvc.perform(post(url(t)+"/edit").cookie(owner.cookie()).with(csrf()).param("title","Reassigned").param("description","").param("assigneeId",owner.id()).param("dueAt",date(t.getDueAt())).param("version",ownerVersion)).andExpect(redirectedUrl(url(t)));
        var updated=work.task(owner.id(),group,t.getId());assertEquals(owner.id(),updated.getAssigneeId());assertEquals(TaskStatus.TODO,updated.getStatus());
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","DONE").param("version",formVersion(owner,t,false))).andExpect(status().isForbidden());
        var conflict=mvc.perform(post(url(t)+"/edit").cookie(creator.cookie()).with(csrf()).param("title","My unsaved changes").param("description","Do not discard").param("assigneeId",worker.id()).param("dueAt",date(t.getDueAt())).param("version",oldVersion))
            .andExpect(status().isConflict()).andExpect(content().string(org.hamcrest.Matchers.containsString("My unsaved changes"))).andReturn().getResponse();
        String returned=HtmlForms.version(conflict.getContentAsString(),url(t)+"/edit");assertEquals(oldVersion,returned);
        mvc.perform(post(url(t)+"/edit").cookie(creator.cookie()).with(csrf()).param("title","Retry stale").param("description","").param("assigneeId",owner.id()).param("dueAt",date(t.getDueAt())).param("version",returned)).andExpect(status().isConflict());
        assertEquals("Reassigned",work.task(owner.id(),group,t.getId()).getTitle());
        String reloaded=formVersion(creator,t,true);assertNotEquals(oldVersion,reloaded);
        mvc.perform(post(url(t)+"/edit").cookie(creator.cookie()).with(csrf()).param("title","After reload").param("description","Draft kept").param("assigneeId",owner.id()).param("dueAt",date(t.getDueAt())).param("version",reloaded)).andExpect(redirectedUrl(url(t)));
    }
    @Test @DisplayName("US-17 UAT-08/09/12/18 filters keep group totals and overdue metadata remains editable")
    void filtersAndPastDue()throws Exception{
        var t=task();var done=task();work.changeStatus(worker.id(),group,done.getId(),TaskStatus.DONE,done.getVersion());
        Instant past=Instant.now().minusSeconds(3600);jdbc.update("UPDATE task SET due_at=? WHERE id=?",java.sql.Timestamp.from(past),t.getId());
        var result=mvc.perform(get("/groups/"+group).cookie(owner.cookie()).param("due","OVERDUE")).andExpect(status().isOk()).andExpect(model().attribute("tasks",org.hamcrest.Matchers.hasSize(1))).andReturn();
        assertEquals(2,((Progress)result.getModelAndView().getModel().get("progress")).total());
        var old=work.task(owner.id(),group,t.getId());
        mvc.perform(post(url(t)+"/edit").cookie(owner.cookie()).with(csrf()).param("title","Overdue renamed").param("description","").param("assigneeId",owner.id()).param("dueAt",date(old.getDueAt())).param("version",formVersion(owner,t,true))).andExpect(redirectedUrl(url(t)));
        assertEquals(old.getDueAt(),work.task(owner.id(),group,t.getId()).getDueAt());
    }
    @Test @DisplayName("UAT-04/11 DATA-04 R2-02 task edit validation returns version for corrected resubmission")
    void editValidationVersion()throws Exception {
        var t=task();String opened=formVersion(creator,t,true);
        var invalid=mvc.perform(post(url(t)+"/edit").cookie(creator.cookie()).with(csrf()).param("title"," ").param("description","Validation draft").param("assigneeId",worker.id()).param("dueAt",date(t.getDueAt())).param("version",opened)).andExpect(status().isBadRequest()).andReturn().getResponse();
        String returned=HtmlForms.version(invalid.getContentAsString(),url(t)+"/edit");assertEquals(opened,returned);
        mvc.perform(post(url(t)+"/edit").cookie(creator.cookie()).with(csrf()).param("title","Corrected title").param("description","Validation draft").param("assigneeId",worker.id()).param("dueAt",date(t.getDueAt())).param("version",returned)).andExpect(redirectedUrl(url(t)));
    }
    @Test @DisplayName("UAT-07/11 DATA-04 R2-02 status errors require reload and fresh HTML version")
    void statusValidationConflict()throws Exception {
        var t=task();String a=formVersion(worker,t,false),b=formVersion(worker,t,false);
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","INVALID").param("version",a)).andExpect(status().isBadRequest());
        String afterValidation=formVersion(worker,t,false);assertEquals(a,afterValidation);
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","DONE").param("version",afterValidation)).andExpect(redirectedUrl(url(t)));
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","TODO").param("version",b)).andExpect(status().isConflict());
        String afterConflict=formVersion(worker,t,false);assertNotEquals(b,afterConflict);
        mvc.perform(post(url(t)+"/status").cookie(worker.cookie()).with(csrf()).param("status","TODO").param("version",afterConflict)).andExpect(redirectedUrl(url(t)));
        assertNull(work.task(owner.id(),group,t.getId()).getCompletedAt());
    }
    @Test @DisplayName("SEC-03/02 UAT-10 every task endpoint rejects foreign owner and anonymous, including malformed input")
    void everyEndpointIsolation()throws Exception{
        var t=task();
        for(String path:List.of("/groups/"+group,"/groups/"+group+"/settings","/groups/"+group+"/tasks/new",url(t),url(t)+"/edit")){
            mvc.perform(get(path).cookie(foreign.cookie())).andExpect(status().isNotFound());mvc.perform(get(path)).andExpect(status().isNotFound());}
        for(String path:List.of("/groups/"+group+"/tasks",url(t)+"/edit",url(t)+"/status","/groups/"+group+"/settings","/groups/"+group+"/invitations"))
            mvc.perform(post(path).cookie(foreign.cookie()).with(csrf()).param("actorId",owner.id()).param("status","INVALID")).andExpect(status().isNotFound());
    }
    @Test @DisplayName("US-18 SEC-08/09 UAT-16 user-controlled HTML is escaped in group/task/detail/forms")
    void xssAndHeaders()throws Exception{
        String payload="<script>alert(1)</script>";work.renameGroup(owner.id(),group,payload,payload,work.group(owner.id(),group).getVersion());
        var t=work.createTask(creator.id(),group,payload,payload,worker.id(),Instant.now().plusSeconds(3600));
        for(String path:List.of("/groups","/groups/"+group,url(t),url(t)+"/edit")){
            var response=mvc.perform(get(path).cookie(owner.cookie())).andExpect(status().isOk()).andExpect(header().string("Referrer-Policy","no-referrer")).andReturn().getResponse();
            assertFalse(response.getContentAsString().contains(payload));assertTrue(response.getContentAsString().contains("&lt;script&gt;"));assertFalse(response.getHeader("Content-Security-Policy").contains("unsafe-inline"));
        }
    }
    @Test @DisplayName("W1.1 M1 comment revisions use membership, CSRF, owner hide and optimistic versions")
    void commentsAndTimeline()throws Exception {
        var t=task();String commentUrl=url(t)+"/comments";String payload="<script>alert(1)</script> xin chào";
        mvc.perform(post(commentUrl).cookie(creator.cookie()).param("body",payload)).andExpect(status().isForbidden());
        mvc.perform(post(commentUrl).cookie(creator.cookie()).with(csrf()).param("body",payload).param("actorId",owner.id()).param("authorId",owner.id()))
            .andExpect(redirectedUrl(url(t)));
        var initial=mvc.perform(get(url(t)).cookie(worker.cookie())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(initial.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));assertFalse(initial.contains(payload));
        var comment=commentsFor(t).getFirst();
        mvc.perform(post(commentUrl+"/"+comment.id+"/edit").cookie(worker.cookie()).with(csrf()).param("body","forbidden").param("version","0")).andExpect(status().isForbidden());
        mvc.perform(post(commentUrl+"/"+comment.id+"/edit").cookie(creator.cookie()).with(csrf()).param("body","updated").param("version","9")).andExpect(status().isConflict());
        mvc.perform(post(commentUrl+"/"+comment.id+"/edit").cookie(creator.cookie()).with(csrf()).param("body","updated").param("version","0")).andExpect(redirectedUrl(url(t)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM task_comment_revision WHERE comment_id=?",Integer.class,comment.id));
        mvc.perform(get(url(t)).cookie(owner.cookie())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("updated")));
        mvc.perform(post(commentUrl+"/"+comment.id+"/hide").cookie(owner.cookie()).with(csrf())).andExpect(redirectedUrl(url(t)));
        mvc.perform(get(url(t)).cookie(owner.cookie())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("updated"))));
        mvc.perform(get(url(t)).cookie(foreign.cookie())).andExpect(status().isNotFound());
        mvc.perform(post(commentUrl).cookie(foreign.cookie()).with(csrf()).param("body","nope")).andExpect(status().isNotFound());
    }
    @Test @DisplayName("W1.1 M2 mentions persist inbox recipients once and read ownership is protected")
    void mentionsAndInbox()throws Exception {
        var t=task();String postComment=url(t)+"/comments";
        mvc.perform(post(postComment).cookie(creator.cookie()).with(csrf()).param("body","Please review").param("mentionIds",owner.id()).param("mentionIds",owner.id()))
            .andExpect(redirectedUrl(url(t)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM comment_mention WHERE mentioned_user_id=?",Integer.class,owner.id()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_id=? AND comment_id IS NOT NULL",Integer.class,owner.id()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_id=? AND comment_id IS NOT NULL",Integer.class,worker.id()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_id=? AND comment_id IS NOT NULL",Integer.class,creator.id()));
        var inbox=mvc.perform(get("/notifications").cookie(owner.cookie())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn được nhắc trong bình luận"))).andReturn();
        var id=jdbc.queryForObject("SELECT id FROM notification WHERE recipient_id=? AND comment_id IS NOT NULL",String.class,owner.id());
        mvc.perform(post("/notifications/"+id+"/read").cookie(worker.cookie()).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/notifications/"+id+"/read").cookie(owner.cookie())).andExpect(status().isForbidden());
        mvc.perform(post("/notifications/"+id+"/open").cookie(owner.cookie()).with(csrf())).andExpect(redirectedUrl(url(t)+"#comment-"+jdbc.queryForObject("SELECT comment_id FROM notification WHERE id=?",String.class,id)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id=? AND read_at IS NOT NULL",Integer.class,id));
        mvc.perform(post(postComment).cookie(creator.cookie()).with(csrf()).param("body","bad mention").param("mentionIds",foreign.id())).andExpect(status().isBadRequest());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM task_comment WHERE task_id=?",Integer.class,t.getId()));
    }
    @Test @DisplayName("W1.1 M3 private file download checks membership, MIME signature, hide retention and pin rights")
    void privateAttachmentsAndPins()throws Exception{
        var t=task();String base=url(t)+"/attachments";byte[] bytes="synthetic attachment data\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file=new org.springframework.mock.web.MockMultipartFile("file","..\\private.txt","text/plain",bytes);
        attachmentService.upload(creator.id(),group,t.getId(),file);
        String id=jdbc.queryForObject("SELECT id FROM task_attachment WHERE task_id=?",String.class,t.getId());
        mvc.perform(get(base+"/"+id).cookie(worker.cookie())).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment"))).andExpect(content().bytes(bytes));
        mvc.perform(get(base+"/"+id).cookie(foreign.cookie())).andExpect(status().isNotFound());
        assertThrows(RuleViolation.class,()->attachmentService.upload(creator.id(),group,t.getId(),new org.springframework.mock.web.MockMultipartFile("file","wrong.pdf","application/pdf",bytes)));
        mvc.perform(post(url(t)+"/pins/attachment/"+id).cookie(worker.cookie()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(url(t)+"/pins/attachment/"+id).cookie(creator.cookie()).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(base+"/"+id+"/hide").cookie(worker.cookie()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(base+"/"+id+"/hide").cookie(creator.cookie()).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals("READY",jdbc.queryForObject("SELECT lifecycle FROM task_attachment WHERE id=?",String.class,id));
        assertEquals(bytes.length,jdbc.queryForObject("SELECT size_bytes FROM task_attachment WHERE id=?",Long.class,id));
        mvc.perform(get(base+"/"+id).cookie(worker.cookie())).andExpect(status().isNotFound());
    }
    private List<TaskCommentEntity> commentsFor(Task t){return jdbc.query("SELECT id,task_id,group_id,author_id,body,version,created_at,updated_at,hidden_at,hidden_by FROM task_comment WHERE task_id=?",(rs,n)->{
        var c=new TaskCommentEntity();c.id=rs.getString("id");c.taskId=rs.getString("task_id");c.groupId=rs.getString("group_id");c.authorId=rs.getString("author_id");c.body=rs.getString("body");c.version=rs.getLong("version");return c;
    },t.getId());}
    @Test void editedMentionsNotifyOnlyNewMemberAndPaginationStaysBounded()throws Exception {
        var t=task();String base=url(t)+"/comments";
        mvc.perform(post(base).cookie(creator.cookie()).with(csrf()).param("body","First mention").param("mentionIds",owner.id())).andExpect(status().is3xxRedirection());
        String id=commentsFor(t).getFirst().id;
        mvc.perform(post(base+"/"+id+"/edit").cookie(creator.cookie()).with(csrf()).param("body","New mention").param("version","0").param("mentionIds",owner.id(),worker.id())).andExpect(status().is3xxRedirection());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE comment_id=? AND recipient_id=?",Integer.class,id,owner.id()));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE comment_id=? AND recipient_id=?",Integer.class,id,worker.id()));
        mvc.perform(get(base+"/"+id+"/revisions").cookie(creator.cookie())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("First mention")));
        mvc.perform(get(base+"/"+id+"/revisions").cookie(worker.cookie())).andExpect(status().isForbidden());
        for(int i=0;i<22;i++)mvc.perform(post(base).cookie(creator.cookie()).with(csrf()).param("body","page-item-"+i)).andExpect(status().is3xxRedirection());
        var response=mvc.perform(get(url(t)).cookie(creator.cookie())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(20,java.util.regex.Pattern.compile("<li[^>]*id=\"comment-[0-9a-f-]{36}\"").matcher(response).results().count());
        mvc.perform(get(url(t)+"?page=1").cookie(worker.cookie())).andExpect(status().isOk());
    }
}
