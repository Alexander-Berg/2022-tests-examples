package ru.yandex.infra.controller.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaderServiceImplTest {
    private static final String TEST_SERVICE_NAME = "test";
    private MetricRegistry registry;

    @BeforeEach
    void before() {
        registry = new MetricRegistry();
    }

    @Test
    void denyProcessingAfterLossOfLock() {
        DummyLockingService lockingService = new DummyLockingService(true);
        LeaderServiceImpl leader = new LeaderServiceImpl(TEST_SERVICE_NAME, lockingService, registry);
        leader.ensureLeadership();
        assertMetricValues(1, 0);
        leader.allowProcessing();
        assertMetricValues(1, 1);
        lockingService.setLockCanBeTaken(false);
        lockingService.loseLock();
        assertThrows(RuntimeException.class, leader::ensureLeadership);
        assertThat("Processing should be forbidden after lock is lost", !leader.isProcessingAllowed());
        assertMetricValues(0, 0);
    }

    @Test
    void runCallbacksOnLeadershipAcquired() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        DummyLockingService lockingService = new DummyLockingService(true);
        LeaderServiceImpl leader = new LeaderServiceImpl(TEST_SERVICE_NAME, lockingService, registry);
        leader.addLeadershipAcquiredCallback(() -> triggered.set(true));
        leader.ensureLeadership();
        assertThat("Callback should have been called", triggered.get());
    }

    @Test
    void runCallbacksOnFirstProcessingAllowed() {
        AtomicInteger counter = new AtomicInteger(0);
        DummyLockingService lockingService = new DummyLockingService(true);
        LeaderServiceImpl leader = new LeaderServiceImpl(TEST_SERVICE_NAME, lockingService, registry);
        leader.addProcessingAllowedCallback(counter::incrementAndGet);
        leader.allowProcessing();
        assertThat(counter.get(), equalTo(1));
        leader.allowProcessing();
        assertThat(counter.get(), equalTo(1));
    }

    @Test
    void runCallbackOnLeadershipLost() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        DummyLockingService lockingService = new DummyLockingService(true);
        LeaderServiceImpl leader = new LeaderServiceImpl(TEST_SERVICE_NAME, lockingService, registry);
        leader.ensureLeadership();
        leader.addLeadershipLostCallback(() -> triggered.set(true));
        lockingService.loseLock();
        leader.ensureLeadership();
        assertThat("Callback should have been called", triggered.get());
    }

    @Test
    void notRunLossCallbacksOnStart() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        DummyLockingService lockingService = new DummyLockingService(true);
        LeaderServiceImpl leader = new LeaderServiceImpl(TEST_SERVICE_NAME, lockingService, registry);
        leader.addLeadershipLostCallback(() -> triggered.set(true));
        leader.ensureLeadership();
        assertThat("callback should not have been called", !triggered.get());
    }

    private void assertMetricValues(int leaderLock, int processingAllowed) {
        assertThat(registry.getGauges().get(TEST_SERVICE_NAME + "." + LeaderServiceImpl.LEADER_LOCK_METRIC).getValue(), equalTo(leaderLock));
        assertThat(registry.getGauges().get(TEST_SERVICE_NAME + "." + LeaderServiceImpl.PROCESSING_ALLOWED_METRIC).getValue(), equalTo(processingAllowed));
    }
}
