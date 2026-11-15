#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p "${LOG_DIR:-./logs}"
export JAVA_HOME=/tmp/opencode/mandrel/mandrel
export PATH="${JAVA_HOME}/bin:${PATH}"
echo "==> Building native executable (Mandrel)..."
mvn -Pnative package -DskipTests
echo "==> Native build complete: target/*-runner"
ls -lh target/*-runner