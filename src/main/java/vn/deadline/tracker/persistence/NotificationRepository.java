package vn.deadline.tracker.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.*;
public interface NotificationRepository extends JpaRepository<NotificationEntity,String> {
    @Query(value="""
        SELECT n.* FROM notification n
        WHERE n.recipient_id=:recipient
          AND EXISTS (SELECT 1 FROM group_member m WHERE m.group_id=n.group_id AND m.user_id=:recipient)
          AND (n.kind<>'REMINDER' OR EXISTS (
              SELECT 1 FROM task t WHERE t.id=n.task_id AND t.group_id=n.group_id
              AND t.status<>'DONE' AND t.assignee_id=n.reminder_assignee_id
              AND t.due_at=n.reminder_due_at AND t.version=n.reminder_task_version))
          AND (:beforeAt IS NULL OR n.created_at<:beforeAt OR (n.created_at=:beforeAt AND n.id<:beforeId))
        ORDER BY n.created_at DESC,n.id DESC
        """,nativeQuery=true)
    List<NotificationEntity> inboxPage(@Param("recipient") String recipient,
        @Param("beforeAt") Instant beforeAt,@Param("beforeId") String beforeId,Pageable pageable);
    @Query(value="""
        SELECT COUNT(*) FROM notification n
        WHERE n.recipient_id=:recipient AND n.read_at IS NULL
          AND EXISTS (SELECT 1 FROM group_member m WHERE m.group_id=n.group_id AND m.user_id=:recipient)
          AND (n.kind<>'REMINDER' OR EXISTS (
              SELECT 1 FROM task t WHERE t.id=n.task_id AND t.group_id=n.group_id
              AND t.status<>'DONE' AND t.assignee_id=n.reminder_assignee_id
              AND t.due_at=n.reminder_due_at AND t.version=n.reminder_task_version))
        """,nativeQuery=true)
    long unreadVisible(@Param("recipient") String recipient);
    Optional<NotificationEntity> findByIdAndRecipientId(String id,String recipientId);
    boolean existsByEventKeyAndRecipientId(String eventKey,String recipientId);
}
