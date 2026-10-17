package vn.deadline.tracker;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import vn.deadline.tracker.security.RateLimiter;
import static org.junit.jupiter.api.Assertions.*;
class RateLimitTest {
    static class MutableClock extends Clock {
        Instant now=Instant.parse("2026-10-17T00:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}
    }
    @Test @DisplayName("UAT-17 bounded rate limiter expires with injected Clock, IP and username limits independent")
    void boundsAndExpiry() {
        var clock=new MutableClock();var limiter=new RateLimiter(clock);
        for(int i=0;i<5;i++) assertTrue(limiter.allow("login","ip"+i,"USER"));
        assertFalse(limiter.allow("login","fresh-ip"," user "));
        for(int i=0;i<30;i++) assertTrue(limiter.allow("login","shared-ip","user"+i));
        assertFalse(limiter.allow("login","shared-ip","new-name"));
        clock.now=clock.now.plusSeconds(901);assertTrue(limiter.allow("login","shared-ip","user"));
    }
}
