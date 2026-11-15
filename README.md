# harnessj-mini

**harnessj-mini** is a minimal, testable Java reference for agent harness loops. It implements the core agent-loop pattern — `while(true)` until no tool calls, with limits, compaction, stuck detection, permission gate, error as observation — in two variants: a Spring Boot edition (Lanterna TUI + Web UI) and a Quarkus + LangChain4j edition (native build). No agentic framework, no RAG, no vector store. Built to understand what an agent harness actually is before scaling to production.

Minimal agent harness in Java — two implementations:

- **`harness/`** — Spring Boot (Java 21), Lanterna TUI + Web UI (SSE), port 18000.
- **`harness2/`** — Quarkus + LangChain4j, native build, Web UI, port 18004.

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