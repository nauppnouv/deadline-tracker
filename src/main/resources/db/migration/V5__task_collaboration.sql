CREATE TABLE task_comment (
    id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    group_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    hidden_at DATETIME(6) NULL,
    hidden_by VARCHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_group FOREIGN KEY (group_id) REFERENCES project_group(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_author FOREIGN KEY (group_id, author_id) REFERENCES group_member(group_id, user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_hidden_by FOREIGN KEY (group_id, hidden_by) REFERENCES group_member(group_id, user_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_comment_body CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 4000),
    INDEX ix_comment_task_created (task_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE task_comment_revision (
    id VARCHAR(36) NOT NULL,
    comment_id VARCHAR(36) NOT NULL,
    editor_id VARCHAR(36) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    version BIGINT NOT NULL,
    revised_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_revision_comment FOREIGN KEY (comment_id) REFERENCES task_comment(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_revision_editor FOREIGN KEY (editor_id) REFERENCES app_user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_revision_body CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 4000),
    INDEX ix_revision_comment_version (comment_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
