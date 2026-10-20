#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${DB_HOST:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${DB_NAME:?}" "${BACKUP_DIR:?}" "${ATTACHMENT_ROOT:?}"
[[ "${BACKUP_WRITES_PAUSED:-}" == 1 ]] || { echo 'Set BACKUP_WRITES_PAUSED=1 only after stopping every application writer' >&2; exit 1; }
[[ "$DB_NAME" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'Invalid database name' >&2; exit 1; }
repo="$(cd "$(dirname "$0")/.." && pwd -P)"
mkdir -p -- "$BACKUP_DIR" "$ATTACHMENT_ROOT"
[[ ! -L "$ATTACHMENT_ROOT" ]] || { echo 'Attachment root cannot be a symlink' >&2; exit 1; }
BACKUP_DIR="$(cd "$BACKUP_DIR" && pwd -P)"
ATTACHMENT_ROOT="$(cd "$ATTACHMENT_ROOT" && pwd -P)"
case "$BACKUP_DIR/" in "$repo/"*) echo 'Backups must be outside repository' >&2; exit 1;; esac
case "$ATTACHMENT_ROOT/" in "$repo/"*) echo 'Attachment storage must be outside repository' >&2; exit 1;; esac
chmod 700 "$BACKUP_DIR" "$ATTACHMENT_ROOT"
store="$BACKUP_DIR/generations"
mkdir -p -- "$store"
chmod 700 "$store"
lock="$BACKUP_DIR/.w11-generation.lock"
mkdir -- "$lock" 2>/dev/null || { echo 'Generation backup already running or stale lock present' >&2; exit 1; }
stage="$(mktemp -d "$store/.pending-XXXXXX")"
published=false
cleanup(){
  if [[ "$published" != true ]]; then rm -rf -- "$stage"; fi
  rmdir -- "$lock"
}
trap cleanup EXIT
mkdir -p -- "$stage/database" "$stage/attachments"
chmod 700 "$stage/database" "$stage/attachments"
export BACKUP_DIR="$stage/database" BACKUP_KEEP=1
bash "$repo/scripts/backup.sh" > "$stage/backup-output.log"
db_file="$(sed -n 's/^BACKUP_FILE=//p' "$stage/backup-output.log" | tail -n 1)"
[[ -n "$db_file" && "$db_file" == "$stage/database/"* && -s "$db_file" ]] || { echo 'Database backup did not produce a verified candidate' >&2; exit 1; }
rm -f -- "$stage/backup-output.log"
export MYSQL_PWD="$DB_PASSWORD"
sql=(mariadb --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --batch --skip-column-names)
rows="$("${sql[@]}" "$DB_NAME" -e 'SELECT storage_key,size_bytes,sha256,lifecycle FROM task_attachment ORDER BY storage_key')"
files_manifest="$stage/attachments.tsv"
: > "$files_manifest"
while IFS=$'\t' read -r key size digest lifecycle; do
  [[ -n "$key" ]] || continue
  [[ "$key" =~ ^[0-9a-f-]{36}$ && "$size" =~ ^[0-9]+$ && "$digest" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid attachment metadata' >&2; exit 1; }
  [[ "$lifecycle" == READY ]] || { echo 'Reconcile attachments and require every referenced file to be READY before generation backup' >&2; exit 1; }
  source_file="$ATTACHMENT_ROOT/$key.blob"
  [[ -f "$source_file" && ! -L "$source_file" ]] || { echo "Referenced attachment missing: $key" >&2; exit 1; }
  [[ "$(stat -c '%s' "$source_file")" == "$size" && "$(sha256sum "$source_file" | cut -d ' ' -f 1)" == "$digest" ]] || { echo "Referenced attachment hash/size mismatch: $key" >&2; exit 1; }
  cp --no-clobber -- "$source_file" "$stage/attachments/$key.blob"
  chmod 600 "$stage/attachments/$key.blob"
  printf '%s\t%s\t%s\n' "$key" "$size" "$digest" >> "$files_manifest"
done <<< "$rows"
chmod 600 "$files_manifest"
# Writers must already be paused. Refuse orphan/temporary/extra bytes instead of silently omitting them.
expected_blobs="$(cut -f1 "$files_manifest" | sed '/^$/d;s/$/.blob/' | sort)"
actual_blobs="$(find "$ATTACHMENT_ROOT" -mindepth 1 -maxdepth 1 -printf '%f\n' | sort)"
[[ "$actual_blobs" == "$expected_blobs" ]] || { echo 'Attachment root has unpaired or temporary files; reconcile before backup' >&2; exit 1; }
db_name="${db_file##*/}"
{
  printf 'W11_GENERATION_V1\n'
  printf 'DATABASE_FILE\t%s\n' "$db_name"
  printf 'DATABASE_SHA256\t%s\n' "$(cat "$db_file.sha256")"
  printf 'DATABASE_TABLES_SHA256\t%s\n' "$(sha256sum "$db_file.tables.tsv" | cut -d ' ' -f 1)"
  printf 'ATTACHMENT_COUNT\t%s\n' "$(wc -l < "$files_manifest" | tr -d ' ')"
  printf 'ATTACHMENTS_MANIFEST_SHA256\t%s\n' "$(sha256sum "$files_manifest" | cut -d ' ' -f 1)"
} > "$stage/generation.manifest"
chmod 600 "$stage/generation.manifest"
generation="generation-$(date -u +%Y%m%dT%H%M%SZ)-$(basename "$stage" | cut -d- -f2)"
final="$store/$generation"
mv -- "$stage" "$final"
published=true
pointer_tmp="$store/.current-$(basename "$final").tmp"
printf '%s\n' "$generation" > "$pointer_tmp"
chmod 600 "$pointer_tmp"
mv -f -- "$pointer_tmp" "$store/current"
echo 'GENERATION_BACKUP_READY (restore verification required before use)'
printf 'GENERATION_DIR=%s\n' "$final"
