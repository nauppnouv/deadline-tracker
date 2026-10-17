package vn.deadline.tracker;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import vn.deadline.tracker.service.*;
import vn.deadline.tracker.domain.*;
import vn.deadline.tracker.persistence.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @Import(AuthWebTest.Probes.class)
class AuthWebTest extends DatabaseTest {
    @Autowired MockMvc mvc; @Autowired AccountService accounts; @Autowired AccountRepository rows; @Autowired WorkService work;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mappings;
    static final String PASSWORD="A long test passphrase 123!";
    String name(){return "u"+UUID.randomUUID().toString().replace("-","").substring(0,20);}
    Cookie login(String username,String password) throws Exception {
        var result=mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",username).param("password",password)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/groups")).andReturn();
        Cookie cookie=result.getResponse().getCookie("SESSION");assertNotNull(cookie,"JDBC session cookie required");return cookie;
    }
    @Test @DisplayName("US-09 SEC-05/14 BR-23/24 normalized usernames, hash, Unicode and password bounds")
    void registrationRules() {
        String username=name();var id=accounts.register(" "+username.toUpperCase(Locale.ROOT)+" "," Same name ",PASSWORD);
        var saved=rows.findById(id).orElseThrow();assertEquals(username,saved.username);assertNotEquals(PASSWORD,saved.passwordHash);assertTrue(saved.passwordHash.contains("argon2"));
        assertThrows(RuleViolation.class,()->accounts.register(username,"X",PASSWORD));
        for(String invalid:Arrays.asList(null,"short","x".repeat(129))) assertThrows(RuleViolation.class,()->accounts.register(name(),"X",invalid));
        assertThrows(RuleViolation.class,()->accounts.register("a!","X",PASSWORD));
        assertThrows(RuleViolation.class,()->accounts.register(name()," ",PASSWORD));
        assertThrows(RuleViolation.class,()->accounts.register(name(),"x".repeat(81),PASSWORD));
        assertNotNull(accounts.register(name(),"Unicode","🔒".repeat(100)));
    }
    @Test @DisplayName("UAT-01/13 SEC-06 actual JDBC cookie logout invalidation and password change")
    void sessionLifecycle() throws Exception {
        String username=name();String id=accounts.register(username,"Member",PASSWORD);
        Cookie a=login(username,PASSWORD),b=login(username,PASSWORD);
        mvc.perform(get("/account").cookie(a)).andExpect(status().isOk());
        mvc.perform(post("/logout").cookie(a).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/account").cookie(a)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/account").cookie(b)).andExpect(status().is3xxRedirection());
        b=login(username,PASSWORD);
        accounts.changePassword(id,PASSWORD,"Another long passphrase 456!");
        mvc.perform(get("/account").cookie(b)).andExpect(status().is3xxRedirection());
        login(username,"Another long passphrase 456!");
        assertThrows(RuleViolation.class,()->accounts.changePassword(id,PASSWORD,"Another long passphrase 789!"));
    }
    @Test @DisplayName("SEC-03 UAT-10 group object access ignores forged identity and isolates owners")
    void accessMatrix() throws Exception {
        String userA=name(),userB=name();String a=accounts.register(userA,"Owner A",PASSWORD),b=accounts.register(userB,"Owner B",PASSWORD);
        String ga=work.createGroup(a,"A","").getId(); work.createGroup(b,"B","");
        var task=work.createTask(a,ga,"Private A","",a,Instant.now().plusSeconds(3600));
        String url="/groups/"+ga+"/probe/"+task.getId();
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).cookie(login(userB,PASSWORD)).param("actorId",a).param("role","OWNER")).andExpect(status().isNotFound());
        mvc.perform(get(url).cookie(login(userA,PASSWORD))).andExpect(status().isOk()).andExpect(content().string("Private A"));
        String userC=name(),c=accounts.register(userC,"Member A",PASSWORD);
        jdbc.update("INSERT INTO group_member(id,group_id,user_id,role,joined_at) VALUES(?,?,?,'MEMBER',NOW())",UUID.randomUUID().toString(),ga,c);
        mvc.perform(get(url).cookie(login(userC,PASSWORD))).andExpect(status().isOk()).andExpect(content().string("Private A"));
    }
    @Test @DisplayName("UAT-15 SEC-04 CSRF required for login/register/logout and protected writes")
    void csrfProtection() throws Exception {
        mvc.perform(post("/register").param("username",name()).param("displayName","X").param("password",PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(post("/login").param("username","x").param("password",PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf().useInvalidToken()).param("username",name())).andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",name()).param("displayName","X").param("password",PASSWORD).param("role","OWNER").param("actorId","forged"))
            .andExpect(status().is3xxRedirection());
    }
    @Test @DisplayName("UAT-17 SEC-06 IP and canonical username rate limits return generic errors")
    void rateLimit() throws Exception {
        String username=name();
        for(int i=0;i<5;i++) mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",username).param("password","bad"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf()).param("username",username.toUpperCase(Locale.ROOT)).param("password","bad")).andExpect(status().isTooManyRequests());
    }
    @Test @DisplayName("SEC-01/09/14 protected routes, headers and maximum form size")
    void defaultsAndHeaders() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(header().exists("Content-Security-Policy")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get("/account")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/register").with(csrf()).content("x".repeat(17000))).andExpect(status().isPayloadTooLarge());
    }
    @TestConfiguration static class Probes { @Bean Probe probe(WorkService work){return new Probe(work);} }
    @RestController static class Probe {
        private final WorkService work;Probe(WorkService work){this.work=work;}
        @GetMapping("/groups/{group}/probe/{task}") String task(@PathVariable String group,@PathVariable String task,java.security.Principal principal){
            return work.task(((org.springframework.security.core.Authentication)principal).getPrincipal() instanceof vn.deadline.tracker.security.TrackerPrincipal p?p.id():null,group,task).getTitle();
        }
    }
    @Test @DisplayName("SEC-01 enumerate all current MVC mappings and deny anonymous protected requests")
    void mappingInventory() throws Exception {
        for(var mapping:mappings.getHandlerMethods().keySet()) for(String pattern:mapping.getPatternValues()) {
            if(Set.of("/login","/register","/error").contains(pattern))continue;
            String path=pattern.replaceAll("\\{[^}]+}","not-a-member");
            if(mapping.getMethodsCondition().getMethods().contains(org.springframework.web.bind.annotation.RequestMethod.POST))
                mvc.perform(post(path).with(csrf())).andExpect(path.startsWith("/groups/")?status().isNotFound():status().is3xxRedirection());
            else mvc.perform(get(path)).andExpect(path.startsWith("/groups/")?status().isNotFound():status().is3xxRedirection());
        }
    }
    @Test @DisplayName("SEC-06 login rotates session, cookies protected, expiration rejects old session")
    void fixationAndExpiry() throws Exception {
        String username=name();accounts.register(username,"Member",PASSWORD);
        Cookie anonymous=mvc.perform(get("/login")).andReturn().getResponse().getCookie("SESSION");assertNotNull(anonymous);
        var authenticated=mvc.perform(post("/login").cookie(anonymous).with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",username).param("password",PASSWORD)).andExpect(redirectedUrl("/groups")).andReturn();
        Cookie session=authenticated.getResponse().getCookie("SESSION");assertNotNull(session);assertNotEquals(anonymous.getValue(),session.getValue());
        assertTrue(authenticated.getResponse().getHeaders("Set-Cookie").stream().anyMatch(h->h.contains("HttpOnly")&&h.contains("SameSite=Lax")));
        mvc.perform(get("/account").cookie(anonymous)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/account").cookie(session)).andExpect(status().isOk());
        String sessionId=new String(Base64.getDecoder().decode(session.getValue()),java.nio.charset.StandardCharsets.UTF_8);
        jdbc.update("UPDATE SPRING_SESSION SET LAST_ACCESS_TIME=0, EXPIRY_TIME=0 WHERE SESSION_ID=?",sessionId);
        mvc.perform(get("/account").cookie(session)).andExpect(status().is3xxRedirection());
    }
}
