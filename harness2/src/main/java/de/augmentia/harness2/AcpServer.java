package de.augmentia.harness2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * ACP-Server (Recommendation 13): minimal HTTP/JSON-RPC endpoint exposing tools.
 * Runs on a given port; POST / with JSON-RPC 2.0 requests.
 */
public final class AcpServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpServer server;

    public AcpServer(int port) throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws java.io.IOException {
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ObjectNode req = (ObjectNode) MAPPER.readTree(body);
            String method = req.path("method").asText("");
            ObjectNode resp = MAPPER.createObjectNode();
            resp.put("jsonrpc", "2.0");
            resp.put("id", req.path("id").asInt(0));
            if ("tools/list".equals(method)) {
                resp.set("result", MAPPER.valueToTree(Map.of("tools", Tools.schemas())));
            } else if ("tools/call".equals(method)) {
                String name = req.path("params").path("name").asText("");
                Map<String, Object> args = MAPPER.convertValue(req.path("params").path("arguments"), Map.class);
                String out = Tools.dispatch(name, args);
                resp.set("result", MAPPER.valueToTree(Map.of("content", out)));
            } else {
                resp.put("error", "unknown method: " + method);
            }
            byte[] bytes = MAPPER.writeValueAsBytes(resp);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        } catch (Exception e) {
            try { ex.sendResponseHeaders(500, 0); ex.close(); } catch (Exception ignored) {}
        }
    }
}