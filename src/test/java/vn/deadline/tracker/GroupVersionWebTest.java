package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import vn.deadline.tracker.service.*;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc
class GroupVersionWebTest extends DatabaseTest {
    @Autowired MockMvc mvc; @Autowired AccountService accounts; @Autowired WorkService work;
    String actor,path; Cookie first,second;
    @BeforeEach void seed()throws Exception {
        String name="g"+UUID.randomUUID().toString().replace("-","").substring(0,20),password="Group version fixture passphrase 123!";
        actor=accounts.register(name,"Owner",password);path="/groups/"+work.createGroup(actor,"Original name","Original description").getId()+"/settings";
        first=mvc.perform(post("/login").with(csrf()).param("username",name).param("password",password)).andReturn().getResponse().getCookie("SESSION");
        second=mvc.perform(post("/login").with(csrf()).param("username",name).param("password",password)).andReturn().getResponse().getCookie("SESSION");
        assertNotNull(first);assertNotNull(second);
    }
    @Test @DisplayName("P7-03 DATA-04 two old group forms cannot silently overwrite, conflict preserves draft")
    void staleGroupForm()throws Exception {
        assertEquals(200,mvc.perform(get(path).cookie(first)).andReturn().getResponse().getStatus());
        assertEquals(200,mvc.perform(get(path).cookie(second)).andReturn().getResponse().getStatus());
        assertEquals(302,mvc.perform(post(path).cookie(first).with(csrf()).param("name","Saved new name").param("description","Original description").param("version","0")).andReturn().getResponse().getStatus());
        var stale=mvc.perform(post(path).cookie(second).with(csrf()).param("name","Original name").param("description","Keep this unsaved draft").param("version","0")).andReturn().getResponse();
        assertAll(()->assertEquals(409,stale.getStatus()),()->assertTrue(stale.getContentAsString().contains("Keep this unsaved draft")),
            ()->assertEquals("Saved new name",work.group(actor,path.split("/")[2]).getName()));
        assertEquals(302,mvc.perform(post(path).cookie(second).with(csrf()).param("name","Saved new name").param("description","Draft after reload").param("version","1")).andReturn().getResponse().getStatus());
    }
    @Test @DisplayName("P7-03 DATA-04 group update requires nonnegative form version")
    void versionRequired()throws Exception {
        for(String version:new String[]{null,"-1","not-a-version"}) {
            var request=post(path).cookie(first).with(csrf()).param("name","Should not save").param("description","");
            if(version!=null)request.param("version",version);
            assertEquals(400,mvc.perform(request).andReturn().getResponse().getStatus());
        }
        assertEquals("Original name",work.group(actor,path.split("/")[2]).getName());
    }
}
