CREATE TABLE notification_preference (
    user_id VARCHAR(36) NOT NULL,
    reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    remind_24h BOOLEAN NOT NULL DEFAULT TRUE,
    remind_1h BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_preference_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

ALTER TABLE notification
    ADD COLUMN reminder_assignee_id VARCHAR(36),
    ADD COLUMN reminder_due_at DATETIME(6),
    ADD COLUMN reminder_task_version BIGINT;
