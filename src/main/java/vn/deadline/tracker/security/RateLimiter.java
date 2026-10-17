package vn.deadline.tracker.security;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class RateLimiter {
    private record Window(Instant expires,int attempts){}
    private final Map<String,Window> windows=new HashMap<>();private final Clock clock;
    public RateLimiter(Clock clock){this.clock=clock;}
    public synchronized boolean allow(String action,String ip,String username){
        Instant now=clock.instant();windows.entrySet().removeIf(e->!e.getValue().expires().isAfter(now));
        String normalized=username==null?"":username.strip().toLowerCase(Locale.ROOT);
        if(normalized.length()>64) normalized=normalized.substring(0,64);
        int seconds=action.equals("register")?3600:900;
        int ipMax=action.equals("register")?10:30;
        String ipKey=action+":ip:"+ip,userKey=action+":user:"+normalized;
        if(windows.size()>=10000 && (!windows.containsKey(ipKey)||!windows.containsKey(userKey))) return false;
        Window iw=windows.getOrDefault(ipKey,new Window(now.plusSeconds(seconds),0));
        Window uw=windows.getOrDefault(userKey,new Window(now.plusSeconds(seconds),0));
        if(iw.attempts()>=ipMax||uw.attempts()>=5) return false;
        windows.put(ipKey,new Window(iw.expires(),iw.attempts()+1));windows.put(userKey,new Window(uw.expires(),uw.attempts()+1));return true;
    }
}
