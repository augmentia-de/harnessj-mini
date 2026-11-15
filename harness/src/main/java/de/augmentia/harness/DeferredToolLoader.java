package de.augmentia.harness;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deferred Tool Loading (Recommendation 4): when tool count exceeds threshold,
 * expose only a search tool + load individual tools on demand.
 */
public final class DeferredToolLoader {
    private final List<Map<String, Object>> allTools;
    private final Set<String> loaded = new HashSet<>();
    private final int threshold;

    public DeferredToolLoader(List<Map<String, Object>> allTools, int threshold) {
        this.allTools = new ArrayList<>(allTools);
        this.threshold = threshold;
    }

    public boolean shouldDefer() {
        return allTools.size() > threshold;
    }

    public List<Map<String, Object>> visibleTools() {
        if (!shouldDefer()) return allTools;
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(Map.of("name", "tool_search", "description", "Search available tools by keyword"));
        out.add(Map.of("name", "tool_load", "description", "Load a tool by name for use"));
        for (String name : loaded) {
            allTools.stream().filter(t -> name.equals(t.get("name"))).findFirst().ifPresent(out::add);
        }
        return out;
    }

    public List<Map<String, Object>> search(String query) {
        String q = query == null ? "" : query.toLowerCase();
        return allTools.stream()
                .filter(t -> String.valueOf(t.get("name")).toLowerCase().contains(q)
                        || String.valueOf(t.get("description")).toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public boolean load(String name) {
        boolean exists = allTools.stream().anyMatch(t -> name.equals(t.get("name")));
        if (exists) loaded.add(name);
        return exists;
    }
}