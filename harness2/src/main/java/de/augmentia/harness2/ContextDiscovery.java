package de.augmentia.harness2;

import java.nio.file.*;
import java.util.*;

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