package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import vn.deadline.tracker.persistence.*;
import vn.deadline.tracker.domain.*;
import java.time.*;
import java.util.UUID;
@Service @Transactional
public class AccountService {
    private final AccountRepository accounts;private final PasswordEncoder encoder;private final SessionStore sessions;private final Clock clock;
    public AccountService(AccountRepository accounts,PasswordEncoder encoder,SessionStore sessions,Clock clock){this.accounts=accounts;this.encoder=encoder;this.sessions=sessions;this.clock=clock;}
    public String register(String username,String displayName,String password){
        String normalized=Inputs.username(username);String display=Inputs.required(displayName,80,"Tên hiển thị");Inputs.password(password);
        if(accounts.existsByUsername(normalized))throw RuleViolation.invalid("Tên tài khoản đã được sử dụng.");
        var row=new AccountEntity();row.id=UUID.randomUUID().toString();row.username=normalized;row.displayName=display;
        row.passwordHash=encoder.encode(password);row.enabled=true;row.createdAt=clock.instant();
        try{return accounts.saveAndFlush(row).id;}catch(DataIntegrityViolationException e){throw RuleViolation.invalid("Không thể tạo tài khoản với thông tin này.");}
    }
    public void changePassword(String actor,String current,String next){
        var row=accounts.findLockedById(actor).orElseThrow(RuleViolation::missing);Inputs.password(next);
        if(!row.enabled||current==null||!encoder.matches(current,row.passwordHash))throw RuleViolation.invalid("Mật khẩu hiện tại không đúng.");
        row.passwordHash=encoder.encode(next);row.mustChangePassword=false;accounts.flush();sessions.invalidateAll(row.username);
    }
    @Transactional(readOnly=true) public AccountEntity current(String actor){return accounts.findById(actor).filter(a->a.enabled).orElseThrow(RuleViolation::missing);}
}
