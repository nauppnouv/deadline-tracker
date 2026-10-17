package vn.deadline.tracker.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
@Service
public class SessionStore {
    private final JdbcTemplate jdbc;public SessionStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void invalidateAll(String username){jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?",username);}
}
