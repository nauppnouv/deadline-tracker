package vn.deadline.tracker.ops;
import org.springframework.stereotype.Service;import org.springframework.context.annotation.Profile;import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;import vn.deadline.tracker.persistence.*;import vn.deadline.tracker.service.SessionStore;import vn.deadline.tracker.domain.*;
import java.security.SecureRandom;import java.util.*;
@Service @Profile("ops") @Transactional
public class OpsActions {
    private final AccountRepository users;private final PasswordEncoder encoder;private final SessionStore sessions;private final SecureRandom random=new SecureRandom();
    public OpsActions(AccountRepository users,PasswordEncoder encoder,SessionStore sessions){this.users=users;this.encoder=encoder;this.sessions=sessions;}
    private AccountEntity account(String username){String normalized=Inputs.username(username);var found=users.findByUsername(normalized).orElseThrow(RuleViolation::missing);return users.findLockedById(found.id).orElseThrow(RuleViolation::missing);}
    private String temporary(){byte[] bytes=new byte[24];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public String resetPassword(String username){var user=account(username);if(!user.enabled)throw RuleViolation.invalid("Tài khoản không còn hoạt động.");String temporary=temporary();user.passwordHash=encoder.encode(temporary);user.mustChangePassword=true;users.flush();sessions.invalidateAll(user.username);return temporary;}
    public void anonymize(String username){var user=account(username);String old=user.username;UUID.fromString(user.id);user.enabled=false;user.username="deleted-"+user.id;user.displayName="Đã xóa";user.passwordHash=encoder.encode(temporary());user.mustChangePassword=false;users.flush();sessions.invalidateAll(old);}
}
