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
source "$(dirname "$0")/checksum.lib.sh"
# An assignment propagates the SQL exit status; process substitution does not.
tables="$("${sql[@]}" "$DB_NAME" -e 'SHOW TABLES')"
[[ -n "$tables" ]] || { echo 'No tables available for backup' >&2; exit 1; }
file="$(mktemp "$BACKUP_DIR/deadline-$(date -u +%Y%m%dT%H%M%SZ).XXXXXX.sql.gz")"
manifest="$file.tables.pending"
complete=false
cleanup() {
  if [[ "$complete" != true ]]; then
    rm -f -- "$file" "$file.sha256" "$file.tables.tsv" "$manifest"
  fi
}
trap cleanup EXIT
trap 'echo "Backup interrupted; incomplete file is not verified" >&2' ERR
mariadb-dump --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --single-transaction --routines --triggers --hex-blob "$DB_NAME" | gzip > "$file"
# Stop application writes while generating this verification manifest.
while IFS= read -r table; do
  [[ "$table" =~ ^[a-zA-Z0-9_]+$ ]] || exit 1
  count="$("${sql[@]}" "$DB_NAME" -e "SELECT COUNT(*) FROM \`$table\`")"
  checksum="$(table_checksum "$DB_NAME" "$table")"
  [[ "$count" =~ ^[0-9]+$ && "$checksum" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid table verification data' >&2; exit 1; }
  printf '%s\t%s\tsha256:%s\n' "$table" "$count" "$checksum"
done <<< "$tables" > "$manifest"
[[ -s "$manifest" ]] || { echo 'Empty backup manifest' >&2; exit 1; }
mv -- "$manifest" "$file.tables.tsv"
sha256sum "$file" | cut -d ' ' -f 1 > "$file.sha256"
chmod 600 "$file" "$file.sha256" "$file.tables.tsv"
# Only remove our own generated backups, never unrelated files.
backup_names="$(find "$BACKUP_DIR" -maxdepth 1 -type f -regextype posix-extended -regex '.*/deadline-[0-9]{8}T[0-9]{6}Z\.[a-zA-Z0-9]{6}\.sql\.gz' -printf '%T@ %f\n' | sort -rn | cut -d ' ' -f 2-)"
backups=()
while IFS= read -r name; do
  if [[ -n "$name" && -s "$BACKUP_DIR/$name.sha256" && -s "$BACKUP_DIR/$name.tables.tsv" ]]; then backups+=("$name"); fi
done <<< "$backup_names"
complete=true
for ((i=keep; i<${#backups[@]}; i++)); do
  old="$BACKUP_DIR/${backups[$i]}"
  rm -- "$old" "$old.sha256" "$old.tables.tsv"
done
printf 'BACKUP_FILE=%s\n' "$file"
