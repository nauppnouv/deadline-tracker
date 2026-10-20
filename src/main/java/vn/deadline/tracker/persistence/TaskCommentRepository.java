package vn.deadline.tracker.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity,String> {
    Optional<TaskCommentEntity> findByIdAndTaskId(String id,String taskId);
    List<TaskCommentEntity> findByTaskIdOrderByCreatedAtDescIdDesc(String taskId);
}
