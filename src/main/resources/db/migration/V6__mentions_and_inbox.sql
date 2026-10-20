CREATE TABLE comment_mention (
    id VARCHAR(36) NOT NULL,
    comment_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    mentioned_user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_comment_mention UNIQUE (comment_id, mentioned_user_id),
    CONSTRAINT fk_mention_comment FOREIGN KEY (comment_id) REFERENCES task_comment(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_mention_group FOREIGN KEY (group_id) REFERENCES project_group(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_mention_member FOREIGN KEY (group_id, mentioned_user_id) REFERENCES group_member(group_id, user_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE notification (
    id VARCHAR(36) NOT NULL,
    event_key VARCHAR(36) NOT NULL,
    recipient_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    comment_id VARCHAR(36),
    kind VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_notification_recipient_event UNIQUE (event_key, recipient_id),
    CONSTRAINT fk_notification_member FOREIGN KEY (group_id, recipient_id) REFERENCES group_member(group_id, user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_notification_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_notification_comment FOREIGN KEY (comment_id) REFERENCES task_comment(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_notification_inbox (recipient_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
