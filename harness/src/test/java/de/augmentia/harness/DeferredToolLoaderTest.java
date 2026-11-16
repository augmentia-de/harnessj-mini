package de.augmentia.harness;

import de.augmentia.harness.DeferredToolLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredToolLoaderTest {
    @Test
    void noDeferBelowThreshold() {
        var tools = List.<Map<String, Object>>of(Map.of("name", "a", "description", "a tool"));
        var loader = new DeferredToolLoader(tools, 15);
        assertThat(loader.shouldDefer()).isFalse();
        assertThat(loader.visibleTools()).hasSize(1);
    }

    @Test
    void deferAboveThreshold() {
        var tools = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < 20; i++) tools.add(new java.util.HashMap<>(Map.of("name", "t" + i, "description", "tool " + i)));
        var loader = new DeferredToolLoader(tools, 15);
        assertThat(loader.shouldDefer()).isTrue();
        assertThat(loader.visibleTools().stream().map(t -> t.get("name")).toList()).contains("tool_search", "tool_load");
    }

    @Test
    void searchAndLoad() {
        var tools = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < 20; i++) tools.add(new java.util.HashMap<>(Map.of("name", "t" + i, "description", "tool " + i)));
        var loader = new DeferredToolLoader(tools, 15);
        assertThat(loader.search("tool 5")).hasSize(1);
        assertThat(loader.load("t5")).isTrue();
        assertThat(loader.visibleTools().stream().map(t -> t.get("name")).toList()).contains("t5");
    }
}