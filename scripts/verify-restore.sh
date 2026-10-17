#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${DB_HOST:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${DB_NAME:?}" "${RESTORE_DB:?}" "${BACKUP_FILE:?}"
[[ "$DB_NAME" =~ ^[a-zA-Z0-9_]+$ && "$RESTORE_DB" =~ ^deadline_restore_[a-zA-Z0-9_]+$ && "$DB_NAME" != "$RESTORE_DB" ]] || { echo 'Unsafe restore target' >&2; exit 1; }
[[ -f "$BACKUP_FILE" && -f "$BACKUP_FILE.sha256" && -f "$BACKUP_FILE.tables.tsv" ]] || { echo 'Backup components missing' >&2; exit 1; }
expected="$(cat "$BACKUP_FILE.sha256")"
[[ "$expected" =~ ^[a-f0-9]{64}$ && "$(sha256sum "$BACKUP_FILE" | cut -d ' ' -f 1)" == "$expected" ]] || { echo 'Backup checksum mismatch' >&2; exit 1; }
export MYSQL_PWD="$DB_PASSWORD"
sql=(mariadb --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --batch --skip-column-names)
[[ "$("${sql[@]}" -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$RESTORE_DB'")" == 0 ]] || { echo 'Restore database already exists; refusing overwrite' >&2; exit 1; }
"${sql[@]}" -e "CREATE DATABASE \`$RESTORE_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
gzip -dc -- "$BACKUP_FILE" | "${sql[@]}" "$RESTORE_DB"
[[ "$("${sql[@]}" "$RESTORE_DB" -e 'SHOW TABLES' | sort)" == "$(cut -f 1 "$BACKUP_FILE.tables.tsv" | sort)" ]] || { echo 'Table inventory mismatch' >&2; exit 1; }
while IFS=$'\t' read -r table expected_count expected_checksum; do
  [[ "$table" =~ ^[a-zA-Z0-9_]+$ && "$expected_count" =~ ^[0-9]+$ && "$expected_checksum" =~ ^[0-9]+$ ]] || exit 1
  count="$("${sql[@]}" "$RESTORE_DB" -e "SELECT COUNT(*) FROM \`$table\`")"
  checksum="$("${sql[@]}" "$RESTORE_DB" -e "CHECKSUM TABLE \`$table\` EXTENDED" | cut -f 2)"
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
while IFS=$'\t' read -r table constraint column parent parent_column; do
  for identifier in "$table" "$constraint" "$column" "$parent" "$parent_column"; do [[ "$identifier" =~ ^[a-zA-Z0-9_]+$ ]] || exit 1; done
  key="$table.$constraint"
  if [[ "$key" != "$current_key" ]]; then
    verify_fk
    current_key="$key"; fk_table="$table"; fk_parent="$parent"; first_parent_column="$parent_column"
    join_condition="c.\`$column\`=p.\`$parent_column\`"; nonnull_condition="c.\`$column\` IS NOT NULL"
  else
    join_condition+=" AND c.\`$column\`=p.\`$parent_column\`"; nonnull_condition+=" AND c.\`$column\` IS NOT NULL"
  fi
done < <("${sql[@]}" -e "SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='$RESTORE_DB' AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION")
verify_fk
echo 'RESTORE_OK (trial database retained for inspection)'
