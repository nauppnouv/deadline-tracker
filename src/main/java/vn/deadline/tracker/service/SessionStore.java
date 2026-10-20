package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
@Service
public class SessionStore {
    private final JdbcTemplate jdbc;public SessionStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @org.springframework.transaction.annotation.Transactional
    public void invalidateAll(String username){
        var ids=jdbc.queryForList("SELECT id FROM app_user WHERE username=? FOR UPDATE",String.class,username);
        for(String id:ids)revoke(id);
        jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?",username);
    }
    @org.springframework.transaction.annotation.Transactional
    public void invalidateAll(String username,String userId){
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE",String.class,userId);revoke(userId);
        jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?",username);
    }
    private void revoke(String userId){
        jdbc.update("INSERT INTO push_preference(user_id,enabled,grant_epoch,registration_window) VALUES (?,FALSE,1,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE grant_epoch=grant_epoch+1",userId);
        jdbc.update("UPDATE push_device SET enabled=FALSE,grant_epoch=grant_epoch+1 WHERE user_id=?",userId);
    }
}
