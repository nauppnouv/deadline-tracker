#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${DB_HOST:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${DB_NAME:?}" "${BACKUP_DIR:?}"
[[ "$DB_NAME" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'Invalid database name' >&2; exit 1; }
keep="${BACKUP_KEEP:-7}"
[[ "$keep" =~ ^[0-9]+$ ]] && ((keep >= 1 && keep <= 1000)) || { echo 'Invalid retention' >&2; exit 1; }
mkdir -p -- "$BACKUP_DIR"
BACKUP_DIR="$(cd "$BACKUP_DIR" && pwd -P)"
repo="$(cd "$(dirname "$0")/.." && pwd -P)"
case "$BACKUP_DIR/" in "$repo/"*) echo 'Backups must be outside repository' >&2; exit 1;; esac
chmod 700 "$BACKUP_DIR"
export MYSQL_PWD="$DB_PASSWORD"
sql=(mariadb --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --batch --skip-column-names)
file="$(mktemp "$BACKUP_DIR/deadline-$(date -u +%Y%m%dT%H%M%SZ).XXXXXX.sql.gz")"
trap 'echo "Backup interrupted; incomplete file is not verified" >&2' ERR
mariadb-dump --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --single-transaction --routines --triggers --hex-blob "$DB_NAME" | gzip > "$file"
sha256sum "$file" | cut -d ' ' -f 1 > "$file.sha256"
# Stop application writes while generating this verification manifest.
while IFS= read -r table; do
  [[ "$table" =~ ^[a-zA-Z0-9_]+$ ]] || exit 1
  count="$("${sql[@]}" "$DB_NAME" -e "SELECT COUNT(*) FROM \`$table\`")"
  checksum="$("${sql[@]}" "$DB_NAME" -e "CHECKSUM TABLE \`$table\` EXTENDED" | cut -f 2)"
  printf '%s\t%s\t%s\n' "$table" "$count" "$checksum"
done < <("${sql[@]}" "$DB_NAME" -e 'SHOW TABLES') > "$file.tables.tsv"
chmod 600 "$file" "$file.sha256" "$file.tables.tsv"
# Only remove our own generated backups, never unrelated files.
mapfile -t backups < <(find "$BACKUP_DIR" -maxdepth 1 -type f -regextype posix-extended -regex '.*/deadline-[0-9]{8}T[0-9]{6}Z\.[a-zA-Z0-9]{6}\.sql\.gz' -printf '%T@ %f\n' | sort -rn | cut -d ' ' -f 2- | while IFS= read -r name; do
  if [[ -f "$BACKUP_DIR/$name.sha256" && -f "$BACKUP_DIR/$name.tables.tsv" ]]; then printf '%s\n' "$name"; fi
done)
for ((i=keep; i<${#backups[@]}; i++)); do
  old="$BACKUP_DIR/${backups[$i]}"
  rm -- "$old" "$old.sha256" "$old.tables.tsv"
done
printf 'BACKUP_FILE=%s\n' "$file"
