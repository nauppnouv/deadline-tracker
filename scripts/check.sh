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
if [[ "${RUN_DEPENDENCY_AUDIT:-1}" == 1 ]]; then
  audit_args=()
  if [[ -n "${DEPENDENCY_CHECK_DATA:-}" ]]; then audit_args+=("-Ddependency.check.dataDirectory=$DEPENDENCY_CHECK_DATA"); fi
  bash scripts/mvn.sh -Paudit "${audit_args[@]}" org.owasp:dependency-check-maven:check
  echo 'OK dependency audit'
else
  echo "BỎ QUA dependency audit: ${AUDIT_SKIP_REASON:-explicit opt-out, reason not provided}; not a passing SEC-13 result"
fi
bash scripts/check-migrations.sh
