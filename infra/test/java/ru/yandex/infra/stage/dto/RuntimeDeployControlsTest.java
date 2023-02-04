package ru.yandex.infra.stage.dto;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class RuntimeDeployControlsTest {
    @Test
    void equalsForSpecifiedDeployUnit() {
        var rdc = new RuntimeDeployControls(Map.of(
                "du1", Set.of("sas", "vla"),
                "du2", Set.of("myt")
        ), Map.of(), Map.of());

        assertThat(rdc.equals(rdc, "du1"), equalTo(true));
        assertThat(rdc.equals(rdc, "du2"), equalTo(true));
        assertThat(rdc.equals(rdc, "missed"), equalTo(true));

        assertThat(rdc.equals(null, "du1"), equalTo(false));
        assertThat(rdc.equals(null, "du2"), equalTo(false));
        assertThat(rdc.equals(null, "missed"), equalTo(false));

        var rdc2 = new RuntimeDeployControls(Map.of(
                "du1", Set.of("vla"),
                "du2", Set.of("myt")
        ), Map.of(), Map.of());

        assertThat(rdc.equals(rdc2, "du1"), equalTo(false));
        assertThat(rdc.equals(rdc2, "du2"), equalTo(true));
        assertThat(rdc.equals(rdc2, "missed"), equalTo(true));

        var rdc3 = new RuntimeDeployControls(Map.of(
                "du2", Set.of("myt")
        ), Map.of(), Map.of());
        assertThat(rdc.equals(rdc3, "du1"), equalTo(false));
        assertThat(rdc.equals(rdc3, "du2"), equalTo(true));
    }
}
