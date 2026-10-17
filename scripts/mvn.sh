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
mkdir -p .tools/home .tools/docker-client
if [[ ! -f .tools/docker-client/config.json ]]; then printf '{"auths":{}}\n' > .tools/docker-client/config.json; fi
TOOLS_HOME="$ROOT/.tools/home"
DOCKER_CLIENT="$ROOT/.tools/docker-client"
if command -v cygpath >/dev/null 2>&1; then TOOLS_HOME="$(cygpath -m "$TOOLS_HOME")"; DOCKER_CLIENT="$(cygpath -m "$DOCKER_CLIENT")"; fi
export DOCKER_CONFIG="$DOCKER_CLIENT"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Duser.home=\"$TOOLS_HOME\" -Duser.timezone=UTC"
exec mvn -B -ntp -s scripts/maven-settings.xml "-Dmaven.repo.local=${MAVEN_REPO_LOCAL:-$ROOT/.m2-local}" "$@"
