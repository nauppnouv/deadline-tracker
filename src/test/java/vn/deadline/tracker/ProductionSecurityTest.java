package vn.deadline.tracker;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"DB_PASSWORD=App production test passphrase 123!","SERVER_ADDRESS=127.0.0.1","server.forward-headers-strategy=native"}) @AutoConfigureMockMvc @ActiveProfiles("prod")
@org.springframework.context.annotation.Import(ProductionSecurityTest.Probes.class)
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class ProductionSecurityTest extends DatabaseTest {
    @org.springframework.test.context.DynamicPropertySource
    static void prepareMigratedFixture(org.springframework.test.context.DynamicPropertyRegistry registry) {
        // Test setup uses the fixture owner; the prod context itself only validates.
        org.flywaydb.core.Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()).load().migrate();
    }
    @Autowired MockMvc mvc;
    @org.springframework.boot.test.context.TestConfiguration static class Probes {
        @org.springframework.context.annotation.Bean Probe probe(){return new Probe();}
    }
    @org.springframework.web.bind.annotation.RestController static class Probe {
        @org.springframework.web.bind.annotation.GetMapping("/test-system-error") String fail(){throw new IllegalStateException("synthetic-sensitive-exception-marker");}
    }
    @Test @DisplayName("SEC-05/11 system error returns reference and does not log sensitive exception message")
    void unknownError(org.springframework.boot.test.system.CapturedOutput output)throws Exception{
        var result=mvc.perform(get("/test-system-error").secure(true).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-actor"))).andExpect(status().isInternalServerError()).andReturn();
        String body=result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(body.contains("Mã tham chiếu"));assertFalse(body.contains("synthetic-sensitive-exception-marker"));assertFalse(body.contains("IllegalStateException"));
        assertTrue(output.getAll().contains("Request failed reference="));assertFalse(output.getAll().contains("synthetic-sensitive-exception-marker"));
    }
    @Test @DisplayName("SEC-06/09 prod cookie Secure and TLS HSTS, CSP and frame headers")
    void prodHeaders()throws Exception{
        var r=mvc.perform(get("/login").secure(true)).andExpect(status().isOk()).andExpect(header().string("Strict-Transport-Security",org.hamcrest.Matchers.containsString("max-age="))).andExpect(header().string("X-Frame-Options","DENY")).andExpect(header().string("X-Content-Type-Options","nosniff")).andReturn();
        assertTrue(r.getResponse().getHeaders("Set-Cookie").stream().anyMatch(h->h.contains("Secure")&&h.contains("HttpOnly")&&h.contains("SameSite=Lax")));
        assertFalse(r.getResponse().getHeader("Content-Security-Policy").contains("unsafe-inline"));
    }
    @Test @DisplayName("SEC-11 debug endpoints closed, safe error page does not expose exception")
    void debugClosed()throws Exception{
        for(String path:List.of("/actuator","/actuator/env","/h2-console","/swagger-ui/index.html"))mvc.perform(get(path).secure(true)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/error").secure(true)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("java.lang"))));
    }
    @Test @DisplayName("SEC-08/10/12/14 source policies: no raw template HTML, inline scripts or destructive business routes")
    void sourcePolicies()throws Exception{
        try(var paths=Files.walk(Path.of("src/main/resources/templates"))){for(Path file:paths.filter(Files::isRegularFile).toList()){
            String text=Files.readString(file);assertFalse(text.contains("th:utext"));assertFalse(text.matches("(?is).*<script(?![^>]*src=)[^>]*>.*"));assertFalse(text.matches("(?is).*\\son(?:click|error|load)\\s*=.*"));assertFalse(text.contains("style=\""));
        }}
        try(var paths=Files.walk(Path.of("src/main/java"))){for(Path file:paths.filter(p->p.toString().endsWith(".java")).toList()){
            String text=Files.readString(file);assertFalse(text.contains("@DeleteMapping"));assertFalse(text.contains("RequestMethod.DELETE"));
        }}
    }
}
