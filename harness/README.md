# Harness — 90-Zeilen-MVP-Scaffold

Standalone Java module. Implements 10 of 18 design recommendations.

## Module

- `Model` — Provider-Abstraktion (Interface)
- `Tools` — bash, read_file, write_file, search_replace + Truncation
- `ContextDiscovery` — hierarchische AGENTS.md-Discovery
- `MinimalHarness` — linearer Loop, Limits, Threshold-Compaction, Error-as-Observation, Stuck-Detection
- Optional: `PermissionGate`, `MemoryStore`, `McpClient`, `SubAgent`

## Build & Test

```bash
mvn test
mvn package -DskipTests
```

## Run

```bash
set -a; . ./.env; set +a
java -jar target/harness-0.1.0.jar "your task here"
```

## Konfiguration

See `.env.example`. Values are mapped via `application.properties`.