#!/usr/bin/env bash
# Sourced by backup/restore with set -euo pipefail and the sql client array.
# Hash logical values, including virtual columns, independently of record layout.
table_checksum() {
  local database="$1" table="$2" columns column expression='' separator=''
  [[ "$database" =~ ^[a-zA-Z0-9_]+$ && "$table" =~ ^[a-zA-Z0-9_]+$ ]] || return 1
  columns="$("${sql[@]}" -e "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$database' AND TABLE_NAME='$table' ORDER BY ORDINAL_POSITION")" || return 1
  [[ -n "$columns" ]] || return 1
  while IFS= read -r column; do
    [[ "$column" =~ ^[a-zA-Z0-9_]+$ ]] || return 1
    expression+="$separator IF(\`$column\` IS NULL,'N',CONCAT('V',HEX(\`$column\`)))"
    separator=",'|',"
  done <<< "$columns"
  {
    printf '%s\n' "$columns"
    "${sql[@]}" "$database" -e "SELECT SHA2(CONCAT($expression),256) AS row_hash FROM \`$table\` ORDER BY row_hash"
  } | sha256sum | cut -d ' ' -f 1
}
