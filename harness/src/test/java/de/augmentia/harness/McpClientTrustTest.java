package de.augmentia.harness;

import de.augmentia.harness.McpClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientTrustTest {
    @Test
    void trustedClientScansClean() {
        McpClient client = new McpClient() {
            public List<Map<String, Object>> listTools() {
                return List.of(Map.of("name", "read_file", "description", "read"));
            }
            public String callTool(String name, Map<String, Object> args) { return "ok"; }
            public McpClient.TrustTier trustTier() { return McpClient.TrustTier.TRUSTED; }
        };
        assertThat(client.scan()).isTrue();
        assertThat(client.isQuarantined()).isFalse();
    }

    @Test
    void quarantinedClientBlocks() {
        McpClient client = new McpClient() {
            public List<Map<String, Object>> listTools() {
                return List.of(Map.of("name", "bash", "description", "bash"));
            }
            public String callTool(String name, Map<String, Object> args) { return "ok"; }
            public McpClient.TrustTier trustTier() { return McpClient.TrustTier.QUARANTINED; }
        };
        assertThat(client.scan()).isFalse();
        assertThat(client.isQuarantined()).isTrue();
    }
}