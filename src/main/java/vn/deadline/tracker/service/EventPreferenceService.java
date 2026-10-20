package vn.deadline.tracker.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class EventPreferenceService {
    public record Preferences(boolean assignments,boolean comments,boolean mentions) {}
    private final JdbcTemplate jdbc;private final GroupAccess access;
    public EventPreferenceService(JdbcTemplate jdbc,GroupAccess access){this.jdbc=jdbc;this.access=access;}
    @Transactional(readOnly=true) public Preferences get(String actor){
        access.active(actor);
        List<Preferences> rows=jdbc.query("SELECT assignments_enabled,comments_enabled,mentions_enabled FROM notification_preference WHERE user_id=?",(r,n)->new Preferences(r.getBoolean(1),r.getBoolean(2),r.getBoolean(3)),actor);
        return rows.isEmpty()?new Preferences(true,true,true):rows.getFirst();
    }
    public void update(String actor,boolean assignments,boolean comments,boolean mentions){
        access.active(actor);jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE",String.class,actor);
        jdbc.update("INSERT IGNORE INTO notification_preference(user_id) VALUES (?)",actor);
        jdbc.update("UPDATE notification_preference SET assignments_enabled=?,comments_enabled=?,mentions_enabled=? WHERE user_id=?",assignments,comments,mentions,actor);
    }
    @Transactional(readOnly=true) public boolean allows(String actor,String kind){
        var p=get(actor);return switch(kind){case "TASK_ASSIGNMENT"->p.assignments();case "COMMENT"->p.comments();case "MENTION"->p.mentions();default->true;};
    }
}
