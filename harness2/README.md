# Harness2 — Quarkus + LangChain4j (Native)

Funktional identisch zu `harness`, aber auf Quarkus + LangChain4j Basis mit Native-Build.

## Build

```bash
# JVM
mvn package -DskipTests

# Native
mvn -Pnative package -DskipTests
```

## Run (JVM)
```bash
set -a; . ./.env; set +a
java -jar target/quarkus-app/quarkus-run.jar
# Web UI: http://localhost:18004
```

## Run (Native)
```bash
./target/harness2-0.1.0-runner
```

## Docker (Native)
```bash
./deploy-native.sh
```

## Konfiguration
Siehe `.env.example`. Werte werden über `application.properties` gemappt (`llm.*`).