#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p "${LOG_DIR:-./logs}" ./data/home ./workspace
if ! ls target/*-runner >/dev/null 2>&1; then
    echo "==> Building native executable..."
    export JAVA_HOME=/tmp/opencode/mandrel/mandrel
    export PATH="${JAVA_HOME}/bin:${PATH}"
    mvn -Pnative package -DskipTests
fi
echo "==> Building Docker image (native)..."
docker build -f deploy/Dockerfile.native -t harness2:native .
echo "==> Stopping previous container (if any)..."
docker compose -f deploy/docker-compose.yml down 2>/dev/null || docker rm -f harness2-native 2>/dev/null || true
echo "==> Starting container on port 18004 (localhost only)..."
docker compose -f deploy/docker-compose.yml up -d --build
echo "==> Deployed. Endpoint: http://127.0.0.1:18004"
docker logs --tail 5 harness2-native