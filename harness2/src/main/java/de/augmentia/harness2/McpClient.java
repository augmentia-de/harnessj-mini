package de.augmentia.harness2;

import java.util.List;
import java.util.Map;

public interface McpClient {
    enum TrustTier { TRUSTED, SCANNED, QUARANTINED }

    List<Map<String, Object>> listTools();
    String callTool(String name, Map<String, Object> args);
    TrustTier trustTier();

    /**
     * Scan tools for dangerous patterns. Returns true if safe.
     */
    default boolean scan() {
        List<String> dangerous = List.of("bash", "rm", "delete", "exec", "shell", "eval");
        for (Map<String, Object> tool : listTools()) {
            String name = String.valueOf(tool.get("name")).toLowerCase();
            for (String d : dangerous) {
                if (name.contains(d)) return false;
            }
        }
        return true;
    }

    /**
     * Whether this client is allowed to execute tools.
     */
    default boolean isQuarantined() {
        return trustTier() == TrustTier.QUARANTINED;
    }
}