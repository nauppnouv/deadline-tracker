package vn.deadline.tracker;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.builder.SpringApplicationBuilder;import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.servlet.MockMvc;import jakarta.servlet.http.Cookie;
import vn.deadline.tracker.service.*;import vn.deadline.tracker.persistence.*;import vn.deadline.tracker.ops.*;
import java.time.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class OpsWorkflowTest extends DatabaseTest {
    @Autowired AccountService accounts;@Autowired AccountRepository users;@Autowired TaskRepository tasks;@Autowired WorkService work;@Autowired MockMvc mvc;
    @Autowired InvitationService invites;
    static final String PASSWORD="Operator workflow test passphrase 123!";
    String name(){return "u"+UUID.randomUUID().toString().replace("-","").substring(0,20);}
    Cookie login(String name,String password)throws Exception{return mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",name).param("password",password)).andExpect(redirectedUrl("/groups")).andReturn().getResponse().getCookie("SESSION");}
    ConfigurableApplicationContext ops(){return new SpringApplicationBuilder(DeadlineTrackerApplication.class).profiles("ops").web(WebApplicationType.NONE).initializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("ops-test",Map.of("spring.datasource.url",DB.getJdbcUrl(),"spring.datasource.username",DB.getUsername(),"spring.datasource.password",DB.getPassword(),"app.ops.execute","false","logging.level.root","ERROR")))).run();}
    static class Console implements OpsConsole {String confirmation,secret;boolean available=true;Console(String confirmation){this.confirmation=confirmation;}public boolean available(){return available;}public String readLine(String prompt){return confirmation;}public void secret(String value){secret=value;}public void message(String value){}}
    @Test @DisplayName("DATA-09 UAT-13 ops reset runner in non-web context forces password change and invalidates sessions")
    void reset()throws Exception{
        String username=name(),id=accounts.register(username,"Member",PASSWORD);Cookie old=login(username,PASSWORD);String temporary;
        try(var ctx=ops()){
            assertFalse(ctx instanceof org.springframework.web.context.WebApplicationContext);assertTrue(ctx.getBeansOfType(org.springframework.security.web.SecurityFilterChain.class).isEmpty());
            var console=new Console(username);new OpsCommand(ctx.getBean(OpsActions.class),console).run("ops","reset-password",username);temporary=console.secret;assertNotNull(temporary);
        }
        assertTrue(users.findById(id).orElseThrow().mustChangePassword);
        mvc.perform(get("/account").cookie(old)).andExpect(status().is3xxRedirection());Cookie reset=login(username,temporary);
        mvc.perform(get("/groups").cookie(reset)).andExpect(redirectedUrl("/password"));
        mvc.perform(post("/password").cookie(reset).with(csrf()).param("currentPassword",temporary).param("newPassword","New user passphrase after reset 456!").param("confirmPassword","New user passphrase after reset 456!"))
            .andExpect(redirectedUrl("/login?changed"));
        assertFalse(users.findById(id).orElseThrow().mustChangePassword);login(username,"New user passphrase after reset 456!");
    }
    @Test @DisplayName("DATA-09 anonymize runner keeps task references, disables auth, requires exact confirmation")
    void anonymize()throws Exception{
        String username=name(),id=accounts.register(username,"Original name",PASSWORD);String group=work.createGroup(id,"Preserved","").getId();var task=work.createTask(id,group,"Preserved task","",id,Instant.now().plusSeconds(3600));Cookie cookie=login(username,PASSWORD);
        try(var ctx=ops()){
            var actions=ctx.getBean(OpsActions.class);
            assertThrows(IllegalArgumentException.class,()->new OpsCommand(actions,new Console("wrong")).run("ops","anonymize-user",username));assertTrue(users.findById(id).orElseThrow().enabled);
            var absent=new Console(username);absent.available=false;assertThrows(IllegalStateException.class,()->new OpsCommand(actions,absent).run("ops","reset-password",username));
            new OpsCommand(actions,new Console(username)).run("ops","anonymize-user",username);
        }
        var deleted=users.findById(id).orElseThrow();assertFalse(deleted.enabled);assertEquals("deleted-"+id,deleted.username);assertEquals("Đã xóa",deleted.displayName);
        assertEquals(id,tasks.findById(task.getId()).orElseThrow().assigneeId);
        mvc.perform(get("/account").cookie(cookie)).andExpect(status().is3xxRedirection());
    }
    @Test @DisplayName("DATA-09 SEC-11 operations never exposed by web and credential unchanged")
    void noWebOps()throws Exception{
        String username=name(),id=accounts.register(username,"Member",PASSWORD);String before=users.findById(id).orElseThrow().passwordHash;Cookie cookie=login(username,PASSWORD);
        mvc.perform(post("/ops/reset-password").cookie(cookie).with(csrf()).param("username",username)).andExpect(status().is4xxClientError());
        mvc.perform(get("/ops/anonymize-user").cookie(cookie)).andExpect(status().isNotFound());assertEquals(before,users.findById(id).orElseThrow().passwordHash);
    }
    @Test @DisplayName("D-21 disabled member retains historical task but cannot receive new assignments")
    void disabledAssignee(){
        String owner=accounts.register(name(),"Owner",PASSWORD),disabledName=name(),disabled=accounts.register(disabledName,"Disabled",PASSWORD);
        String group=work.createGroup(owner,"Inactive member fixture","").getId();invites.join(disabled,invites.issue(owner,group));
        var existing=work.createTask(owner,group,"Historical assignment","",disabled,Instant.now().plusSeconds(3600));
        var other=work.createTask(owner,group,"Active assignment","",owner,Instant.now().plusSeconds(3600));
        try(var ctx=ops()){ctx.getBean(OpsActions.class).anonymize(disabledName);}
        assertThrows(vn.deadline.tracker.domain.RuleViolation.class,()->work.createTask(owner,group,"Rejected","",disabled,Instant.now().plusSeconds(3600)));
        assertThrows(vn.deadline.tracker.domain.RuleViolation.class,()->work.editTask(owner,group,other.getId(),other.getTitle(),"",disabled,other.getDueAt(),other.getVersion()));
        assertEquals(disabled,work.editTask(owner,group,existing.getId(),"Historical renamed","",disabled,existing.getDueAt(),existing.getVersion()).getAssigneeId());
    }
}
