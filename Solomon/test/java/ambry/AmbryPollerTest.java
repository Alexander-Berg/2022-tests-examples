package ru.yandex.solomon.alert.ambry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import ru.yandex.ambry.AmbryClient;
import ru.yandex.ambry.AmbryClientStub;
import ru.yandex.ambry.dto.LastUpdatedResponse;
import ru.yandex.ambry.dto.YasmAlertDto;
import ru.yandex.monlib.metrics.MetricConsumer;
import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.histogram.HistogramSnapshot;
import ru.yandex.monlib.metrics.labels.Label;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.labels.LabelsBuilder;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.client.stub.AlertApiStub;
import ru.yandex.solomon.flags.FeatureFlag;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.locks.ReadOnlyDistributedLockStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;
import ru.yandex.solomon.util.host.HostUtils;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class AmbryPollerTest {

    @Rule
    public TestRule timeout = new DisableOnDebug(Timeout.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build());

    private MetricRegistry registry;
    private FeatureFlagHolderStub flagHolderStub;
    private ManualClock manualClock;
    private ManualScheduledExecutorService scheduledExecutorService;
    private ReadOnlyDistributedLockStub distributedLock;
    private AmbryClientStub ambryClient;
    private AlertApiStub alertApi;

    private static class AmbryClientProxy implements AmbryClient {
        private final AmbryClient backend;
        private final Hooks hooks;

        public interface Hooks {
            default void onList(TagFormat tagFormat, int limit, int offset, List<YasmAlertDto> response, Throwable t) { }
            default void onLastUpdatedTotal(LastUpdatedResponse response, Throwable t) { }
        }

        public AmbryClientProxy(AmbryClient backend, Hooks hooks) {
            this.backend = backend;
            this.hooks = hooks;
        }

        @Override
        public CompletableFuture<List<YasmAlertDto>> list(TagFormat tagFormat, int limit, int offset) {
            return backend.list(tagFormat, limit, offset)
                    .whenComplete((r, t) -> hooks.onList(tagFormat, limit, offset, r, t));
        }

        @Override
        public CompletableFuture<LastUpdatedResponse> lastUpdatedTotal() {
            return backend.lastUpdatedTotal()
                    .whenComplete(hooks::onLastUpdatedTotal);
        }
    }

    private static class UpdateTracker {
        private long lastTrackedUpdate = 0;

        public void waitForUpdate(AmbryPoller poller) throws InterruptedException {
            long lastUpdated;
            while ((lastUpdated = poller.getLastAmbryUpdatedMillis()) == lastTrackedUpdate) {
                Thread.sleep(5);
            }
            lastTrackedUpdate = lastUpdated;
        }
    }

    @Before
    public void setUp() {
        registry = new MetricRegistry();
        flagHolderStub = new FeatureFlagHolderStub();
        manualClock = new ManualClock();
        scheduledExecutorService = new ManualScheduledExecutorService(2, manualClock);
        distributedLock = new ReadOnlyDistributedLockStub(new ManualClock());
        distributedLock.setOwner(HostUtils.getFqdn());
        ambryClient = new AmbryClientStub(manualClock);
        alertApi = new AlertApiStub();
    }

    @Test
    public void periodic() throws InterruptedException {
        CountDownLatch[] list = new CountDownLatch[] {new CountDownLatch(1)};
        CountDownLatch[] updated = new CountDownLatch[] {new CountDownLatch(1)};

        var ambryClientProxy = new AmbryClientProxy(ambryClient, new AmbryClientProxy.Hooks() {
            @Override
            public void onList(AmbryClient.TagFormat tagFormat, int limit, int offset, List<YasmAlertDto> response,
                               Throwable t)
            {
                list[0].countDown();
            }

            @Override
            public void onLastUpdatedTotal(LastUpdatedResponse response, Throwable t) {
                updated[0].countDown();
            }
        });

        new AmbryPoller(
                manualClock,
                registry,
                flagHolderStub,
                "yasm_",
                alertApi, // sync is disabled for all, so alert API is not called
                ambryClientProxy,
                scheduledExecutorService,
                distributedLock
        );
        manualClock.passedTime(5, TimeUnit.SECONDS);
        updated[0].await();
        list[0].await();

        list[0] = new CountDownLatch(1);
        ambryClient.add(cons("example", "upper", "unistat-errors_dmmm"));
        updated[0] = new CountDownLatch(3);
        for (int i = 0; i < 60; i++) {
            manualClock.passedTime(1, TimeUnit.SECONDS);
            Thread.sleep(5);
        }
        updated[0].await();
        list[0].await();
    }

    @Test
    public void notEnabled() throws InterruptedException {
        UpdateTracker tracker = new UpdateTracker();
        ambryClient.add(cons("example", "upper", "unistat-errors_dmmm"));

        var ambryPoller = new AmbryPoller(
                manualClock,
                registry,
                flagHolderStub,
                "yasm_",
                alertApi, // sync is disabled for all, so alert API is not called
                ambryClient,
                scheduledExecutorService,
                distributedLock
        );

        manualClock.passedTime(1, TimeUnit.SECONDS);

        tracker.waitForUpdate(ambryPoller);
    }

    @Test
    public void enabledCreate() throws InterruptedException {
        UpdateTracker tracker = new UpdateTracker();
        ambryClient.add(cons("example", "upper", "unistat-errors_dmmm"));
        flagHolderStub.setFlag("yasm_upper", FeatureFlag.SYNC_AMBRY_ALERTS, true);

        var ambryPoller = new AmbryPoller(
                manualClock,
                registry,
                flagHolderStub,
                "yasm_",
                alertApi,
                ambryClient,
                scheduledExecutorService,
                distributedLock
        );

        manualClock.passedTime(1, TimeUnit.SECONDS);

        tracker.waitForUpdate(ambryPoller);
    }

    @Test
    public void forceReload() throws InterruptedException {
        CountDownLatch[] list = new CountDownLatch[] {new CountDownLatch(3)};

        var ambryClientProxy = new AmbryClientProxy(ambryClient, new AmbryClientProxy.Hooks() {
            @Override
            public void onList(AmbryClient.TagFormat tagFormat, int limit, int offset, List<YasmAlertDto> response,
                               Throwable t)
            {
                list[0].countDown();
            }
        });

        ambryClient.add(cons("example", "upper", "unistat-errors_dmmm"));

        new AmbryPoller(
                manualClock,
                registry,
                flagHolderStub,
                "yasm_",
                alertApi,
                ambryClientProxy,
                scheduledExecutorService,
                distributedLock
        );

        for (int i = 0; i < 200; i++) {
            manualClock.passedTime(10, TimeUnit.SECONDS);
            Thread.sleep(1);
        }
        list[0].await();
    }

    @Test
    public void enabledCreateUpdateDelete() throws InterruptedException {
        flagHolderStub.setFlag("yasm_upper", FeatureFlag.SYNC_AMBRY_ALERTS, true);

        UpdateTracker tracker = new UpdateTracker();

        var ambryPoller = new AmbryPoller(
                manualClock,
                registry,
                flagHolderStub,
                "yasm_",
                alertApi,
                ambryClient,
                scheduledExecutorService,
                distributedLock
        );

        ambryClient.add(cons("example", "upper", "unistat-errors_dmmm"));
        manualClock.passedTime(90, TimeUnit.SECONDS);
        tracker.waitForUpdate(ambryPoller);

        ambryClient.add(cons("example2", "upper", "unistat-errors_dmmm"));
        manualClock.passedTime(90, TimeUnit.SECONDS);
        tracker.waitForUpdate(ambryPoller);

        ambryClient.remove("example2");
        manualClock.passedTime(90, TimeUnit.SECONDS);
        tracker.waitForUpdate(ambryPoller);

        MutableLong converted = new MutableLong();
        MutableLong created = new MutableLong();
        MutableLong updated = new MutableLong();
        MutableLong deleted = new MutableLong();

        registry.supply(0, new MetricConsumer() {
            private LabelsBuilder labelsBuilder;
            private Labels labels;

            public void onStreamBegin(int countHint) { }
            public void onStreamEnd() { }
            public void onCommonTime(long tsMillis) { }
            public void onMetricBegin(MetricType type) { }
            public void onMetricEnd() { }
            public void onLabelsBegin(int countHint) {
                labelsBuilder = new LabelsBuilder(countHint);
            }
            public void onLabelsEnd() {
                labels = labelsBuilder.build();
            }
            public void onLabel(Label label) {
                labelsBuilder.add(label);
            }
            public void onDouble(long tsMillis, double value) { }
            public void onLong(long tsMillis, long value) {
                Label label = labels.findByKey("projectId");
                if (label == null || !label.getValue().equals("yasm_upper")) {
                    return;
                }
                Label metric = labels.findByKey("sensor");
                Objects.requireNonNull(metric);
                Label status = labels.findByKey("status");
                if (metric.getValue().equals("ambryPoller.convertedSuccessfully")) {
                    converted.setValue(value);
                }
                if (metric.getValue().equals("ambryPoller.updateStatus")) {
                    Objects.requireNonNull(status);
                    if (status.getValue().equals("OK")) {
                        updated.setValue(value);
                    }
                }
                if (metric.getValue().equals("ambryPoller.createStatus")) {
                    Objects.requireNonNull(status);
                    if (status.getValue().equals("OK")) {
                        created.setValue(value);
                    }
                }
                if (metric.getValue().equals("ambryPoller.deleteStatus")) {
                    Objects.requireNonNull(status);
                    if (status.getValue().equals("OK")) {
                        deleted.setValue(value);
                    }
                }
            }
            public void onHistogram(long tsMillis, HistogramSnapshot snapshot) { }
        });

        Assert.assertTrue(converted.getValue() >= 4);
        Assert.assertTrue(created.getValue() >= 2);
        Assert.assertTrue(updated.getValue() >= 2);
        Assert.assertTrue(deleted.getValue() >= 1);
    }

    private YasmAlertDto cons(String name, String itype, String signal) {
        YasmAlertDto alert = new YasmAlertDto();
        alert.name = name;
        alert.tags = Map.of("itype", List.of(itype));
        alert.signal = signal;
        alert.warn = List.of(10.0, 42.0);
        alert.crit = List.of(42.0, 100500.0);
        return alert;
    }

}
