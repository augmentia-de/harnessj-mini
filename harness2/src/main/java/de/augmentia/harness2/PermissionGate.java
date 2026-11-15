package de.augmentia.harness2;

import java.util.Map;

public interface PermissionGate {
    enum Mode { PLAN, DEFAULT, YOLO }

    boolean allow(String tool, Map<String, Object> args);

    Mode mode();

    static PermissionGate allowAll() {
        return of(Mode.YOLO);
    }

    static PermissionGate readOnly() {
        return new PermissionGate() {
            public boolean allow(String tool, Map<String, Object> args) {
                return !tool.equals("write_file") && !tool.equals("search_replace") && !tool.equals("bash");
            }
            public Mode mode() { return Mode.PLAN; }
        };
    }

    static PermissionGate of(Mode mode) {
        return new PermissionGate() {
            public boolean allow(String tool, Map<String, Object> args) {
                return switch (mode) {
                    case PLAN -> tool.equals("read_file");
                    case DEFAULT -> !tool.equals("bash") || isSafeBash(args);
                    case YOLO -> true;
                };
            }
            public Mode mode() { return mode; }
        };
    }

    static PermissionGate fromPolicyFile(java.nio.file.Path path) {
        java.util.Map<String, Boolean> rules = new java.util.HashMap<>();
        java.util.Properties props = new java.util.Properties();
        try { props.load(java.nio.file.Files.newInputStream(path)); } catch (Exception ignored) {}
        for (String key : props.stringPropertyNames()) {
            rules.put(key.trim(), Boolean.parseBoolean(props.getProperty(key).trim()));
        }
        return new PermissionGate() {
            public boolean allow(String tool, Map<String, Object> args) {
                if (rules.containsKey(tool)) return rules.get(tool);
                return true; // default allow
            }
            public Mode mode() { return Mode.DEFAULT; }
        };
    }

    private static boolean isSafeBash(Map<String, Object> args) {
        if (args == null) return false;
        Object cmd = args.get("cmd");
        if (!(cmd instanceof String s)) return false;
        String t = s.trim().toLowerCase();
        return t.startsWith("ls") || t.startsWith("cat") || t.startsWith("echo") || t.startsWith("pwd");
    }
}