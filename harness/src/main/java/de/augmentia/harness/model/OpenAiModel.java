package de.augmentia.harness.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.augmentia.harness.Model;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAiModel implements Model {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String sessionId;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public OpenAiModel(String baseUrl, String apiKey, String model) {
        this(baseUrl, apiKey, model, "");
    }

    public OpenAiModel(String baseUrl, String apiKey, String model, String sessionId) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        if (sessionId == null || sessionId.isBlank()) {
            this.sessionId = java.util.UUID.randomUUID().toString();
        } else {
            this.sessionId = sessionId;
        }
    }

    @Override
    public Response complete(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        try {
            ObjectNode req = MAPPER.createObjectNode();
            req.put("model", model);
            ArrayNode msgs = req.putArray("messages");
            for (Map<String, Object> m : messages) {
                ObjectNode mn = MAPPER.createObjectNode();
                for (Map.Entry<String, Object> e : m.entrySet()) {
                    mn.set(e.getKey(), MAPPER.valueToTree(e.getValue()));
                }
                msgs.add(mn);
            }
            if (tools != null && !tools.isEmpty()) {
                ArrayNode tArr = req.putArray("tools");
                for (Map<String, Object> t : tools) {
                    ObjectNode to = MAPPER.createObjectNode();
                    to.put("type", "function");
                    ObjectNode fn = to.putObject("function");
                    fn.put("name", String.valueOf(t.get("name")));
                    fn.put("description", String.valueOf(t.get("description")));
                    Object params = t.get("parameters");
                    if (params != null) {
                        fn.set("parameters", MAPPER.valueToTree(params));
                    } else {
                        fn.putObject("parameters").put("type", "object");
                    }
                    tArr.add(to);
                }
            }
            String json = MAPPER.writeValueAsString(req);
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            if (apiKey != null && !apiKey.isBlank()) rb.header("Authorization", "Bearer " + apiKey);
            if (!sessionId.isBlank()) rb.header("x-opencode-session", sessionId);
            HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                return new Response("HTTP " + resp.statusCode() + ": " + resp.body(), List.of(), 0, 0.0);
            }
            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode choice = root.path("choices").path(0).path("message");
            String content = choice.path("content").asText("");
            List<ToolCall> calls = new ArrayList<>();
            JsonNode tc = choice.path("tool_calls");
            if (tc.isArray()) {
                for (JsonNode c : tc) {
                    String id = c.path("id").asText("");
                    String name = c.path("function").path("name").asText("");
                    JsonNode argsNode = c.path("function").path("arguments");
                    Map<String, Object> args = argsNode.isTextual()
                            ? MAPPER.readValue(argsNode.asText(), Map.class)
                            : MAPPER.convertValue(argsNode, Map.class);
                    calls.add(new ToolCall(id, name, args));
                }
            }
            int tokens = root.path("usage").path("total_tokens").asInt(0);
            return new Response(content, calls, tokens, 0.0);
        } catch (Exception e) {
            return new Response("ERROR: " + e.getMessage(), List.of(), 0, 0.0);
        }
    }
}