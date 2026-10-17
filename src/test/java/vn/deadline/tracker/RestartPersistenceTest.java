package vn.deadline.tracker;

import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.MapPropertySource;
import vn.deadline.tracker.persistence.*;
import vn.deadline.tracker.service.WorkService;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RestartPersistenceTest extends DatabaseTest {
    ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(DeadlineTrackerApplication.class).web(WebApplicationType.NONE)
            .initializers(context->context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("restart-test",Map.of(
                "spring.datasource.url",DB.getJdbcUrl(),"spring.datasource.username",DB.getUsername(),"spring.datasource.password",DB.getPassword(),
                "spring.main.banner-mode","off","logging.level.root","ERROR"))))
            .run();
    }
    @Test @DisplayName("DATA-10 UAT-09 two real application contexts reload committed overdue task")
    void restart() {
        String userId=UUID.randomUUID().toString(),groupId,taskId;
        try(var first=start()) {
            var account=new AccountEntity();account.id=userId;account.username="r"+userId.replace("-","").substring(0,20);account.displayName="Restart";
            account.passwordHash="disabled-test-fixture";account.enabled=true;account.createdAt=Instant.now();
            first.getBean(AccountRepository.class).saveAndFlush(account);
            var work=first.getBean(WorkService.class);groupId=work.createGroup(userId,"Restart","").getId();
            taskId=work.createTask(userId,groupId,"Survives restart","",userId,Instant.now().plusSeconds(3600)).getId();
            first.getBean(JdbcTemplate.class).update("UPDATE task SET due_at=? WHERE id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(3600)),taskId);
        }
        try(var second=start()) {
            var work=second.getBean(WorkService.class);var task=work.task(userId,groupId,taskId);
            assertEquals("Survives restart",task.getTitle());assertTrue(task.isOverdue(Instant.now()));
            assertTrue(work.group(userId,groupId).isOwner(userId));
        }
    }
}
