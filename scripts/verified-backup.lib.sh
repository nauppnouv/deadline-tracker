#!/usr/bin/env bash
# A full restore protects its actual input bytes separately from candidates.
# Receipts assume the private backup directory and scripts remain trusted.
backup_set_digests() {
  local path value
  for path in "$1" "$1.sha256" "$1.tables.tsv"; do
    [[ -f "$path" && ! -L "$path" ]] || { echo 'Backup set must contain three regular files' >&2; return 1; }
    value="$(sha256sum -- "$path")" || return 1
    value="${value%% *}"
    [[ "$value" =~ ^[a-f0-9]{64}$ ]] || return 1
    printf '%s\n' "$value"
  done
}

current_verified_backup() {
  local store="$1" name file recorded actual digest
  local -a receipt
  [[ -d "$store" && ! -L "$store" && -f "$store/current" && ! -L "$store/current" ]] || { echo 'Invalid verified backup store or pointer' >&2; return 1; }
  name="$(cat -- "$store/current")" || return 1
  [[ "$name" =~ ^verified-[a-zA-Z0-9]{6}$ && -d "$store/$name" && ! -L "$store/$name" ]] || { echo 'Invalid verified backup generation' >&2; return 1; }
  file="$store/$name/backup.sql.gz"
  [[ -f "$store/$name/verification.txt" && ! -L "$store/$name/verification.txt" ]] || { echo 'Verified backup receipt missing' >&2; return 1; }
  mapfile -t receipt < "$store/$name/verification.txt" || return 1
  [[ "${#receipt[@]}" == 5 && "${receipt[0]}" == deadline-full-restore-v1 ]] || { echo 'Invalid verified backup receipt' >&2; return 1; }
  for digest in "${receipt[@]:1:3}"; do
    [[ "$digest" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid verified backup digest' >&2; return 1; }
  done
  recorded="$(printf '%s\n' "${receipt[@]:1:3}")"
  actual="$(backup_set_digests "$file")" || return 1
  [[ "$recorded" == "$actual" ]] || { echo 'Verified backup bytes no longer match full-restore receipt' >&2; return 1; }
  printf '%s\n' "$file"
}

prepare_verified_backup() {
  local parent name repo
  parent="$(cd -- "$(dirname -- "$BACKUP_FILE")" && pwd -P)"
  name="$(basename -- "$BACKUP_FILE")"
  BACKUP_FILE="$parent/$name"
  backup_set_dir="$parent"
  # Rechecking a protected copy must rotate within the same store, not nest it.
  if [[ "$name" == backup.sql.gz && "${parent##*/}" =~ ^verified-[a-zA-Z0-9]{6}$ && "$(basename -- "$(dirname -- "$parent")")" == .deadline-verified ]]; then
    backup_set_dir="$(dirname -- "$(dirname -- "$parent")")"
  fi
  repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
  case "$backup_set_dir/" in "$repo/"*) echo 'Verified backups must be outside repository' >&2; return 1;; esac
  verified_store="$backup_set_dir/.deadline-verified"
  [[ ! -L "$verified_store" ]] || { echo 'Verified backup store cannot be a symlink' >&2; return 1; }
  verified_lock="$backup_set_dir/.deadline-backup.lock"
  mkdir -- "$verified_lock" || { echo 'Backup or restore already running or stale lock present' >&2; return 1; }
  verified_pending=''
  verified_unpublished=''
  verified_pointer_temp=''
  trap cleanup_verified_backup EXIT
}

cleanup_verified_backup() {
  local code=$? directory
  trap - EXIT
  # These are only exact paths allocated by this invocation; never recurse.
  for directory in "${verified_pending:-}" "${verified_unpublished:-}"; do
    [[ -n "$directory" ]] || continue
    rm -f -- "$directory/backup.sql.gz" "$directory/backup.sql.gz.sha256" "$directory/backup.sql.gz.tables.tsv" "$directory/verification.txt" || code=1
    rmdir -- "$directory" || code=1
  done
  if [[ -n "${verified_pointer_temp:-}" ]]; then rm -f -- "$verified_pointer_temp" || code=1; fi
  rmdir -- "$verified_lock" || code=1
  exit "$code"
}

protect_verified_backup() {
  local previous='' generation target old
  if [[ -e "$verified_store/current" || -L "$verified_store/current" ]]; then
    old="$(current_verified_backup "$verified_store")" || return 1
    previous="$(dirname -- "$old")"
  fi
  mkdir -p -- "$verified_store"
  chmod 700 "$verified_store"
  verified_pending="$(mktemp -d "$verified_store/.pending-XXXXXX")"
  cp -- "$BACKUP_FILE" "$verified_pending/backup.sql.gz"
  cp -- "$BACKUP_FILE.sha256" "$verified_pending/backup.sql.gz.sha256"
  cp -- "$BACKUP_FILE.tables.tsv" "$verified_pending/backup.sql.gz.tables.tsv"
  [[ "$verified_source_digests" == "$(backup_set_digests "$BACKUP_FILE")" && "$verified_source_digests" == "$(backup_set_digests "$verified_pending/backup.sql.gz")" ]] || { echo 'Backup components changed during full restore verification' >&2; return 1; }
  printf 'deadline-full-restore-v1\n%s\n%s\n' "$verified_source_digests" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$verified_pending/verification.txt"
  chmod 600 "$verified_pending/backup.sql.gz" "$verified_pending/backup.sql.gz.sha256" "$verified_pending/backup.sql.gz.tables.tsv" "$verified_pending/verification.txt"
  generation="verified-${verified_pending##*.pending-}"
  target="$verified_store/$generation"
  [[ ! -e "$target" && ! -L "$target" ]] || { echo 'Verified backup generation already exists' >&2; return 1; }
  mv -- "$verified_pending" "$target"
  verified_pending=''
  verified_unpublished="$target"
  verified_pointer_temp="$(mktemp "$verified_store/.current-XXXXXX")"
  printf '%s\n' "$generation" > "$verified_pointer_temp"
  mv -T -- "$verified_pointer_temp" "$verified_store/current"
  verified_pointer_temp=''
  verified_unpublished=''
  # The new complete directory and atomic pointer exist before retiring the old.
  if [[ -n "$previous" ]]; then
    rm -- "$previous/backup.sql.gz" "$previous/backup.sql.gz.sha256" "$previous/backup.sql.gz.tables.tsv" "$previous/verification.txt"
    rmdir -- "$previous"
  fi
  printf 'VERIFIED_BACKUP_FILE=%s\n' "$target/backup.sql.gz"
}
