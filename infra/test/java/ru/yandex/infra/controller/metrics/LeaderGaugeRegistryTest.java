package ru.yandex.infra.controller.metrics;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.concurrent.DummyLockingService;
import ru.yandex.infra.controller.concurrent.LeaderServiceImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaderGaugeRegistryTest {
    private static final String METRIC_NAME = "metric_name";

    private MetricRegistry metricRegistry;
    private LeaderGaugeRegistry gaugeRegistry;
    private DummyLockingService lockingService;
    private LeaderServiceImpl leaderService;

    @BeforeEach
    void before() {
        metricRegistry = new MetricRegistry();
        // Manipulating LeaderService is harder, so use LockingService to simplify things
        lockingService = new DummyLockingService(true);
        leaderService = new LeaderServiceImpl("test", lockingService, metricRegistry);
        gaugeRegistry = new LeaderGaugeRegistry(metricRegistry, leaderService);
    }

    @Test
    void addMetricIfProcessingAllowed() {
        lockingService.lock();
        leaderService.allowProcessing();
        addMetric();
        assertMetricStored();
    }

    @Test
    void notAddMetricIfLockNotAcquired() {
        addMetric();
        assertMetricNotStored();
    }

    @Test
    void dontRemoveMetricsOnLeadershipLoss() {
        leaderService.ensureLeadership();
        leaderService.allowProcessing();
        addMetric();
        lockingService.loseLock();
        lockingService.setLockCanBeTaken(false);
        assertThrows(RuntimeException.class, leaderService::ensureLeadership);
        assertMetricStored();
    }

    @Test
    void restoreMetricOnLockAcquired() {
        addMetric();
        assertMetricNotStored();
        leaderService.ensureLeadership();
        assertMetricStored();
    }

    private void addMetric() {
        gaugeRegistry.add(METRIC_NAME, () -> 5);
    }

    private void assertMetricStored() {
        assertThat(metricRegistry.getGauges(), hasKey(METRIC_NAME));
    }

    private void assertMetricNotStored() {
        assertThat(metricRegistry.getGauges(), not(hasKey(METRIC_NAME)));
    }
}
