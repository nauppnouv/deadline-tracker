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
invalid=false
# The next commit takes bytes from the index, which can differ from the files
# Maven reads. Neither view may hide a change to a baseline migration.
for scope in worktree index; do
  diff_args=(--no-ext-diff --no-textconv --name-status --find-renames)
  if [[ "$scope" == index ]]; then diff_args+=(--cached); fi
  if ! changes="$(git diff "${diff_args[@]}" "$baseline" -- src/main/resources/db/migration)"; then
    echo "FAIL git diff ($scope) against migration baseline: $baseline" >&2; exit 1
  fi
  while IFS=$'\t' read -r status old_path new_path; do
    case "$status" in
      M*|D*|R*|T*) echo "FAIL baseline migration changed ($scope, $status): $old_path ${new_path:-}" >&2; invalid=true;;
    esac
  done <<< "$changes"
done
[[ "$invalid" == false ]] || exit 1
echo "OK immutable committed migrations (baseline=$baseline; worktree and index)"
