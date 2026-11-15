# harnessj-mini

Minimal agent harness in Java — two implementations:

- **`harness/`** — Spring Boot (Java 21), Lanterna TUI + Web UI (SSE), port 18000.
- **`harness2/`** — Quarkus + LangChain4j, native build, Web UI, port 18004.

Both implement the 18 design recommendations from the Harness Engineering paper (Section 16) as a 90-line MVP scaffold.

## Build

```bash
# harness (Spring Boot)
cd harness && mvn package -DskipTests

# harness2 (Quarkus native)
cd harness2 && mvn -Pnative package -DskipTests
```

## Run

```bash
# harness
cd harness && ./start.sh {tui|web|run} [task]

# harness2
cd harness2 && ./start.sh {web|run}
```

## Docker

```bash
# harness
cd harness && docker compose up -d --build

# harness2
cd harness2 && ./deploy-native.sh
```

## Configuration

See `.env.example` in each subproject.