package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TaskRepository extends JpaRepository<TaskEntity,String> {
 Optional<TaskEntity> findByIdAndGroupId(String id,String groupId);
 List<TaskEntity> findByGroupIdOrderByDueAtAscIdAsc(String groupId);
}
