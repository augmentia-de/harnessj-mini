package de.augmentia.harness;

import de.augmentia.harness.PermissionGate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionGateTest {
    @Test
    void planModeOnlyRead() {
        var gate = PermissionGate.of(PermissionGate.Mode.PLAN);
        assertThat(gate.allow("read_file", Map.of())).isTrue();
        assertThat(gate.allow("write_file", Map.of())).isFalse();
        assertThat(gate.allow("bash", Map.of())).isFalse();
    }

    @Test
    void yoloAllowsAll() {
        var gate = PermissionGate.of(PermissionGate.Mode.YOLO);
        assertThat(gate.allow("bash", Map.of("cmd", "rm -rf /"))).isTrue();
    }

    @Test
    void defaultGatesBash() {
        var gate = PermissionGate.of(PermissionGate.Mode.DEFAULT);
        assertThat(gate.allow("bash", Map.of("cmd", "ls"))).isTrue();
        assertThat(gate.allow("bash", Map.of("cmd", "rm -rf /"))).isFalse();
    }

    @Test
    void policyFileLoads() {
        // no file -> default allow
        var gate = PermissionGate.fromPolicyFile(java.nio.file.Path.of("/nonexistent"));
        assertThat(gate.allow("anything", Map.of())).isTrue();
    }
}