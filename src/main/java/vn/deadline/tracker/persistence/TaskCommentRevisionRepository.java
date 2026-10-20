package vn.deadline.tracker.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TaskCommentRevisionRepository extends JpaRepository<TaskCommentRevisionEntity,String> {
    List<TaskCommentRevisionEntity> findByCommentIdOrderByVersionAsc(String commentId);
}
