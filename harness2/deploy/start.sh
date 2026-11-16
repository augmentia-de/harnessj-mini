#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p "${LOG_DIR:-./logs}"
set -a; . ./.env 2>/dev/null || true; set +a
MODE="${1:-web}"
TASK="${*:2}"
case "$MODE" in
    web)  export QUARKUS_HTTP_PORT="${QUARKUS_HTTP_PORT:-18004}"; exec mvn quarkus:dev ;;
    run)  exec ./target/harness2-0.1.0-runner ;;
    *)    echo "Usage: $0 {web|run} [task]"; exit 1 ;;
esac