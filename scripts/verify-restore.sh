#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${DB_HOST:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${DB_NAME:?}" "${RESTORE_DB:?}" "${BACKUP_FILE:?}"
[[ "$DB_NAME" =~ ^[a-zA-Z0-9_]+$ && "$RESTORE_DB" =~ ^deadline_restore_[a-zA-Z0-9_]+$ && "$DB_NAME" != "$RESTORE_DB" ]] || { echo 'Unsafe restore target' >&2; exit 1; }
[[ -f "$BACKUP_FILE" && -f "$BACKUP_FILE.sha256" && -f "$BACKUP_FILE.tables.tsv" ]] || { echo 'Backup components missing' >&2; exit 1; }
source "$(dirname "$0")/verified-backup.lib.sh"
prepare_verified_backup
verified_source_digests="$(backup_set_digests "$BACKUP_FILE")"
expected="$(cat "$BACKUP_FILE.sha256")"
[[ "$expected" =~ ^[a-f0-9]{64}$ && "$(sha256sum "$BACKUP_FILE" | cut -d ' ' -f 1)" == "$expected" ]] || { echo 'Backup checksum mismatch' >&2; exit 1; }
export MYSQL_PWD="$DB_PASSWORD"
sql=(mariadb --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --batch --skip-column-names)
source "$(dirname "$0")/checksum.lib.sh"
source "$(dirname "$0")/schema.lib.sh"
[[ "$("${sql[@]}" -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$RESTORE_DB'")" == 0 ]] || { echo 'Restore database already exists; refusing overwrite' >&2; exit 1; }
"${sql[@]}" -e "CREATE DATABASE \`$RESTORE_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
gzip -dc -- "$BACKUP_FILE" | "${sql[@]}" "$RESTORE_DB"
verify_schema "$RESTORE_DB"
# Exercise Owner invariants in one connection/transaction. The temporary
# procedure handles only the expected SQLSTATE; any other SQL error fails closed.
"${sql[@]}" "$RESTORE_DB" <<'SQL'
DELIMITER //
CREATE PROCEDURE deadline_verify_owner()
BEGIN
 DECLARE uid CHAR(36) DEFAULT UUID();
 DECLARE other_uid CHAR(36) DEFAULT UUID();
 DECLARE gid CHAR(36) DEFAULT UUID();
 DECLARE blocked BOOLEAN DEFAULT FALSE;
 DECLARE owners INT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 INSERT INTO app_user(id,username,display_name,password_hash,created_at)
 VALUES(uid,CONCAT('probe.',LEFT(REPLACE(uid,'-',''),24)),'Restore probe','unusable',UTC_TIMESTAMP(6)),
 (other_uid,CONCAT('probe.',LEFT(REPLACE(other_uid,'-',''),24)),'Restore probe','unusable',UTC_TIMESTAMP(6));
 INSERT INTO project_group(id,name,description,owner_id,created_at)
 VALUES(gid,'Restore probe','',uid,UTC_TIMESTAMP(6));
 SELECT COUNT(*) INTO owners FROM group_member WHERE group_id=gid AND user_id=uid AND role='OWNER';
 IF owners<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Restore probe: group must have Owner'; END IF;
 BEGIN
  DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET blocked=TRUE;
  UPDATE group_member SET role='MEMBER' WHERE group_id=gid AND user_id=uid;
 END;
 IF NOT blocked THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Restore probe: Owner demotion was allowed'; END IF;
 -- Ordinary metadata edits must still work; the whole probe is rolled back.
 UPDATE project_group SET name='Restore probe renamed' WHERE id=gid;
 SET blocked=FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET blocked=TRUE;
  UPDATE project_group SET owner_id=other_uid WHERE id=gid;
 END;
 IF NOT blocked THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Restore probe: Owner replacement was allowed'; END IF;
 SET blocked=FALSE;
 BEGIN
  DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET blocked=TRUE;
  DELETE FROM group_member WHERE group_id=gid AND user_id=uid;
 END;
 IF NOT blocked THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Restore probe: Owner deletion was allowed'; END IF;
 ROLLBACK;
END//
DELIMITER ;
CALL deadline_verify_owner();
DROP PROCEDURE deadline_verify_owner;
SQL
echo 'OWNER_BEHAVIOR_OK (create Owner; demotion/replacement/deletion blocked; metadata editable; transaction rolled back)'
actual_tables="$("${sql[@]}" "$RESTORE_DB" -e 'SHOW TABLES' | sort)"
expected_tables="$(cut -f 1 "$BACKUP_FILE.tables.tsv" | sort)"
[[ -n "$actual_tables" && "$actual_tables" == "$expected_tables" ]] || { echo 'Table inventory mismatch' >&2; exit 1; }
while IFS=$'\t' read -r table expected_count expected_checksum; do
  [[ "$table" =~ ^[a-zA-Z0-9_]+$ && "$expected_count" =~ ^[0-9]+$ ]] || exit 1
  count="$("${sql[@]}" "$RESTORE_DB" -e "SELECT COUNT(*) FROM \`$table\`")"
  if [[ "$expected_checksum" =~ ^sha256:[a-f0-9]{64}$ ]]; then
    checksum="sha256:$(table_checksum "$RESTORE_DB" "$table")"
  elif [[ "$expected_checksum" =~ ^[0-9]+$ ]]; then
    # Existing backup manifests still use the original MariaDB checksum.
    checksum="$("${sql[@]}" "$RESTORE_DB" -e "CHECKSUM TABLE \`$table\` EXTENDED" | cut -f 2)"
  else
    echo 'Invalid checksum format' >&2; exit 1
  fi
  [[ "$count" == "$expected_count" && "$checksum" == "$expected_checksum" ]] || { echo "Mismatch: $table" >&2; exit 1; }
  printf 'TABLE_OK %s rows=%s checksum=%s\n' "$table" "$count" "$checksum"
done < "$BACKUP_FILE.tables.tsv"
# Revalidate every foreign key, including framework sessions, after dump import.
current_key=''
verify_fk() {
  [[ -n "$current_key" ]] || return 0
  orphans="$("${sql[@]}" "$RESTORE_DB" -e "SELECT COUNT(*) FROM \`$fk_table\` c LEFT JOIN \`$fk_parent\` p ON $join_condition WHERE $nonnull_condition AND p.\`$first_parent_column\` IS NULL")"
  [[ "$orphans" == 0 ]] || { echo "Foreign key violation: $current_key" >&2; exit 1; }
  printf 'FK_OK %s\n' "$current_key"
}
foreign_keys="$("${sql[@]}" -e "SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='$RESTORE_DB' AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION")"
while IFS=$'\t' read -r table constraint column parent parent_column; do
  [[ -n "$table" ]] || continue
  for identifier in "$table" "$constraint" "$column" "$parent" "$parent_column"; do [[ "$identifier" =~ ^[a-zA-Z0-9_]+$ ]] || exit 1; done
  key="$table.$constraint"
  if [[ "$key" != "$current_key" ]]; then
    verify_fk
    current_key="$key"; fk_table="$table"; fk_parent="$parent"; first_parent_column="$parent_column"
    join_condition="c.\`$column\`=p.\`$parent_column\`"; nonnull_condition="c.\`$column\` IS NOT NULL"
  else
    join_condition+=" AND c.\`$column\`=p.\`$parent_column\`"; nonnull_condition+=" AND c.\`$column\` IS NOT NULL"
  fi
done <<< "$foreign_keys"
verify_fk
# Snapshot sessions predate later logouts/password changes. Never reuse them.
"${sql[@]}" "$RESTORE_DB" -e 'DELETE FROM SPRING_SESSION'
remaining_sessions="$("${sql[@]}" "$RESTORE_DB" -e 'SELECT COUNT(*) FROM SPRING_SESSION')"
remaining_attributes="$("${sql[@]}" "$RESTORE_DB" -e 'SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES')"
[[ "$remaining_sessions" == 0 && "$remaining_attributes" == 0 ]] || { echo 'Restored sessions were not cleared' >&2; exit 1; }
echo 'SESSIONS_CLEARED (after snapshot comparison)'
protect_verified_backup
echo 'BẮT BUỘC: đặt lại mật khẩu từng tài khoản bằng ops reset-password trước khi mở lại web'
echo 'Verify identity first; reset every account allowed to sign in; keep disabled accounts disabled. Verification does not authorize reopening web.'
echo 'RESTORE_OK (keep isolated until post-snapshot account security review)'
