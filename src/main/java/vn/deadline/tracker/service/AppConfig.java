package vn.deadline.tracker.service;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class AppConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.argon2.Argon2PasswordEncoder(16,32,1,19456,2);
    }
}
