ALTER TABLE notification ADD COLUMN reminder_offset INT;
ALTER TABLE notification_preference
 ADD COLUMN assignments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 ADD COLUMN comments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 ADD COLUMN mentions_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE task_attachment ADD COLUMN comment_id VARCHAR(36),
 ADD CONSTRAINT fk_attachment_comment_task FOREIGN KEY (comment_id,task_id) REFERENCES task_comment(id,task_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE push_preference (
 user_id VARCHAR(36) NOT NULL PRIMARY KEY,
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 grant_epoch BIGINT NOT NULL DEFAULT 0,
 registration_count INT NOT NULL DEFAULT 0,
 registration_window DATETIME(6) NOT NULL,
 CONSTRAINT fk_push_preference_user FOREIGN KEY(user_id) REFERENCES app_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE push_device (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL,
 endpoint VARCHAR(2048) NOT NULL,
 endpoint_hash VARCHAR(64) NOT NULL,
 p256dh VARCHAR(88) NOT NULL,
 auth_secret VARCHAR(24) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 grant_epoch BIGINT NOT NULL DEFAULT 1,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_push_endpoint UNIQUE(endpoint_hash),
 CONSTRAINT fk_push_device_user FOREIGN KEY(user_id) REFERENCES app_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 INDEX ix_push_user(user_id,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE push_delivery (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 notification_id VARCHAR(36) NOT NULL,
 device_id VARCHAR(36) NOT NULL,
 grant_epoch BIGINT NOT NULL,
 state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0,
 next_attempt_at DATETIME(6) NOT NULL,
 lease_until DATETIME(6),
 lease_token VARCHAR(36),
 created_at DATETIME(6) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 last_status INT,
 CONSTRAINT uq_push_delivery UNIQUE(notification_id,device_id,grant_epoch),
 CONSTRAINT fk_push_delivery_notification FOREIGN KEY(notification_id) REFERENCES notification(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_push_delivery_device FOREIGN KEY(device_id) REFERENCES push_device(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 INDEX ix_push_pending(state,next_attempt_at,lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
