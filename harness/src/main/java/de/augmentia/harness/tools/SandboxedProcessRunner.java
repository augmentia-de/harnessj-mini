package de.augmentia.harness.tools;

import java.io.*;
import java.util.*;

/**
 * OS-Level Sandboxing (Recommendation 10): wraps bash execution in bwrap if available.
 * Falls back to direct execution when bwrap is not installed.
 */
/**
 * OS-level sandboxing (Recommendation 10) for bash execution.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Check if {@code bwrap} (bubblewrap) is available.</li>
 *   <li>If available: wrap command in bwrap (read-only root, unshared namespaces).</li>
 *   <li>If not: fall back to direct bash execution.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * Replaces {@link Tools#bash(String)} when sandboxing is desired.
 * Check {@link #isSandboxed()} to see if bwrap is active.
 */
public final class SandboxedProcessRunner {
    private final boolean useSandbox;

    public SandboxedProcessRunner() {
        this.useSandbox = isBwrapAvailable();
    }

    public SandboxedProcessRunner(boolean force) {
        this.useSandbox = force && isBwrapAvailable();
    }

    private static boolean isBwrapAvailable() {
        try {
            Process p = new ProcessBuilder("bwrap", "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String run(String cmd) {
        try {
            List<String> pb = new ArrayList<>();
            if (useSandbox) {
                pb.addAll(List.of("bwrap", "--ro-bind", "/", "/", "--dev", "/dev",
                        "--proc", "/proc", "--tmpfs", "/tmp", "--unshare-all", "--die-with-parent"));
            }
            pb.addAll(List.of("bash", "-c", cmd));
            Process p = new ProcessBuilder(pb).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            int code = p.waitFor();
            return Tools.truncate("exit=" + code + "\nstdout:\n" + out);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public boolean isSandboxed() {
        return useSandbox;
    }
}