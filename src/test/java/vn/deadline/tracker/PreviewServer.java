package vn.deadline.tracker;

import org.springframework.boot.SpringApplication;
import vn.deadline.tracker.service.*;
import java.nio.file.*;import java.time.*;import java.util.*;

/** Disposable local UI fixture: never included in the production jar. */
public class PreviewServer extends DatabaseTest {
    public static void main(String[] args)throws Exception{
        var app=new SpringApplication(DeadlineTrackerApplication.class);
        app.addInitializers(ctx->ctx.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("preview-db",Map.of("spring.datasource.url",DB.getJdbcUrl(),"spring.datasource.username",DB.getUsername(),"spring.datasource.password",DB.getPassword()))));
        try(var context=app.run("--server.address=127.0.0.1","--server.port=18080","--spring.session.jdbc.cleanup-cron=-")){
            var accounts=context.getBean(AccountService.class);var work=context.getBean(WorkService.class);var invitations=context.getBean(InvitationService.class);
            String owner=accounts.register("demo.owner","Minh (dữ liệu mẫu)","Only a local demo passphrase 123!");
            String member=accounts.register("demo.member","An (dữ liệu mẫu)","Only a local demo passphrase 123!");
            String group=work.createGroup(owner,"Đồ án Deadline Tracker","Nhóm thử nghiệm, không chứa thông tin thật.").getId();
            invitations.join(member,invitations.issue(owner,group));
            work.createTask(owner,group,"Thiết kế cơ sở dữ liệu","Kiểm tra quan hệ nhóm, thành viên và công việc.",owner,Instant.now().plusSeconds(7200));
            var done=work.createTask(owner,group,"Chốt yêu cầu nhóm","",member,Instant.now().plusSeconds(86400));
            work.changeStatus(member,group,done.getId(),vn.deadline.tracker.domain.TaskStatus.DONE,done.getVersion());
            System.out.println("PREVIEW_READY http://127.0.0.1:18080/login");
            Instant end=Instant.now().plusSeconds(1800);
            while(Instant.now().isBefore(end)&&!Files.exists(Path.of("target/preview.stop")))Thread.sleep(500);
        }
    }
}
