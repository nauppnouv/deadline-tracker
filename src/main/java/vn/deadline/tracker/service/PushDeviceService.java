package vn.deadline.tracker.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.deadline.tracker.domain.RuleViolation;

@Service @Transactional
public class PushDeviceService {
    public record Device(String id,boolean enabled,Instant updatedAt){}
    private final JdbcTemplate jdbc;private final GroupAccess access;private final PushEndpointPolicy policy;
    private final PushSettings settings;private final Clock clock;
    public PushDeviceService(JdbcTemplate jdbc,GroupAccess access,PushEndpointPolicy policy,PushSettings settings,Clock clock){this.jdbc=jdbc;this.access=access;this.policy=policy;this.settings=settings;this.clock=clock;}
    public String enable(String actor,String endpoint,String p256dh,String auth){return enableForSession(actor,endpoint,p256dh,auth,null);}
    public long sessionEpoch(String actor){access.active(actor);var rows=jdbc.queryForList("SELECT grant_epoch FROM push_preference WHERE user_id=?",Long.class,actor);return rows.isEmpty()?0:rows.getFirst();}
    public String enableForSession(String actor,String endpoint,String p256dh,String auth,Long expectedEpoch) {
        access.active(actor);if(!settings.enabled)throw RuleViolation.invalid("Thông báo desktop chưa được cấu hình trên máy chủ.");
        try {
            policy.endpoint(endpoint);Security.addProvider(new BouncyCastleProvider());
            if(p256dh==null||auth==null||!p256dh.matches("[A-Za-z0-9_-]{87}=?")||!auth.matches("[A-Za-z0-9_-]{22}={0,2}")
                ||Base64.getUrlDecoder().decode(p256dh).length!=65||Base64.getUrlDecoder().decode(auth).length!=16
                ||Base64.getUrlDecoder().decode(p256dh)[0]!=4)throw new IllegalArgumentException();
            nl.martijndwars.webpush.Utils.loadPublicKey(p256dh);
        } catch(Exception invalid){throw RuleViolation.invalid("Thiết bị hoặc nhà cung cấp thông báo không hợp lệ.");}
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE",String.class,actor);
        if(expectedEpoch!=null&&sessionEpoch(actor)!=expectedEpoch)throw RuleViolation.forbidden();
        Instant now=clock.instant();var ts=Timestamp.from(now);
        jdbc.update("INSERT IGNORE INTO push_preference(user_id,registration_window) VALUES (?,?)",actor,ts);
        var preference=jdbc.queryForMap("SELECT registration_window,registration_count FROM push_preference WHERE user_id=? FOR UPDATE",actor);
        Instant window=((Timestamp)preference.get("registration_window")).toInstant();
        int attempts=((Number)preference.get("registration_count")).intValue();
        if(window.plusSeconds(3600).isBefore(now)){window=now;attempts=0;}
        if(attempts>=20)throw RuleViolation.invalid("Bạn đã thử bật thông báo quá nhiều lần. Hãy thử lại sau.");
        String hash=hash(endpoint);
        var existing=jdbc.queryForList("SELECT id,user_id FROM push_device WHERE endpoint_hash=? FOR UPDATE",hash);
        if(!existing.isEmpty()&&!actor.equals(existing.getFirst().get("user_id")))throw RuleViolation.conflict();
        if(existing.isEmpty()&&jdbc.queryForObject("SELECT COUNT(*) FROM push_device WHERE user_id=?",Integer.class,actor)>=5) {
            existing=jdbc.queryForList("SELECT id,user_id FROM push_device WHERE user_id=? AND enabled=FALSE ORDER BY updated_at LIMIT 1 FOR UPDATE",actor);
            if(existing.isEmpty())throw RuleViolation.invalid("Tối đa 5 thiết bị. Hãy tắt một thiết bị cũ trước.");
        }
        jdbc.update("UPDATE push_preference SET enabled=TRUE,registration_window=?,registration_count=? WHERE user_id=?",Timestamp.from(window),attempts+1,actor);
        String id=existing.isEmpty()?UUID.randomUUID().toString():(String)existing.getFirst().get("id");
        if(existing.isEmpty())jdbc.update("INSERT INTO push_device(id,user_id,endpoint,endpoint_hash,p256dh,auth_secret,enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,TRUE,?,?)",id,actor,endpoint,hash,p256dh,auth,ts,ts);
        else jdbc.update("UPDATE push_device SET endpoint=?,endpoint_hash=?,p256dh=?,auth_secret=?,enabled=TRUE,grant_epoch=grant_epoch+1,updated_at=? WHERE id=? AND user_id=?",endpoint,hash,p256dh,auth,ts,id,actor);
        return id;
    }
    public void disable(String actor,String device) {
        access.active(actor);
        if(jdbc.update("UPDATE push_device SET enabled=FALSE,grant_epoch=grant_epoch+1,updated_at=? WHERE id=? AND user_id=?",Timestamp.from(clock.instant()),device,actor)!=1)throw RuleViolation.missing();
    }
    public void disableAll(String actor) {
        access.active(actor);jdbc.update("UPDATE push_preference SET enabled=FALSE WHERE user_id=?",actor);
        jdbc.update("UPDATE push_device SET enabled=FALSE,grant_epoch=grant_epoch+1,updated_at=? WHERE user_id=?",Timestamp.from(clock.instant()),actor);
    }
    @Transactional(readOnly=true) public List<Device> devices(String actor) {
        access.active(actor);return jdbc.query("SELECT id,enabled,updated_at FROM push_device WHERE user_id=? ORDER BY created_at",(rs,n)->new Device(rs.getString(1),rs.getBoolean(2),rs.getTimestamp(3).toInstant()),actor);
    }
    /** Session attributes are server-managed and survive session fixation protection. */
    public void accountSwitched(jakarta.servlet.http.HttpSession session,String actor) {
        if(session==null)return;Object previous=session.getAttribute("push.owner"),device=session.getAttribute("push.device");
        if(previous instanceof String old&&device instanceof String id&&!old.equals(actor)) {
            jdbc.update("UPDATE push_device SET enabled=FALSE,grant_epoch=grant_epoch+1 WHERE id=? AND user_id=?",id,old);
            session.removeAttribute("push.owner");session.removeAttribute("push.device");
        }
    }
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException();}}
}
