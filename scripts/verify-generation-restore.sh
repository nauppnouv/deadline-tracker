#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${DB_HOST:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${DB_NAME:?}" "${RESTORE_DB:?}" "${GENERATION_DIR:?}" "${RESTORE_ATTACHMENT_ROOT:?}"
[[ "$DB_NAME" =~ ^[a-zA-Z0-9_]+$ && "$RESTORE_DB" =~ ^deadline_restore_[a-zA-Z0-9_]+$ && "$DB_NAME" != "$RESTORE_DB" ]] || { echo 'Unsafe restore database' >&2; exit 1; }
repo="$(cd "$(dirname "$0")/.." && pwd -P)"
generation="$(cd "$GENERATION_DIR" && pwd -P)"
[[ ! -L "$GENERATION_DIR" && ! -L "$generation" && ! -L "$generation/database" && ! -L "$generation/attachments" && ! -L "$generation/generation.manifest" && ! -L "$generation/attachments.tsv" ]] || { echo 'Generation components cannot be symlinks' >&2; exit 1; }
target_parent="$(dirname "$RESTORE_ATTACHMENT_ROOT")"
mkdir -p -- "$target_parent"
target_parent="$(cd "$target_parent" && pwd -P)"
RESTORE_ATTACHMENT_ROOT="$target_parent/$(basename "$RESTORE_ATTACHMENT_ROOT")"
case "$generation/" in "$repo/"*) echo 'Generation must be outside repository' >&2; exit 1;; esac
case "$RESTORE_ATTACHMENT_ROOT/" in "$repo/"*) echo 'Restore files must be outside repository' >&2; exit 1;; esac
[[ ! -e "$RESTORE_ATTACHMENT_ROOT" && ! -L "$RESTORE_ATTACHMENT_ROOT" ]] || { echo 'Restore attachment target already exists; refusing overwrite' >&2; exit 1; }
[[ -f "$generation/generation.manifest" && -f "$generation/attachments.tsv" ]] || { echo 'Generation manifest missing' >&2; exit 1; }
[[ "$(head -n 1 "$generation/generation.manifest")" == W11_GENERATION_V1 ]] || { echo 'Unsupported generation format' >&2; exit 1; }
db_file="$(awk -F '\t' '$1=="DATABASE_FILE"{print $2}' "$generation/generation.manifest")"
db_hash="$(awk -F '\t' '$1=="DATABASE_SHA256"{print $2}' "$generation/generation.manifest")"
tables_hash="$(awk -F '\t' '$1=="DATABASE_TABLES_SHA256"{print $2}' "$generation/generation.manifest")"
files_hash="$(awk -F '\t' '$1=="ATTACHMENTS_MANIFEST_SHA256"{print $2}' "$generation/generation.manifest")"
expected_count="$(awk -F '\t' '$1=="ATTACHMENT_COUNT"{print $2}' "$generation/generation.manifest")"
[[ "$db_file" =~ ^deadline-[A-Za-z0-9T.Z-]+\.sql\.gz$ && "$db_hash" =~ ^[a-f0-9]{64}$ && "$tables_hash" =~ ^[a-f0-9]{64}$ && "$files_hash" =~ ^[a-f0-9]{64}$ && "$expected_count" =~ ^[0-9]+$ ]] || { echo 'Invalid generation manifest fields' >&2; exit 1; }
database="$generation/database"
[[ -f "$database/$db_file" && ! -L "$database/$db_file" && -f "$database/$db_file.sha256" && ! -L "$database/$db_file.sha256" && -f "$database/$db_file.tables.tsv" && ! -L "$database/$db_file.tables.tsv" ]] || { echo 'Database generation files missing or symlinked' >&2; exit 1; }
[[ "$(cat "$database/$db_file.sha256")" == "$db_hash" && "$(sha256sum "$database/$db_file.tables.tsv" | cut -d ' ' -f 1)" == "$tables_hash" && "$(sha256sum "$generation/attachments.tsv" | cut -d ' ' -f 1)" == "$files_hash" ]] || { echo 'Generation component checksum mismatch' >&2; exit 1; }
[[ "$(wc -l < "$generation/attachments.tsv" | tr -d ' ')" == "$expected_count" ]] || { echo 'Attachment manifest count mismatch' >&2; exit 1; }
expected_blobs="$(cut -f1 "$generation/attachments.tsv" | sed '/^$/d;s/$/.blob/' | sort)"
actual_blobs="$(find "$generation/attachments" -mindepth 1 -maxdepth 1 -printf '%f\n' | sort)"
[[ "$actual_blobs" == "$expected_blobs" ]] || { echo 'Generation has extra or missing attachment files' >&2; exit 1; }
temp="$(mktemp -d "${TMPDIR:-/tmp}/w11-restore-XXXXXX")"
stage="$target_parent/.restore-attachments-$(basename "$temp")"
published=false
cleanup(){ rm -rf -- "$temp"; if [[ "$published" != true ]]; then rm -rf -- "$stage"; fi; }
trap cleanup EXIT
mkdir -m 700 -- "$stage"
mkdir -m 700 -- "$temp/db"
cp -- "$database/$db_file" "$database/$db_file.sha256" "$database/$db_file.tables.tsv" "$temp/db/"
while IFS=$'\t' read -r key size digest; do
  [[ "$key" =~ ^[0-9a-f-]{36}$ && "$size" =~ ^[0-9]+$ && "$digest" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid attachment manifest row' >&2; exit 1; }
  blob="$generation/attachments/$key.blob"
  [[ -f "$blob" && ! -L "$blob" && "$(stat -c '%s' "$blob")" == "$size" && "$(sha256sum "$blob" | cut -d ' ' -f 1)" == "$digest" ]] || { echo "Attachment generation mismatch: $key" >&2; exit 1; }
  cp --update=none -- "$blob" "$stage/$key.blob"
  chmod 600 "$stage/$key.blob"
done < "$generation/attachments.tsv"
export BACKUP_FILE="$temp/db/$db_file"
if ! bash "$repo/scripts/verify-restore.sh" > "$temp/verify.log" 2>&1; then cat "$temp/verify.log" >&2; exit 1; fi
export MYSQL_PWD="$DB_PASSWORD"
sql=(mariadb --host="$DB_HOST" --port="${DB_PORT:-3306}" --user="$DB_USER" --batch --skip-column-names)
actual_rows="$("${sql[@]}" "$RESTORE_DB" -e 'SELECT COUNT(*) FROM task_attachment')"
[[ "$actual_rows" == "$expected_count" ]] || { echo 'Restored attachment row count differs from paired manifest' >&2; exit 1; }
declare -A attachment_keys=()
while IFS=$'\t' read -r key size digest; do
  [[ -n "$key" ]] || continue
  [[ ! ${attachment_keys[$key]+present} && "$key" =~ ^[0-9a-f-]{36}$ ]] || { echo 'Duplicate or invalid attachment key in manifest' >&2; exit 1; }
  attachment_keys[$key]=1
  restored="$("${sql[@]}" "$RESTORE_DB" -e "SELECT CONCAT(size_bytes,'|',sha256,'|',lifecycle) FROM task_attachment WHERE storage_key='$key'")"
  [[ "$restored" == "$size|$digest|READY" ]] || { echo "Restored attachment metadata mismatch: $key" >&2; exit 1; }
done < "$generation/attachments.tsv"
[[ "${#attachment_keys[@]}" == "$actual_rows" ]] || { echo 'Restored attachment metadata set differs from paired manifest' >&2; exit 1; }
mv -- "$stage" "$RESTORE_ATTACHMENT_ROOT"
published=true
sed -e '/^VERIFIED_BACKUP_FILE=/d' -e '/^RESTORE_OK (/d' "$temp/verify.log"
printf 'VERIFIED_GENERATION=%s\n' "$generation"
echo 'ATTACHMENT_FILES_OK (DB and files verified; keep the restored database isolated until operational review)'
echo 'RESTORE_OK_PAIR (database and attachment blobs match this generation; reset account credentials and review before opening web)'
printf 'RESTORE_ATTACHMENT_ROOT=%s\n' "$RESTORE_ATTACHMENT_ROOT"
