-- Refuse inconsistent existing data rather than guessing a new Owner.
CREATE TEMPORARY TABLE owner_integrity_guard (
 violations BIGINT NOT NULL,
 CONSTRAINT ck_existing_owner_membership CHECK (violations=0)
);
INSERT INTO owner_integrity_guard
 SELECT COUNT(*) FROM project_group g
 LEFT JOIN group_member m ON m.group_id=g.id AND m.user_id=g.owner_id
 WHERE m.id IS NULL OR m.role<>'OWNER';
INSERT INTO owner_integrity_guard
 SELECT COUNT(*) FROM group_member m JOIN project_group g ON g.id=m.group_id
 WHERE m.role='OWNER' AND m.user_id<>g.owner_id;
DROP TEMPORARY TABLE owner_integrity_guard;

DELIMITER //
-- At least one matching Owner, atomically with the group INSERT (InnoDB).
CREATE TRIGGER tr_group_create_owner AFTER INSERT ON project_group
FOR EACH ROW
BEGIN
 INSERT INTO group_member(id,group_id,user_id,role,joined_at)
 VALUES(UUID(),NEW.id,NEW.owner_id,'OWNER',NEW.created_at);
END//

-- Owner transfer and changing stable group IDs are outside W1.
CREATE TRIGGER tr_group_stable_owner BEFORE UPDATE ON project_group
FOR EACH ROW
BEGIN
 IF NEW.owner_id<>OLD.owner_id OR NEW.id<>OLD.id THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Group identity and Owner cannot be changed in W1';
 END IF;
END//

CREATE TRIGGER tr_member_owner_insert BEFORE INSERT ON group_member
FOR EACH ROW
BEGIN
 DECLARE group_owner VARCHAR(36);
 SELECT owner_id INTO group_owner FROM project_group WHERE id=NEW.group_id;
 IF (NEW.role='OWNER' AND NEW.user_id<>group_owner)
    OR (NEW.user_id=group_owner AND NEW.role<>'OWNER') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Owner membership must match the group Owner';
 END IF;
END//

CREATE TRIGGER tr_member_owner_update BEFORE UPDATE ON group_member
FOR EACH ROW
BEGIN
 DECLARE group_owner VARCHAR(36);
 IF OLD.role='OWNER' AND (NEW.role<>'OWNER' OR NEW.group_id<>OLD.group_id OR NEW.user_id<>OLD.user_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Owner membership cannot be removed or moved in W1';
 END IF;
 SELECT owner_id INTO group_owner FROM project_group WHERE id=NEW.group_id;
 IF (NEW.role='OWNER' AND NEW.user_id<>group_owner)
    OR (NEW.user_id=group_owner AND NEW.role<>'OWNER') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Owner membership must match the group Owner';
 END IF;
END//

CREATE TRIGGER tr_member_owner_delete BEFORE DELETE ON group_member
FOR EACH ROW
BEGIN
 IF OLD.role='OWNER' THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Owner membership cannot be removed in W1';
 END IF;
END//
DELIMITER ;
