#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -d "$ROOT/.tools/java25" ]]; then
  for candidate in "$ROOT"/.tools/java25/*; do
    if [[ -f "$candidate/bin/java.exe" || -f "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      break
    fi
  done
fi
cd "$ROOT"
exec mvn -B -ntp "-Dmaven.repo.local=$ROOT/.m2-local" "$@"
