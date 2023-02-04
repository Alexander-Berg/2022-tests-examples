package ru.yandex.infra.sidecars_updater.metrics;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.infra.sidecars_updater.sidecars.Sidecar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SidecarMetricsTest {

    @ParameterizedTest
    @EnumSource(Sidecar.Type.class)
    void notFailWhenGetCountByAbsentRevisionTest(Sidecar.Type type) {
        SidecarMetrics sidecarMetrics = new SidecarMetrics(type);
        int presentRevision = 1;
        sidecarMetrics.addRevision(presentRevision);
        int absentRevision = 2;
        assertDoesNotThrow(() -> sidecarMetrics.getCountByRevision(absentRevision));
    }
}
