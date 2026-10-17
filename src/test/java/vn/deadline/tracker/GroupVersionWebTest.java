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
        String a=version(first),b=version(second);
        assertEquals(302,mvc.perform(post(path).cookie(first).with(csrf()).param("name","Saved new name").param("description","Original description").param("version",a)).andReturn().getResponse().getStatus());
        var stale=mvc.perform(post(path).cookie(second).with(csrf()).param("name","Original name").param("description","Keep this unsaved draft").param("version",b)).andReturn().getResponse();
        assertAll(()->assertEquals(409,stale.getStatus()),()->assertTrue(stale.getContentAsString().contains("Keep this unsaved draft")),
            ()->assertEquals("Saved new name",work.group(actor,path.split("/")[2]).getName()));
        String conflictVersion=HtmlForms.version(stale.getContentAsString(),path);assertEquals(b,conflictVersion);
        assertEquals(409,mvc.perform(post(path).cookie(second).with(csrf()).param("name","Retry stale draft").param("description","").param("version",conflictVersion)).andReturn().getResponse().getStatus());
        String reloaded=version(second);assertNotEquals(b,reloaded);
        assertEquals(302,mvc.perform(post(path).cookie(second).with(csrf()).param("name","Saved new name").param("description","Draft after reload").param("version",reloaded)).andReturn().getResponse().getStatus());
    }
    String version(Cookie cookie)throws Exception {
        var response=mvc.perform(get(path).cookie(cookie)).andReturn().getResponse();assertEquals(200,response.getStatus());
        return HtmlForms.version(response.getContentAsString(),path);
    }
    @Test @DisplayName("UAT-11 DATA-04 R2-02 group validation returns a usable rendered version")
    void validationVersion()throws Exception {
        String opened=version(first);
        var invalid=mvc.perform(post(path).cookie(first).with(csrf()).param("name"," ").param("description","Keep validation draft").param("version",opened)).andReturn().getResponse();
        assertEquals(400,invalid.getStatus());
        String returned=HtmlForms.version(invalid.getContentAsString(),path);assertEquals(opened,returned);
        assertEquals(302,mvc.perform(post(path).cookie(first).with(csrf()).param("name","Corrected").param("description","Keep validation draft").param("version",returned)).andReturn().getResponse().getStatus());
    }
    @Test @DisplayName("P7-03 DATA-04 group update requires nonnegative form version")
    void versionRequired()throws Exception {
        version(first); // Verify the real form before deliberately corrupting its version.
        for(String version:new String[]{null,"-1","not-a-version"}) {
            var request=post(path).cookie(first).with(csrf()).param("name","Should not save").param("description","");
            if(version!=null)request.param("version",version);
            assertEquals(400,mvc.perform(request).andReturn().getResponse().getStatus());
        }
        assertEquals("Original name",work.group(actor,path.split("/")[2]).getName());
    }
}
