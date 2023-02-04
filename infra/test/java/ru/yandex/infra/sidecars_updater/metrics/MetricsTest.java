package ru.yandex.infra.sidecars_updater.metrics;

import java.util.Optional;
import java.util.stream.IntStream;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.infra.controller.metrics.MetricUtils;
import ru.yandex.infra.sidecars_updater.sidecars.Sidecar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MetricsTest {
    private MetricRegistry metricsRegistry;

    @BeforeEach
    public void beforeEach() {
        metricsRegistry = MetricUtils.buildMetricRegistry();
    }

    public void refreshMetricsScenario(Sidecar.Type type, Metrics metrics, int addTimes) {
        SidecarMetrics sidecarMetrics = new SidecarMetrics(type);
        long revision = 10;
        IntStream.range(0, addTimes).forEach((i) -> sidecarMetrics.addRevision(revision));
        metrics.refreshMetrics(sidecarMetrics);

        var gauges = metricsRegistry.getGauges();
        String key = type + "." + revision;
        var actualGaugeValue = Optional.ofNullable(gauges.get(key)).map(Gauge::getValue);

        assertThat(actualGaugeValue, equalTo(Optional.of(addTimes)));
    }

    @ParameterizedTest
    @EnumSource(Sidecar.Type.class)
    public void refreshMetricsTest(Sidecar.Type type) {
        Metrics metrics = new Metrics(metricsRegistry);
        // 1 add to ensure metric is correctly added
        refreshMetricsScenario(type, metrics, 1);
        // more than 1 adds to ensure metric is correctly updated
        refreshMetricsScenario(type, metrics, 3);
    }
}
