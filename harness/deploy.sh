#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p "${LOG_DIR:-./logs}" ./workspace
chown 1000:1000 ./workspace 2>/dev/null || true
if [ ! -f target/harness-0.1.0.jar ]; then
    echo "==> Building jar..."
    mvn package -DskipTests
fi
echo "==> Building Docker image..."
docker build -t harness:latest .
echo "==> Stopping previous container (if any)..."
docker compose down harness 2>/dev/null || docker rm -f harness 2>/dev/null || true
echo "==> Starting container on port 18000 (localhost only)..."
docker compose up -d
echo "==> Deployed. Endpoint: http://127.0.0.1:18000"
docker logs --tail 5 harness