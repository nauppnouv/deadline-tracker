package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AccountRepository extends JpaRepository<AccountEntity,String> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select a from AccountEntity a where a.id=:id")
 Optional<AccountEntity> findLockedById(@org.springframework.data.repository.query.Param("id") String id);
 Optional<AccountEntity> findByUsername(String username);
 boolean existsByUsername(String username);
}
