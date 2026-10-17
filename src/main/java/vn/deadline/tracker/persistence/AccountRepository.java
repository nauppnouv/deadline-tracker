package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AccountRepository extends JpaRepository<AccountEntity,String> {
 Optional<AccountEntity> findByUsername(String username);
 boolean existsByUsername(String username);
}
