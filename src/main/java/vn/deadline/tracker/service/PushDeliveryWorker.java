package vn.deadline.tracker.service;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

@Component
public class PushDeliveryWorker {
    private final JdbcTemplate jdbc;private final PushTransport transport;private final PushSettings settings;
    private final TransactionTemplate transactions;private final Clock clock;
    public PushDeliveryWorker(JdbcTemplate jdbc,PushTransport transport,PushSettings settings,PlatformTransactionManager manager,Clock clock){this.jdbc=jdbc;this.transport=transport;this.settings=settings;this.transactions=new TransactionTemplate(manager);this.clock=clock;}
    @Scheduled(fixedDelayString="${app.push.interval-ms:15000}") public void scheduled(){if(settings.enabled)runOnce();}
    public int runOnce() {
        if(!settings.enabled)return 0;
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Push worker must run after business commit");
        Instant now=clock.instant();var due=jdbc.queryForList("SELECT id FROM push_delivery WHERE ((state='PENDING' AND next_attempt_at<=?) OR (state='LEASED' AND lease_until<=?)) ORDER BY created_at,id LIMIT 20",String.class,Timestamp.from(now),Timestamp.from(now));
        int claimed=0;
        for(String id:due) {
            String token=UUID.randomUUID().toString();
            int changed=transactions.execute(s->jdbc.update("UPDATE push_delivery SET state='LEASED',lease_token=?,lease_until=?,attempts=attempts+1 WHERE id=? AND ((state='PENDING' AND next_attempt_at<=?) OR (state='LEASED' AND lease_until<=?))",token,Timestamp.from(now.plusSeconds(120)),id,Timestamp.from(now),Timestamp.from(now)));
            if(changed==0)continue;claimed++;
            transactions.executeWithoutResult(s->deliver(id,token));
        }
        return claimed;
    }
    private void deliver(String id,String token) {
        var rows=jdbc.queryForList("SELECT o.*,d.user_id,d.endpoint,d.p256dh,d.auth_secret,d.enabled AS device_enabled,d.grant_epoch AS current_epoch FROM push_delivery o JOIN push_device d ON d.id=o.device_id WHERE o.id=? AND o.lease_token=? AND o.state='LEASED' FOR UPDATE",id,token);
        if(rows.isEmpty())return;var row=rows.getFirst();Instant now=clock.instant();
        // Lock device along with delivery: revocation and this bounded network attempt serialize.
        boolean valid=number(row,"current_epoch")==number(row,"grant_epoch")&&number(row,"device_enabled")==1
            &&((Timestamp)row.get("expires_at")).toInstant().isAfter(now)&&number(row,"attempts")<=6;
        if(!valid||!recipientAllowed((String)row.get("notification_id"),(String)row.get("user_id"),now)){finish(id,token,"CANCELLED",null,null);return;}
        PushTransport.Result result;
        try {result=transport.send(new PushTransport.Message((String)row.get("endpoint"),(String)row.get("p256dh"),(String)row.get("auth_secret"),(String)row.get("notification_id")));}
        catch(Exception failure){retry(id,token,row,now,0,null);return;}
        int status=result.status();
        if(status>=200&&status<300)finish(id,token,"ACCEPTED",status,null);
        else if(status==404||status==410){jdbc.update("UPDATE push_device SET enabled=FALSE,grant_epoch=grant_epoch+1 WHERE id=? AND grant_epoch=?",row.get("device_id"),row.get("grant_epoch"));finish(id,token,"EXPIRED",status,null);}
        else if(status==401||status==403)finish(id,token,"CONFIG_ERROR",status,null);
        else if(status==429||status>=500)retry(id,token,row,now,status,result.retryAfter());
        else finish(id,token,"FAILED",status,null); // Includes redirects; never follow Location.
    }
    private boolean recipientAllowed(String notification,String user,Instant now) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM notification n
            JOIN app_user u ON u.id=n.recipient_id AND u.enabled=TRUE
            JOIN group_member m ON m.group_id=n.group_id AND m.user_id=n.recipient_id
            JOIN push_preference pp ON pp.user_id=u.id AND pp.enabled=TRUE
            JOIN task t ON t.id=n.task_id AND t.group_id=n.group_id
            LEFT JOIN task_comment c ON c.id=n.comment_id
            LEFT JOIN notification_preference p ON p.user_id=u.id
            WHERE n.id=? AND n.recipient_id=? AND (n.comment_id IS NULL OR c.hidden_at IS NULL)
            AND (n.kind<>'TASK_ASSIGNMENT' OR COALESCE(p.assignments_enabled,TRUE))
            AND (n.kind<>'COMMENT' OR COALESCE(p.comments_enabled,TRUE))
            AND (n.kind<>'MENTION' OR COALESCE(p.mentions_enabled,TRUE))
            AND (n.kind<>'REMINDER' OR (p.reminders_enabled=TRUE AND t.status<>'DONE'
                AND t.assignee_id=n.reminder_assignee_id AND t.due_at=n.reminder_due_at
                AND t.version=n.reminder_task_version AND t.due_at>?
                AND ((n.reminder_offset=86400 AND p.remind_24h=TRUE) OR (n.reminder_offset=3600 AND p.remind_1h=TRUE))))
            """,Integer.class,notification,user,Timestamp.from(now))==1;
    }
    private void retry(String id,String token,Map<String,Object> row,Instant now,int status,String retryAfter) {
        if(number(row,"attempts")>=6){finish(id,token,"FAILED",status,null);return;}
        long seconds=Math.min(3600,30L<<(int)number(row,"attempts"));
        if(status==429&&retryAfter!=null)seconds=retrySeconds(retryAfter,now);
        finish(id,token,"PENDING",status,Timestamp.from(now.plusSeconds(seconds)));
    }
    public static long retrySeconds(String value,Instant now) {
        try {long seconds;try {seconds=Long.parseLong(value);}catch(NumberFormatException e){seconds=Duration.between(now,ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds();}return Math.max(60,Math.min(3600,seconds));}
        catch(Exception invalid){return 60;}
    }
    private long number(Map<String,Object> row,String key){Object value=row.get(key);return value instanceof Boolean b?(b?1:0):((Number)value).longValue();}
    private void finish(String id,String token,String state,Integer status,Timestamp next){jdbc.update("UPDATE push_delivery SET state=?,last_status=?,next_attempt_at=COALESCE(?,next_attempt_at),lease_until=NULL,lease_token=NULL WHERE id=? AND lease_token=?",state,status,next,id,token);}
}
