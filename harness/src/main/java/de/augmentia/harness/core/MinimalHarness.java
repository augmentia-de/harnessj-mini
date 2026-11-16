package de.augmentia.harness.core;

import de.augmentia.harness.PermissionGate;
import de.augmentia.harness.SubAgent;
import de.augmentia.harness.tools.Tools;
import de.augmentia.harness.exceptions.RetryableException;
import de.augmentia.harness.exceptions.NonRetryableException;

import java.nio.file.Path;
import java.util.*;

/**
 * Minimum viable agent harness (~90 lines concept, extended with retry/streaming).
 *
 * <h2>Purpose</h2>
 * Runs an LLM-driven agent loop: sends a task to the model, executes tool calls,
 * feeds results back, and repeats until the model stops calling tools.
 *
 * <h2>Execution flow (chat / run)</h2>
 * <ol>
 *   <li><b>Setup:</b> Clear messages (run) or keep them (chat). Add system prompt
 *       with auto-discovered context (AGENTS.md, CLAUDE.md, ...).</li>
 *   <li><b>Loop:</b> {@code while (true)}:
 *     <ul>
 *       <li>{@link #checkLimits()} — abort if maxTurns, maxCost, or wall-clock exceeded.</li>
 *       <li>{@link #maybeCompact()} — if token estimate exceeds threshold, summarize
 *           and keep system + last 30 %.</li>
 *       <li>{@code model.complete()} — call LLM with current messages + tool schemas.
 *           Retried on transient errors (429, 5xx) with exponential backoff.</li>
 *       <li>Append assistant response (content + tool_calls) to messages.</li>
 *       <li>If no tool calls → return content (done).</li>
 *       <li>For each tool call:
 *         <ul>
 *           <li>Stuck check (3 identical calls → hard stop).</li>
 *           <li>Permission gate (PLAN/DEFAULT/YOLO).</li>
 *           <li>Dispatch tool → truncate output → append as tool result.</li>
 *           <li>Errors are caught and returned as "ERROR: ..." (error-as-observation).</li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Key invariants</h2>
 * <ul>
 *   <li>Messages are never silently dropped — compaction preserves system + recent tail.</li>
 *   <li>Tool errors become observations, not exceptions — the LLM can react to them.</li>
 *   <li>Limits are checked before every turn — no runaway loops.</li>
 *   <li>Stuck detection prevents infinite tool-call cycles.</li>
 * </ul>
 *
 * @see Model
 * @see Tools
 * @see ContextDiscovery
 */
