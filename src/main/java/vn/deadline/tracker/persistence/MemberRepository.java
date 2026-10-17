package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MemberRepository extends JpaRepository<MemberEntity,String> {
 List<MemberEntity> findByGroupIdOrderByJoinedAtAsc(String groupId);
 List<MemberEntity> findByUserId(String userId);
 boolean existsByGroupIdAndUserId(String groupId,String userId);
}
