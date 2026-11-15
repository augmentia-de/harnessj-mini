# Harness2 — Quarkus + LangChain4j (Native)

Functionally identical to `harness`, but based on Quarkus + LangChain4j with native build.

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

## Configuration
See `.env.example`. Values are mapped via `application.properties` (`llm.*`).

## Note
LangChain4j makes the code shorter and native-build-safe, but adds a dependency.