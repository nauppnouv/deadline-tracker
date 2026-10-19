#!/usr/bin/env bash
# Shared schema contract is also packaged in the application by Maven.
schema_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
schema_invariant_label() {
  local row="$1" kind table object rest
  case "$row" in
    CHECKDEF\|*|GENERATEDDEF\|*|TEXTDEF\|*|COLDEF\|*)
      IFS='|' read -r kind table object rest <<< "$row"
      printf '%s.%s [%s]' "$table" "$object" "$row" ;;
    TRIGGERDEF\|*)
      IFS='|' read -r kind object rest <<< "$row"
      printf 'trigger %s [%s]' "$object" "$row" ;;
    *) printf '%s' "$row" ;;
  esac
}
verify_schema() {
  local database="$1" inventory required actual missing=false count
  count="$("${sql[@]}" "$database" -e 'SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND ACTION_STATEMENT IS NOT NULL')"
  [[ "$count" =~ ^[0-9]+$ ]] || { echo 'Invalid trigger inventory count' >&2; return 1; }
  inventory="$("${sql[@]}" "$database" < "$schema_dir/schema-inventory.sql")"
  while IFS= read -r required; do
    [[ -n "$required" && "$required" != \#* ]] || continue
    if ! grep -Fxq -- "$required" <<< "$inventory"; then
      echo "Missing schema invariant: $(schema_invariant_label "$required") (visible triggers=$count)" >&2
      missing=true
    fi
  done < "$schema_dir/schema-required.txt"
  # New CHECK/generated/text/column/trigger definitions must be reviewed, not silently admitted.
  while IFS= read -r actual; do
    case "$actual" in
      CHECKDEF\|*|GENERATEDDEF\|*|TEXTDEF\|*|COLDEF\|*|TRIGGERDEF\|*)
        if ! grep -Fxq -- "$actual" "$schema_dir/schema-required.txt"; then
          echo "Unexpected schema invariant: $(schema_invariant_label "$actual")" >&2
          missing=true
        fi ;;
    esac
  done <<< "$inventory"
  [[ "$missing" == false ]] || return 1
  echo "SCHEMA_OK triggers=$count (V4 required=5); FK/unique/NOT NULL/InnoDB inventory and exact CHECK/generated/text/column/trigger definitions verified"
}
verify_dump_triggers() {
  local dump="$1" definition kind name table timing event hash line tick=$'\x60'
  # Read every byte so pipefail also detects a corrupt gzip stream.
  definition="$(gzip -dc -- "$dump")"
  while IFS='|' read -r kind name table timing event hash; do
    [[ "$kind" == TRIGGERDEF ]] || continue
    line="$(grep -E "TRIGGER[[:space:]]+${tick}?${name}${tick}?[[:space:]]+${timing}[[:space:]]+${event}[[:space:]]+ON[[:space:]]+${tick}?${table}${tick}?" <<< "$definition" || :)"
    [[ "$line" == *CREATE* ]] || { echo "Dump missing CREATE TRIGGER $name $timing $event ON $table" >&2; return 1; }
  done < "$schema_dir/schema-required.txt"
}
