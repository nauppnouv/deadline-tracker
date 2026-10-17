import java.time.LocalDateTime;

public class Task {
    private final String title;
    private final String description;
    private final String assigneeName;
    private final LocalDateTime dueAt;
    private TaskStatus status;

    public Task(String title, String description, String assigneeName, LocalDateTime dueAt) {
        // Kiem tra du lieu truoc khi tao cong viec.
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Ten cong viec khong duoc de trong.");
        }
        if (assigneeName == null || assigneeName.isBlank()) {
            throw new IllegalArgumentException("Can co nguoi phu trach.");
        }
        if (dueAt == null || !dueAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Han nop phai o tuong lai.");
        }

        this.title = title.strip();
        this.description = description == null ? "" : description.strip();
        this.assigneeName = assigneeName.strip();
        this.dueAt = dueAt;
        this.status = TaskStatus.TODO;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAssigneeName() {
        return this.assigneeName;
    }

    public LocalDateTime getDueAt() {
        return this.dueAt;
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public void updateStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Trang thai khong duoc de trong.");
        }
        this.status = newStatus;
    }
}
