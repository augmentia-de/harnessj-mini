package de.augmentia.harness;

import java.nio.file.Path;
import java.util.*;

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
    private final List<String> recentCallSignatures = new ArrayList<>();
    private int consecutiveMalformed = 0;

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens) {
        this(model, maxTurns, maxCost, compactAtTokens, null, 0);
    }

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens, PermissionGate permissionGate) {
        this(model, maxTurns, maxCost, compactAtTokens, permissionGate, 0);
    }

    public MinimalHarness(Model model, int maxTurns, double maxCost, int compactAtTokens, PermissionGate permissionGate, long wallClockMaxMs) {
        this.model = model;
        this.maxTurns = maxTurns;
        this.maxCost = maxCost;
        this.compactAtTokens = compactAtTokens;
        this.permissionGate = permissionGate;
        this.wallClockMaxMs = wallClockMaxMs;
        this.startedAt = System.currentTimeMillis();
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
            Model.Response resp = model.complete(messages, Tools.schemas());
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
            Model.Response resp = model.complete(messages, Tools.schemas());
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