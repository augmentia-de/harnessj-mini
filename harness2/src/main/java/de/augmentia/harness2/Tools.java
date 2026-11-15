package de.augmentia.harness2;

import java.nio.file.*;
import java.util.*;

public final class Tools {
    private static final int TRUNCATE_LIMIT = 25_000;

    private Tools() {}

    public static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= TRUNCATE_LIMIT ? s : s.substring(0, TRUNCATE_LIMIT) + "\n...[truncated]";
    }

    public static String bash(String cmd) {
        try {
            Process p = new ProcessBuilder("bash", "-c", cmd)
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            return truncate("exit=" + code + "\nstdout:\n" + out);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static String readFile(String path, int offset, int limit) {
        try {
            List<String> lines = Files.readAllLines(Path.of(path));
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < Math.min(lines.size(), offset + limit); i++) {
                sb.append(String.format("%4d: %s%n", i + 1, lines.get(i)));
            }
            return truncate(sb.toString());
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static String writeFile(String path, String content) {
        try {
            Files.writeString(Path.of(path), content == null ? "" : content);
            return "wrote " + (content == null ? 0 : content.length()) + " bytes";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static String searchReplace(String path, String search, String replace) {
        return searchReplace(path, search, replace, false);
    }

    public static String searchReplace(String path, String search, String replace, boolean fuzzy) {
        try {
            String text = Files.readString(Path.of(path));
            int count = countOccurrences(text, search);
            if (count == 1) {
                Files.writeString(Path.of(path), text.replace(search, replace));
                return "OK";
            }
            if (fuzzy) {
                String result = FuzzyEdit.replace(text, search, replace);
                if (result != null) {
                    Files.writeString(Path.of(path), result);
                    return "OK (fuzzy)";
                }
            }
            return "ERROR: search string occurs " + count + "x; must be unique";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private static int countOccurrences(String text, String search) {
        if (search == null || search.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = text.indexOf(search, idx)) >= 0) { count++; idx += search.length(); }
        return count;
    }

    public static List<Map<String, Object>> schemas() {
        return List.of(
            Map.of("name", "bash", "description", "Run a bash command",
                    "parameters", Map.of("type", "object",
                            "properties", Map.of("cmd", Map.of("type", "string", "description", "The bash command to run")),
                            "required", List.of("cmd"))),
            Map.of("name", "read_file", "description", "Read a file with line numbers",
                    "parameters", Map.of("type", "object",
                            "properties", Map.of("path", Map.of("type", "string", "description", "File path"),
                                    "offset", Map.of("type", "integer", "description", "Start line offset (0-based)"),
                                    "limit", Map.of("type", "integer", "description", "Max lines to read")),
                            "required", List.of("path"))),
            Map.of("name", "write_file", "description", "Write content to a file",
                    "parameters", Map.of("type", "object",
                            "properties", Map.of("path", Map.of("type", "string", "description", "File path"),
                                    "content", Map.of("type", "string", "description", "Content to write")),
                            "required", List.of("path", "content"))),
            Map.of("name", "search_replace", "description", "Replace unique search string in file",
                    "parameters", Map.of("type", "object",
                            "properties", Map.of("path", Map.of("type", "string", "description", "File path"),
                                    "search", Map.of("type", "string", "description", "Text to search for (must be unique)"),
                                    "replace", Map.of("type", "string", "description", "Replacement text"),
                                    "fuzzy", Map.of("type", "boolean", "description", "Use fuzzy matching")),
                            "required", List.of("path", "search", "replace")))
        );
    }

    public static String dispatch(String name, Map<String, Object> args) {
        Map<String, Object> a = args == null ? Map.of() : args;
        return switch (name) {
            case "bash" -> {
                Object cmd = a.get("cmd");
                if (cmd == null) cmd = a.get("command");
                yield bash(cmd == null ? "" : String.valueOf(cmd));
            }
            case "read_file" -> readFile(String.valueOf(a.get("path")),
                    toInt(a.getOrDefault("offset", 0)),
                    toInt(a.getOrDefault("limit", 2000)));
            case "write_file" -> writeFile(String.valueOf(a.get("path")), String.valueOf(a.get("content")));
            case "search_replace" -> searchReplace(String.valueOf(a.get("path")),
                    String.valueOf(a.get("search")), String.valueOf(a.get("replace")),
                    Boolean.TRUE.equals(a.get("fuzzy")));
            default -> "ERROR: unknown tool " + name;
        };
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}