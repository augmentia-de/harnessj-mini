# harnessj-mini
​harnessj-mini is a minimal, testable Java reference for agent harness loops. It implements the core agent-loop pattern — while(true) until no tool calls, with limits, compaction, stuck detection, permission gate, error as observation — in two variants: a Spring Boot edition (Lanterna TUI + Web UI) and a Quarkus + LangChain4j edition (native build). No agentic framework, no RAG, no vector store. Built to look under the hood, understand agent behavior, and grasp what an agent harness actually is before scaling to production..

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

## Architecture & Entry Points

### Where to start reading

1. **`Model.java`** (`core/`) — the provider interface. Defines `complete(messages, tools)` and the `Response`/`ToolCall` records. 30 lines.
2. **`Tools.java`** (`tools/`) — the 4 tools (bash, read_file, write_file, search_replace) + `dispatch()` + `truncate()`. The tool surface.
3. **`ContextDiscovery.java`** (`core/`) — walks from cwd to root, collects AGENTS.md / CLAUDE.md / .cursorrules / etc.
4. **`MinimalHarness.java`** (`core/`) — the agent loop. Read `run()` and `chat()` first, then `checkLimits()`, `maybeCompact()`, `completeWithRetry()`.
5. **`HarnessApplication.java`** — Spring Boot entry point. Wires config → model → harness → CLI/TUI/Web.
6. **`HarnessController.java`** (`web/`) — REST/SSE endpoint. Shows how the loop is invoked from the web.

### Execution flow (one turn)

```
Task → MinimalHarness.run()
  → ContextDiscovery.discover(cwd)
  → messages = [system + context, user task]
  → while (true):
      → checkLimits()          // maxTurns, maxCost, wall-clock
      → maybeCompact()         // threshold compaction (50% / 30% tail)
      → model.complete()       // LLM call (with retry on 429/5xx)
      → append assistant response
      → if no tool calls → return
      → for each tool call:
          → isStuck?           // 3 identical calls → hard stop
          → permissionGate.allow?  // PLAN/DEFAULT/YOLO
          → Tools.dispatch()   // execute + truncate
          → append tool result (error-as-observation)
      → if consecutiveMalformed >= 5 → throw
```

### Key invariants

- Messages are never silently dropped — compaction preserves system + recent tail.
- Tool errors become observations, not exceptions — the LLM can react.
- Limits are checked before every turn — no runaway loops.
- Stuck detection prevents infinite tool-call cycles.

### Optional extensions

| Module | Purpose |
|---|---|
| `tools/FuzzyEdit.java` | Fuzzy cascade for edit tools (Levenshtein @ 0.65) |
| `tools/DeferredToolLoader.java` | Deferred loading for >15 tools |
| `tools/SandboxedProcessRunner.java` | OS-level sandboxing (bwrap) |
| `McpClient.java` | MCP client with trust tiers |
| `MemoryStore.java` | Persistent memory interface |
| `SubAgent.java` | Multi-agent delegation |

## Configuration

See `.env.example` in each subproject.
