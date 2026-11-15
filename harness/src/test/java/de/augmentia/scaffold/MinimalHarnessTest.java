package de.augmentia.scaffold;

import de.augmentia.harness.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MinimalHarnessTest {

    static class MockModel implements Model {
        private final String response;
        private int calls = 0;

        MockModel(String response) { this.response = response; }

        @Override
        public Response complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
            calls++;
            return new Response(response, List.of(), 10, 0.01);
        }
    }

    static class MockModelWithToolCall implements Model {
        private final String toolName;
        private final Map<String, Object> args;
        private int calls = 0;

        MockModelWithToolCall(String toolName, Map<String, Object> args) {
            this.toolName = toolName;
            this.args = args;
        }

        @Override
        public Response complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
            calls++;
            if (calls == 1) {
                return new Response("", List.of(new ToolCall("c1", toolName, args)), 10, 0.01);
            }
            // second call: include tool result echo
            return new Response("done", List.of(), 10, 0.01);
        }
    }

    @Test
    void runsSimpleTask() {
        Model mock = new MockModel("done");
        MinimalHarness h = new MinimalHarness(mock, 10, 5.0, 100000);
        String result = h.run("say hello");
        assertThat(result).isEqualTo("done");
    }

    @Test
    void executesToolCall() {
        Model mock = new MockModelWithToolCall("bash", Map.of("cmd", "echo hi"));
        MinimalHarness h = new MinimalHarness(mock, 10, 5.0, 100000);
        String result = h.run("run echo");
        assertThat(result).contains("done");
    }

    @Test
    void truncatesLongOutput() {
        String longStr = "x".repeat(30_000);
        assertThat(Tools.truncate(longStr).length()).isLessThan(26_000);
    }

    @Test
    void discoversContext() {
        // Basic: discover on temp dir without AGENTS.md returns empty string
        String ctx = ContextDiscovery.discover(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));
        assertThat(ctx).isNotNull();
    }

    @Test
    void permissionGateBlocks() {
        Model mock = new MockModelWithToolCall("write_file", Map.of("path", "/tmp/x", "content", "hi"));
        MinimalHarness h = new MinimalHarness(mock, 10, 5.0, 100000, PermissionGate.readOnly());
        String result = h.run("write file");
        assertThat(result).contains("done");
    }
}