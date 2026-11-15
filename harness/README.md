# Harness — 90-Zeilen-MVP-Scaffold

Eigenständiges Java-Modul. Implementiert 10 von 18 Design-Empfehlungen.

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

Siehe `.env.example`. Werte werden über `application.properties` gemappt.