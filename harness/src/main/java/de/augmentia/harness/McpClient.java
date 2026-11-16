package de.augmentia.harness;

import java.util.List;
import java.util.Map;

/**
 * MCP client interface with trust tiers (Recommendation 14).
 *
 * <h2>Trust tiers</h2>
 * <ul>
 *   <li><b>TRUSTED:</b> auto-load tools.</li>
 *   <li><b>SCANNED:</b> load after scanning for dangerous patterns.</li>
 *   <li><b>QUARANTINED:</b> blocked from execution.</li>
 * </ul>
 *
 * <h2>Scanning</h2>
 * {@link #scan()} checks tool names against a list of dangerous patterns
 * (bash, rm, delete, exec, shell, eval).
 */
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