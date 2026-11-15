package de.augmentia.harness;

import java.util.List;
import java.util.Map;

/**
 * Provider abstraction (Recommendation 2): protocol/interface for LLM completion.
 */
public interface Model {
    Response complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools);

    record Response(String content, List<ToolCall> toolCalls, int tokens, double cost) {
        public Response {
            content = content == null ? "" : content;
            toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        }
    }

    record ToolCall(String id, String name, Map<String, Object> args) {}
}