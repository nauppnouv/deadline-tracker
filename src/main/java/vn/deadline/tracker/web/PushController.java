package vn.deadline.tracker.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.deadline.tracker.domain.RuleViolation;
import vn.deadline.tracker.security.TrackerPrincipal;
import vn.deadline.tracker.service.*;

@RestController @RequestMapping("/api/push")
public class PushController {
    public record Subscription(String endpoint,String p256dh,String auth){}
    private final PushDeviceService devices;private final PushSettings settings;private final vn.deadline.tracker.security.RateLimiter limiter;
    public PushController(PushDeviceService devices,PushSettings settings,vn.deadline.tracker.security.RateLimiter limiter){this.devices=devices;this.settings=settings;this.limiter=limiter;}
    @GetMapping("/config") ResponseEntity<?> config(@AuthenticationPrincipal TrackerPrincipal actor,HttpServletRequest request){
        var session=request.getSession(false);devices.accountSwitched(session,actor.id());
        return ResponseEntity.ok().header("Cache-Control","no-store").body(Map.of("configured",settings.enabled,"publicKey",settings.enabled?settings.publicKey:"","devices",devices.devices(actor.id()),"current",session==null?"":Objects.toString(session.getAttribute("push.device"),"")));
    }
    @PostMapping(path="/devices",consumes="application/json") ResponseEntity<?> enable(@AuthenticationPrincipal TrackerPrincipal actor,@RequestBody Subscription body,HttpServletRequest request){
        if(!limiter.allow("push",request.getRemoteAddr(),actor.id()))return ResponseEntity.status(429).body(Map.of("error","Hãy thử bật thông báo lại sau."));
        devices.accountSwitched(request.getSession(false),actor.id());
        var current=request.getSession(false);if(current==null||!(current.getAttribute("push.epoch") instanceof Long epoch))return ResponseEntity.status(401).body(Map.of("error","Hãy đăng nhập lại để bật thông báo."));
        String id=devices.enableForSession(actor.id(),body.endpoint(),body.p256dh(),body.auth(),epoch);
        var session=request.getSession();session.setAttribute("push.owner",actor.id());session.setAttribute("push.device",id);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(Map.of("id",id));
    }
    @PostMapping("/devices/{id}/disable") Map<String,Boolean> disable(@AuthenticationPrincipal TrackerPrincipal actor,@PathVariable String id){devices.disable(actor.id(),id);return Map.of("disabled",true);}
    @PostMapping("/disable-all") Map<String,Boolean> disableAll(@AuthenticationPrincipal TrackerPrincipal actor){devices.disableAll(actor.id());return Map.of("disabled",true);}
    @ExceptionHandler(RuleViolation.class) ResponseEntity<?> invalid(RuleViolation error){return ResponseEntity.status(switch(error.getKind()){case NOT_FOUND->404;case FORBIDDEN->403;case CONFLICT->409;case VALIDATION->400;}).body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) ResponseEntity<?> malformed(){return ResponseEntity.badRequest().body(Map.of("error","Thông tin thiết bị không hợp lệ."));}
}
