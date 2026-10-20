package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity,String> {
    List<CommentMentionEntity> findByCommentId(String commentId);
}
