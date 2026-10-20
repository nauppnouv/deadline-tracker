package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import vn.deadline.tracker.persistence.*;
@Service @Transactional
public class ReminderPreferenceService {
 public record Preferences(boolean enabled,boolean twentyFourHours,boolean oneHour){}
 private final AccountRepository accounts;private final NotificationPreferenceRepository rows;
 public ReminderPreferenceService(AccountRepository accounts,NotificationPreferenceRepository rows){this.accounts=accounts;this.rows=rows;}
 @Transactional(readOnly=true) public Preferences get(String actor){accounts.findById(actor).filter(a->a.enabled).orElseThrow(vn.deadline.tracker.domain.RuleViolation::missing);return rows.findById(actor).map(r->new Preferences(r.remindersEnabled,r.remind24h,r.remind1h)).orElse(new Preferences(false,true,true));}
 public void update(String actor,boolean enabled,boolean twentyFourHours,boolean oneHour){accounts.findById(actor).filter(a->a.enabled).orElseThrow(vn.deadline.tracker.domain.RuleViolation::missing);if(enabled&&!twentyFourHours&&!oneHour)throw vn.deadline.tracker.domain.RuleViolation.invalid("Chọn ít nhất một mốc nhắc hạn hoặc tắt nhắc hạn.");var row=rows.findById(actor).orElseGet(()->{var p=new NotificationPreferenceEntity();p.userId=actor;return p;});row.remindersEnabled=enabled;row.remind24h=twentyFourHours;row.remind1h=oneHour;rows.save(row);}
}
