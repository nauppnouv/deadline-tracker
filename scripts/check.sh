#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo 'CHECK build + tests'
bash scripts/mvn.sh verify "$@"
echo 'OK build + tests'
if grep -RInE 'th:utext|["\x27][[:space:]]*(SELECT|INSERT|UPDATE|DELETE)[[:space:]].*["\x27][[:space:]]*\+' src/main 2>/dev/null; then
  echo 'FAIL forbidden template or SQL concatenation'; exit 1
fi
echo 'OK template / SQL static checks'
if command -v gitleaks >/dev/null 2>&1; then
  gitleaks dir --no-banner --redact --config .gitleaks.toml .
else
  if grep -RIlE 'AKIA[A-Z0-9]{16}|gh[pousr]_[A-Za-z0-9]{30,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY' src docs scripts 2>/dev/null; then
    echo 'FAIL possible secret'; exit 1
  fi
  echo 'BỎ QUA gitleaks: tool missing; fallback credential pattern scan passed'
fi
echo 'OK secret scan'
if [[ "${RUN_DEPENDENCY_AUDIT:-0}" == 1 ]]; then
  bash scripts/mvn.sh -Paudit org.owasp:dependency-check-maven:check
  echo 'OK dependency audit'
else
  echo 'BỎ QUA dependency audit: enable RUN_DEPENDENCY_AUDIT=1; not a passing SEC-13 result'
fi
tracked_migrations="$(git diff --name-only --diff-filter=MD HEAD -- src/main/resources/db/migration 2>/dev/null || true)"
if [[ -n "$tracked_migrations" ]]; then
  echo "FAIL committed migration changed: $tracked_migrations"; exit 1
fi
echo 'OK immutable committed migrations'
