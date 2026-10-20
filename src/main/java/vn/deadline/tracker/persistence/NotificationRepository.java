package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NotificationRepository extends JpaRepository<NotificationEntity,String> {
    List<NotificationEntity> findByRecipientIdOrderByCreatedAtDescIdDesc(String recipientId);
    Optional<NotificationEntity> findByIdAndRecipientId(String id,String recipientId);
    boolean existsByEventKeyAndRecipientId(String eventKey,String recipientId);
    long countByRecipientIdAndReadAtIsNull(String recipientId);
}
