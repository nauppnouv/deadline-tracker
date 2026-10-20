package vn.deadline.tracker.persistence;
import jakarta.persistence.*;
@Entity @Table(name="notification_preference") public class NotificationPreferenceEntity{@Id @Column(name="user_id",length=36)public String userId;@Column(name="reminders_enabled")public boolean remindersEnabled;@Column(name="remind_24h")public boolean remind24h;@Column(name="remind_1h")public boolean remind1h;}
