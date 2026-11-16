package de.augmentia.harness.core;

import java.nio.file.*;
import java.util.*;

/**
 * Hierarchical Markdown context auto-discovery.
 *
 * <h2>Purpose</h2>
 * Walks from the working directory up to the filesystem root and collects
 * all context files (AGENTS.md, CLAUDE.md, CONTEXT.md, .cursorrules, GEMINI.md).
 * Content is injected into the system prompt so the model sees project-level
 * instructions without explicit configuration.
 *
 * <h2>Discovery order</h2>
 * Root-to-leaf: parent directories are scanned first, then child directories.
 * This ensures top-level rules take precedence.
 */
public final class ContextDiscovery {
    private static final List<String> CONVENTIONS = List.of(
            "AGENTS.md", "CLAUDE.md", "CONTEXT.md", ".cursorrules", "GEMINI.md");

    private ContextDiscovery() {}

    public static String discover(Path cwd) {
        List<String> parts = new ArrayList<>();
        Path current = (cwd == null ? Path.of(".") : cwd).toAbsolutePath().normalize();
        while (current != null) {
            for (String name : CONVENTIONS) {
                Path md = current.resolve(name);
                if (Files.exists(md)) {
                    try {
                        parts.add(0, "<ctx path='" + md + "'>\n" + Files.readString(md) + "\n</ctx>");
                    } catch (Exception ignored) {}
                }
            }
            current = current.getParent();
        }
        return String.join("\n", parts);
    }
}