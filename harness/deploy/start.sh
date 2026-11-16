#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p "${LOG_DIR:-./logs}"
set -a; . ./.env 2>/dev/null || true; set +a
MODE="${1:-web}"
TASK="${*:2}"
JAR="target/harness-0.1.0.jar"
if [ ! -f "$JAR" ]; then
    echo "Building..."
    mvn -q package -DskipTests
fi
case "$MODE" in
    tui)  exec java -jar "$JAR" --tui "$TASK" ;;
    web)  exec mvn spring-boot:run ;;
    run)  exec java -jar "$JAR" "$TASK" ;;
    *)    echo "Usage: $0 {tui|web|run} [task]"; exit 1 ;;
esac