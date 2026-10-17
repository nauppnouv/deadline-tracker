package vn.deadline.tracker;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import jakarta.servlet.http.Cookie;
import vn.deadline.tracker.service.*;
import java.time.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class Round2MatrixTest extends DatabaseTest {
    @Autowired MockMvc mvc;@Autowired AccountService accounts;@Autowired WorkService work;@Autowired InvitationService invites;
    record Person(String id,Cookie cookie){}
    Person user()throws Exception {
        String name="m"+UUID.randomUUID().toString().replace("-","").substring(0,20),pass="Round two matrix passphrase 123!";
        String id=accounts.register(name,"Matrix",pass);
        Cookie cookie=mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",name).param("password",pass)).andReturn().getResponse().getCookie("SESSION");assertNotNull(cookie);return new Person(id,cookie);
    }
    @Test @DisplayName("UAT-10 SEC-02 SEC-03 R2 two groups by four actors across every group/task route")
    void routeMatrix()throws Exception {
        Person a=user(),member=user(),b=user();String group=work.createGroup(a.id(),"Matrix A","").getId(),other=work.createGroup(b.id(),"Matrix B","").getId();
        invites.join(member.id(),invites.issue(a.id(),group));
        var task=work.createTask(a.id(),group,"Matrix task","",member.id(),Instant.now().plusSeconds(7200));
        String inviteToken=invites.issue(a.id(),group),inviteId;
        // Repository lookup uses the hashed token; the HTTP route takes invitation ID.
        var contextJdbc=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()));
        inviteId=contextJdbc.queryForObject("SELECT id FROM invitation WHERE token_hash=?",String.class,InvitationService.hash(inviteToken));
        String base="/groups/"+group,url=base+"/tasks/"+task.getId();
        int checks=0;
        for(int role=0;role<4;role++){
            Person person=role==0?a:role==1?member:role==2?b:null;
            for(String route:List.of(base,base+"/settings",base+"/tasks/new",url,url+"/edit")){
                var request=get(route);if(person!=null)request.cookie(person.cookie());
                int expected=role>=2?404:role==1&&(route.endsWith("/settings")||route.endsWith("/edit"))?403:200;
                int actual=mvc.perform(request).andReturn().getResponse().getStatus();assertEquals(expected,actual,"role="+role+" GET "+route);checks++;
            }
            for(String route:List.of(base+"/settings",base+"/tasks",url+"/edit",url+"/status",base+"/invitations",base+"/invitations/"+inviteId+"/revoke")){
                MockHttpServletRequestBuilder request=post(route).with(csrf()).param("actorId",a.id()).param("role","OWNER").param("createdBy",b.id()).param("groupId",other);
                if(person!=null)request.cookie(person.cookie());
                request.param("name","Matrix A").param("description","");
                // A rejected actor submits a known valid version from an authorized
                // form, so 404/403 proves authorization rather than invalid binding.
                String page=null;
                Person reader=a;
                if(route.endsWith("/settings"))page=base+"/settings";
                else if(route.endsWith("/edit"))page=url+"/edit";
                else if(route.endsWith("/status")){page=url;reader=member;}
                else if(route.equals(base+"/tasks")){page=base+"/tasks/new";if(role<=1)reader=person;}
                if(page!=null){
                    var form=mvc.perform(get(page).cookie(reader.cookie())).andReturn().getResponse();assertEquals(200,form.getStatus());
                    request.param("version",HtmlForms.version(form.getContentAsString(),route));
                }
                request.param("title","Matrix task").param("assigneeId",member.id()).param("status","IN_PROGRESS").param("dueAt",java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(task.getDueAt()));
                int expected=role>=2?404:role==0&&route.endsWith("/status")?403:role==1&&!route.equals(base+"/tasks")&&!route.endsWith("/status")?403:302;
                int actual=mvc.perform(request).andReturn().getResponse().getStatus();assertEquals(expected,actual,"role="+role+" POST "+route);checks++;
            }
        }
        assertEquals(404,mvc.perform(get("/groups/"+other+"/tasks/"+task.getId()).cookie(b.cookie())).andReturn().getResponse().getStatus());checks++;
        assertEquals(45,checks,"Keep all 45 original HTTP matrix assertions");
        assertEquals(a.id(),work.group(a.id(),group).getOwnerId());
        System.out.println("R2 SEC-03 matrix PASS: "+checks+" HTTP permission assertions across four actors and two groups");
    }
}
