package vn.deadline.tracker;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;import vn.deadline.tracker.service.*;import vn.deadline.tracker.persistence.*;
import java.util.*;import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class GroupInvitationWebTest extends DatabaseTest {
    @Autowired MockMvc mvc;@Autowired AccountService accounts;@Autowired WorkService work;
    @Autowired InvitationService invites;@Autowired MemberRepository members;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    static final String PASSWORD="Local fixture long passphrase 123!";
    String username(){return "u"+UUID.randomUUID().toString().replace("-","").substring(0,20);}
    Cookie login(String username,String next)throws Exception{
        var result=mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",username).param("password",PASSWORD).param("next",next==null?"":next)).andExpect(status().is3xxRedirection()).andReturn();
        assertEquals(next!=null&&next.startsWith("/invitations/")?next:"/groups",result.getResponse().getRedirectedUrl());return result.getResponse().getCookie("SESSION");
    }
    @Test @DisplayName("US-12 UAT-02 create via web ignores forged owner, four members join by POST")
    void completeGroupFlow()throws Exception{
        String ownerName=username(),owner=accounts.register(ownerName,"Owner",PASSWORD);Cookie cookie=login(ownerName,null);
        var created=mvc.perform(post("/groups").cookie(cookie).with(csrf()).param("name","Project A").param("description","").param("ownerId","forged").param("role","MEMBER")).andExpect(status().is3xxRedirection()).andReturn();
        String group=created.getResponse().getRedirectedUrl().substring("/groups/".length());assertTrue(work.group(owner,group).isOwner(owner));
        for(int i=0;i<4;i++){
            String name=username();accounts.register(name,"Member "+i,PASSWORD);
            var issued=mvc.perform(post("/groups/"+group+"/invitations").cookie(cookie).with(csrf())).andExpect(status().is3xxRedirection()).andReturn();
            String path=issued.getResponse().getRedirectedUrl();assertTrue(path.startsWith("/invitations/"));assertTrue(issued.getFlashMap().isEmpty());
            Cookie member=login(name,path);
            mvc.perform(get(path).cookie(member)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Project A")));
            mvc.perform(get(path).cookie(member)).andExpect(status().isOk()).andExpect(header().string("Referrer-Policy","no-referrer"));
            mvc.perform(post(path).cookie(member).with(csrf())).andExpect(redirectedUrl("/groups/"+group));
            mvc.perform(post("/groups/"+group+"/invitations").cookie(member).with(csrf())).andExpect(status().isForbidden());
        }
        assertEquals(5,members.findByGroupIdOrderByJoinedAtAsc(group).size());
        mvc.perform(get("/groups/"+group).cookie(cookie)).andExpect(status().isOk());
    }
    @Test @DisplayName("SEC-07 token not in DB sessions; anonymous invite login return path and no open redirect")
    void secretHandling()throws Exception{
        String name=username(),id=accounts.register(name,"Owner",PASSWORD);Cookie cookie=login(name,null);String group=work.createGroup(id,"Invite","").getId();
        String token=invites.issue(id,group),path="/invitations/"+token;
        mvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(vn.deadline.tracker.security.ReturnPath.login(path,"continue")));
        mvc.perform(get(path).cookie(cookie)).andExpect(status().isOk()).andExpect(header().string("Referrer-Policy","no-referrer"));
        for(byte[] data:jdbc.query("SELECT ATTRIBUTE_BYTES FROM SPRING_SESSION_ATTRIBUTES",(rs,row)->rs.getBytes(1)))assertFalse(new String(data,StandardCharsets.ISO_8859_1).contains(token));
        mvc.perform(post(path).cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",name).param("password",PASSWORD).param("next","https://example.com"))
            .andExpect(redirectedUrl("/groups"));
    }
}
