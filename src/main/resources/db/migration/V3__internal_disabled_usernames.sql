-- Public registration remains 3..30 characters. Only internal disabled identities use deleted-<UUID>.
ALTER TABLE app_user MODIFY username VARCHAR(44) CHARACTER SET ascii COLLATE ascii_bin NOT NULL;
ALTER TABLE app_user DROP CONSTRAINT ck_username;
ALTER TABLE app_user ADD CONSTRAINT ck_username CHECK (
 (username=LOWER(username) AND username REGEXP '^[a-z0-9._-]{3,30}$')
 OR (enabled=FALSE AND username REGEXP '^deleted-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$')
);
