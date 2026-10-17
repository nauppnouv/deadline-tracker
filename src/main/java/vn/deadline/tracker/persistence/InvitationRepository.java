package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InvitationRepository extends JpaRepository<InvitationEntity,String> {
 Optional<InvitationEntity> findByTokenHash(String tokenHash);
 List<InvitationEntity> findByGroupIdOrderByCreatedAtDesc(String groupId);
}