public class MinimalHarness {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MinimalHarness.class);
    private final Model model;
    private final List<Map<String, Object>> messages = new ArrayList<>();
    private int nTurns = 0;
    private double cost = 0.0;
    private final int maxTurns;
    private final double maxCost;
    private final int compactAtTokens;
    private final long wallClockMaxMs;
    private final long startedAt;
    private final PermissionGate permissionGate;
    private final int maxRetries;
    private final long retryBackoffMs;
    private final List<String> recentCallSignatures = new ArrayList<>();
    private int consecutiveMalformed = 0;

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens) {
        this(model, maxTurns, maxCost, compactAtTokens, null, 0, 3, 1000);
    }

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens, PermissionGate permissionGate) {
        this(model, maxTurns, maxCost, compactAtTokens, permissionGate, 0, 3, 1000);
    }

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens, PermissionGate permissionGate, long wallClockMaxMs) {
        this(model, maxTurns, maxCost, compactAtTokens, permissionGate, wallClockMaxMs, 3, 1000);
    }

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens, PermissionGate permissionGate, long wallClockMaxMs, int maxRetries, long retryBackoffMs) {
        this.model = model;
        this.maxTurns = maxTurns;
        this.maxCost = maxCost;
        this.compactAtTokens = compactAtTokens;
        this.permissionGate = permissionGate;
        this.wallClockMaxMs = wallClockMaxMs;
        this.startedAt = System.currentTimeMillis();
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;
    }

    private int tokenEstimate() {
        return messages.stream().mapToInt(m -> ("" + m).length() / 4).sum();
    }

    private void checkLimits() {
        if (nTurns >= maxTurns) { log.warn("max_turns reached: {}", maxTurns); throw new IllegalStateException("max_turns=" + maxTurns); }
        if (cost >= maxCost) { log.warn("max_cost reached: {}", maxCost); throw new IllegalStateException("max_cost=$" + maxCost); }
        if (wallClockMaxMs > 0 && (System.currentTimeMillis() - startedAt) >= wallClockMaxMs) {
            log.warn("wall_clock_cap reached: {}ms", wallClockMaxMs);
            throw new IllegalStateException("wall_clock_cap=" + wallClockMaxMs + "ms");
        }
    }

    private void maybeCompact() {
        if (tokenEstimate() < compactAtTokens) return;
        String summary = model.complete(
                new ArrayList<>(messages) {{ add(Map.of("role", "user", "content",
                        "Summarize the conversation. Preserve decisions and unresolved issues.")); }},
                List.of()).content();
        int keep = Math.max(4, (int) (messages.size() * 0.30));
        List<Map<String, Object>> kept = new ArrayList<>(messages.subList(Math.max(0, messages.size() - keep), messages.size()));
        messages.clear();
        messages.add(Map.of("role", "system", "content", "You are a SWE agent."));
        messages.add(Map.of("role", "assistant", "content", summary));
        messages.addAll(kept);
    }

    private boolean isStuck(Model.ToolCall call) {
        String sig = call.name() + ":" + call.args().hashCode();
        recentCallSignatures.add(sig);
        if (recentCallSignatures.size() > 3) recentCallSignatures.remove(0);
        if (recentCallSignatures.size() == 3 && recentCallSignatures.stream().distinct().count() == 1) {
            return true;
        }
        return false;
    }

    private Model.Response completeWithRetry(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        int attempts = 0;
        while (true) {
            try {
                return model.complete(messages, tools);
            } catch (NonRetryableException e) {
                log.warn("Non-retryable LLM error: {}", e.getMessage());
                throw e;
            } catch (RetryableException e) {
                attempts++;
                if (attempts >= maxRetries) {
                    log.warn("LLM retry exhausted after {} attempts: {}", attempts, e.getMessage());
                    throw e;
                }
                long backoff = retryBackoffMs * (1L << (attempts - 1));
                log.info("LLM retryable error (attempt {}/{}), backing off {}ms: {}", attempts, maxRetries, backoff, e.getMessage());
                try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
            } catch (Exception e) {
                // Treat unknown exceptions as retryable
                attempts++;
                if (attempts >= maxRetries) {
                    log.warn("LLM unknown error, retry exhausted: {}", e.getMessage());
                    throw new RetryableException("LLM error: " + e.getMessage(), e);
                }
                long backoff = retryBackoffMs * (1L << (attempts - 1));
                log.info("LLM unknown error (attempt {}/{}), backing off {}ms: {}", attempts, maxRetries, backoff, e.getMessage());
                try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
            }
        }
    }

    public String run(String task) {
        return run(task, Path.of("."));
    }

    /**
     * Chat-style turn: preserves conversation history across calls.
     * On first call, sets up system prompt + context. Subsequent calls
     * append user message and continue the loop.
     */
    public String chat(String userMessage) {
        return chat(userMessage, Path.of("."), null);
    }

    public String chat(String userMessage, Path cwd) {
        return chat(userMessage, cwd, null);
    }

    public String chat(String userMessage, java.util.function.Consumer<String> onTurn) {
        return chat(userMessage, Path.of("."), onTurn);
    }

    public String chat(String userMessage, Path cwd, java.util.function.Consumer<String> onTurn) {
        if (messages.isEmpty()) {
            messages.add(Map.of("role", "system", "content",
                    "You are a SWE agent.\n\n" + ContextDiscovery.discover(cwd)));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        while (true) {
            checkLimits();
            maybeCompact();
            Model.Response resp = completeWithRetry(messages, Tools.schemas());
            nTurns++;
            cost += resp.cost();
            messages.add(Map.of("role", "assistant", "content", resp.content(),
                    "tool_calls", resp.toolCalls()));
            if (resp.toolCalls().isEmpty()) return resp.content();
            for (Model.ToolCall call : resp.toolCalls()) {
                log.debug("Tool call: {} args={}", call.name(), call.args());
                if (isStuck(call)) {
                    messages.add(Map.of("role", "tool", "tool_call_id", call.id(),
                            "content", Tools.truncate("ERROR: stuck - repeated identical tool call")));
                    continue;
                }
                String out;
                try {
                    if (permissionGate != null && !permissionGate.allow(call.name(), call.args())) {
                        out = "ERROR: permission denied for " + call.name();
                    } else {
                        out = Tools.dispatch(call.name(), call.args());
                    }
                } catch (Exception e) {
                    out = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    consecutiveMalformed++;
                }
                messages.add(Map.of("role", "tool", "tool_call_id", call.id(),
                        "content", Tools.truncate(out)));
            }
            if (consecutiveMalformed >= 5) throw new IllegalStateException("too many malformed responses");
            if (onTurn != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Turn ").append(nTurns).append(": ").append(resp.content());
                if (!resp.toolCalls().isEmpty()) {
                    sb.append(" [").append(resp.toolCalls().size()).append(" tool calls]");
                }
                onTurn.accept(sb.toString());
            }
        }
    }

    /** Reset conversation history. */
    public void reset() {
        messages.clear();
        nTurns = 0;
        cost = 0.0;
        recentCallSignatures.clear();
        consecutiveMalformed = 0;
    }

    public String run(String task, Path cwd) {
        messages.clear();
        messages.add(Map.of("role", "system", "content",
                "You are a SWE agent.\n\n" + ContextDiscovery.discover(cwd)));
        messages.add(Map.of("role", "user", "content", task));

        while (true) {
            checkLimits();
            maybeCompact();
            Model.Response resp = completeWithRetry(messages, Tools.schemas());
            nTurns++;
            cost += resp.cost();
            messages.add(Map.of("role", "assistant", "content", resp.content(),
                    "tool_calls", resp.toolCalls()));
            if (resp.toolCalls().isEmpty()) return resp.content();
            for (Model.ToolCall call : resp.toolCalls()) {
                log.debug("Tool call: {} args={}", call.name(), call.args());
                if (isStuck(call)) {
                    messages.add(Map.of("role", "tool", "tool_call_id", call.id(),
                            "content", Tools.truncate("ERROR: stuck - repeated identical tool call")));
                    continue;
                }
                String out;
                try {
                    if (permissionGate != null && !permissionGate.allow(call.name(), call.args())) {
                        out = "ERROR: permission denied for " + call.name();
                    } else {
                        out = Tools.dispatch(call.name(), call.args());
                    }
                } catch (Exception e) {
                    out = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    consecutiveMalformed++;
                }
                messages.add(Map.of("role", "tool", "tool_call_id", call.id(),
                        "content", Tools.truncate(out)));
            }
            if (consecutiveMalformed >= 5) throw new IllegalStateException("too many malformed responses");
        }
    }
}