package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TaskEventRepository extends JpaRepository<TaskEventEntity,String> {
 List<TaskEventEntity> findByTaskIdOrderByChangedAtAsc(String taskId);
 List<TaskEventEntity> findByTaskIdOrderByChangedAtDescIdDesc(String taskId);
}
