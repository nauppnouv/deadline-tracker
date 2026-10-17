package vn.deadline.tracker;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.test.web.servlet.MockMvc;
import vn.deadline.tracker.service.*;import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest @AutoConfigureMockMvc
class RepresentativeLoadTest extends DatabaseTest {
    @Autowired MockMvc mvc;@Autowired AccountService accounts;@Autowired WorkService work;@Autowired InvitationService invites;
    record Visitor(String id,Cookie session,String group,String task){}
    @Test @DisplayName("NFR-04 warm local MVC + MariaDB: 3x5x200, 5 concurrent actors, 100 view/save requests")
    void warmRequests()throws Exception{
        List<Visitor> visitors=new ArrayList<>();
        for(int g=0;g<3;g++){
            List<String> ids=new ArrayList<>();List<Cookie> cookies=new ArrayList<>();
            for(int u=0;u<5;u++){
                String name="load"+UUID.randomUUID().toString().replace("-","").substring(0,20);String pass="Load fixture only passphrase 123!";
                ids.add(accounts.register(name,"Load fixture",pass));
                var login=mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).param("username",name).param("password",pass)).andReturn();
                assertEquals(302,login.getResponse().getStatus());cookies.add(login.getResponse().getCookie("SESSION"));
            }
            String group=work.createGroup(ids.getFirst(),"Load group","").getId();
            for(int u=1;u<5;u++)invites.join(ids.get(u),invites.issue(ids.getFirst(),group));
            List<String> taskIds=new ArrayList<>();
            for(int n=0;n<200;n++)taskIds.add(work.createTask(ids.getFirst(),group,"Representative task "+n,"Synthetic content",ids.get(n%5),Instant.now().plusSeconds(86400)).getId());
            for(int u=0;u<5;u++)visitors.add(new Visitor(ids.get(u),cookies.get(u),group,taskIds.get(u)));
        }
        for(var v:visitors)assertEquals(200,mvc.perform(get("/groups/"+v.group()).cookie(v.session())).andReturn().getResponse().getStatus());
        List<Long> timings=Collections.synchronizedList(new ArrayList<>());
        try(var pool=Executors.newFixedThreadPool(5)){
            List<Callable<Void>> jobs=new ArrayList<>();
            for(int worker=0;worker<5;worker++){final int index=worker;jobs.add(()->{
                for(int n=0;n<20;n++){
                    var v=visitors.get((index+(n%3)*5)%visitors.size());String url="/groups/"+v.group();long start=System.nanoTime();
                    int status;
                    if(n%4==0){var task=work.task(v.id(),v.group(),v.task());start=System.nanoTime();status=mvc.perform(post(url+"/tasks/"+v.task()+"/status").cookie(v.session()).with(csrf()).param("status",n%8==0?"IN_PROGRESS":"TODO").param("version",Long.toString(task.getVersion()))).andReturn().getResponse().getStatus();assertEquals(302,status);}
                    else{status=mvc.perform(get(url).cookie(v.session()).param("due",n%2==0?"SOON":"ALL")).andReturn().getResponse().getStatus();assertEquals(200,status);}
                    timings.add((System.nanoTime()-start)/1_000_000);
                }return null;});}
            for(var result:pool.invokeAll(jobs))result.get();
        }
        timings.sort(Long::compareTo);long p95=timings.get(94);String report="NFR-04 local Windows, Docker Desktop MariaDB 11.8.9, Java "+System.getProperty("java.version")+"; MockMvc (no network/TLS/browser); 3 groups x 5 users x 200 tasks, 5 concurrent threads, 100 warm requests (75 views/25 saves), p95="+p95+"ms, max="+timings.getLast()+"ms; CPU="+Runtime.getRuntime().availableProcessors()+"; not a cloud SLA.\n";
        Files.writeString(Path.of("target/load-result.txt"),report);assertTrue(p95<2000,report);
    }
}
