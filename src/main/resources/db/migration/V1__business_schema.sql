CREATE TABLE app_user (
 id VARCHAR(36) PRIMARY KEY, username VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 display_name VARCHAR(80) NOT NULL, password_hash VARCHAR(255) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_username CHECK (username=LOWER(username) AND username REGEXP '^[a-z0-9._-]{3,30}$'),
 CONSTRAINT ck_display_name CHECK (CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 80)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE project_group (
 id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL, description VARCHAR(1000) NOT NULL,
 owner_id VARCHAR(36) NOT NULL, created_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_group_owner FOREIGN KEY(owner_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT ck_group_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE group_member (
 id VARCHAR(36) PRIMARY KEY, group_id VARCHAR(36) NOT NULL, user_id VARCHAR(36) NOT NULL,
 role VARCHAR(10) NOT NULL, joined_at DATETIME(6) NOT NULL,
 owner_marker INT GENERATED ALWAYS AS (CASE WHEN role='OWNER' THEN 1 ELSE NULL END) VIRTUAL,
 CONSTRAINT uq_group_member UNIQUE(group_id,user_id), CONSTRAINT uq_one_owner UNIQUE(group_id,owner_marker),
 CONSTRAINT fk_member_group FOREIGN KEY(group_id) REFERENCES project_group(id) ON DELETE RESTRICT,
 CONSTRAINT fk_member_user FOREIGN KEY(user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT ck_member_role CHECK(role IN ('OWNER','MEMBER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE task (
 id VARCHAR(36) PRIMARY KEY, group_id VARCHAR(36) NOT NULL, title VARCHAR(150) NOT NULL, description VARCHAR(2000) NOT NULL,
 assignee_id VARCHAR(36) NOT NULL, created_by VARCHAR(36) NOT NULL, due_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL, completed_at DATETIME(6), created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_task_group FOREIGN KEY(group_id) REFERENCES project_group(id) ON DELETE RESTRICT,
 CONSTRAINT fk_task_assignee FOREIGN KEY(group_id,assignee_id) REFERENCES group_member(group_id,user_id) ON DELETE RESTRICT,
 CONSTRAINT fk_task_creator FOREIGN KEY(group_id,created_by) REFERENCES group_member(group_id,user_id) ON DELETE RESTRICT,
 CONSTRAINT ck_task_title CHECK(CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 150),
 CONSTRAINT ck_task_status CHECK(status IN ('TODO','IN_PROGRESS','DONE')),
 CONSTRAINT ck_task_completion CHECK((status='DONE' AND completed_at IS NOT NULL) OR (status<>'DONE' AND completed_at IS NULL)),
 INDEX ix_task_group_due(group_id,due_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE invitation (
 id VARCHAR(36) PRIMARY KEY, group_id VARCHAR(36) NOT NULL, token_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
 created_by VARCHAR(36) NOT NULL, created_at DATETIME(6) NOT NULL, expires_at DATETIME(6) NOT NULL,
 used_at DATETIME(6), used_by VARCHAR(36), revoked_at DATETIME(6), version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_invite_group FOREIGN KEY(group_id) REFERENCES project_group(id) ON DELETE RESTRICT,
 CONSTRAINT fk_invite_creator FOREIGN KEY(created_by) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT fk_invite_used_by FOREIGN KEY(used_by) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT ck_invite_used CHECK((used_at IS NULL AND used_by IS NULL) OR (used_at IS NOT NULL AND used_by IS NOT NULL)),
 CONSTRAINT ck_invite_expiry CHECK(expires_at>created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE task_event (
 id VARCHAR(36) PRIMARY KEY, task_id VARCHAR(36) NOT NULL, actor_id VARCHAR(36) NOT NULL,
 event_type VARCHAR(20) NOT NULL, changed_at DATETIME(6) NOT NULL,
 old_status VARCHAR(20), new_status VARCHAR(20), old_assignee VARCHAR(36), new_assignee VARCHAR(36), old_due DATETIME(6), new_due DATETIME(6),
 CONSTRAINT fk_event_task FOREIGN KEY(task_id) REFERENCES task(id) ON DELETE RESTRICT,
 CONSTRAINT fk_event_actor FOREIGN KEY(actor_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT ck_event_type CHECK(event_type IN ('CREATED','UPDATED','REASSIGNED','STATUS_CHANGED')),
 INDEX ix_event_task(task_id,changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
