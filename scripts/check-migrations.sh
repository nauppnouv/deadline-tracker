#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
baseline="${MIGRATION_BASELINE_REF-origin/main}"
if [[ "$baseline" == none ]]; then
  echo 'BỎ QUA: migration immutability (MIGRATION_BASELINE_REF=none explicitly set)'
  exit 0
fi
if ! git rev-parse --verify "${baseline}^{commit}" >/dev/null; then
  echo "FAIL migration baseline ref unavailable: $baseline" >&2; exit 1
fi
if ! changes="$(git diff --no-ext-diff --no-textconv --name-status --find-renames "$baseline" -- src/main/resources/db/migration)"; then
  echo "FAIL git diff against migration baseline: $baseline" >&2; exit 1
fi
invalid=false
while IFS=$'\t' read -r status old_path new_path; do
  case "$status" in
    M*|D*|R*|T*) echo "FAIL baseline migration changed ($status): $old_path ${new_path:-}" >&2; invalid=true;;
  esac
done <<< "$changes"
[[ "$invalid" == false ]] || exit 1
echo "OK immutable committed migrations (baseline=$baseline)"
