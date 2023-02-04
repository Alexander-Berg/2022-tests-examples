package ru.yandex.solomon.alert.unroll;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;


/**
 * @author Vladimir Gordiychuk
 */
public class UnrollExecutorTest {
    private static final long UNROLL_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1L);
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withTimeout(10, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build();

    private ManualClock clock;
    private ScheduledExecutorService executorService;

    private SolomonClientStub solomon;
    private UnrollExecutorImpl unrollExecutor;

    private static ThresholdAlert.Builder randomActiveAlert() {
        return AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setState(AlertState.ACTIVE);
    }

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(Runtime.getRuntime().availableProcessors(), clock);

        solomon = new SolomonClientStub();
        MetricsClient client = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(client);
        MultiAlertUnrollFactory factory = new MultiAlertUnrollFactoryImpl(cachingMetricsClient);
        unrollExecutor = new UnrollExecutorImpl(
                clock,
                executorService,
                MetricRegistry.root(),
                factory,
                Duration.ofMillis(UNROLL_INTERVAL_MILLIS),
                Duration.ofSeconds(30)
        );
    }

    @After
    public void tearDown() throws Exception {
        if (unrollExecutor != null) {
            unrollExecutor.close();
        }

        if (solomon != null) {
            solomon.close();
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void assignAndUnrollEmpty() throws Exception {
        String labels = "project=solomon, cluster=local, service=test, sensor=idleTime";
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors(labels + ", host=*")
                .setGroupByLabel("host")
                .build();

        ConsumerStub consumer = assignAndUnroll(parent);
        assertThat(consumer.labels, iterableWithSize(0));
    }

    @Test
    public void unrollStaticLabels() throws InterruptedException {
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "local",
            "service", "test",
            "sensor", "idleTime");

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());

        ConsumerStub consumer = assignAndUnroll(parent);
        assertThat(consumer.labels, equalTo(
                ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2"))));
    }

    @Test
    public void unrollNow() throws InterruptedException {
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "local",
            "service", "test",
            "sensor", "idleTime");

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());

        ConsumerStub consumer = new ConsumerStub();
        unrollExecutor.unrollNow(parent, consumer);
        consumer.syncUnroll.await();

        assertThat(consumer.labels, equalTo(
                ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2"))));

        // await next unrolling
        solomon.addMetric(labels.add("host", "solomon-3"), AggrGraphDataArrayList.empty());
        awaitUnroll(consumer);

        assertThat(consumer.labels, equalTo(
                ImmutableSet.of(
                        Labels.of("host", "solomon-1"),
                        Labels.of("host", "solomon-2"),
                        Labels.of("host", "solomon-3"))));
    }

    @Test
    public void unrollNewLabels() throws Exception {
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        Set<Labels> expected = new HashSet<>();
        ConsumerStub consumer = assignAndUnroll(parent);
        assertThat(consumer.labels, iterableWithSize(0));

        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "local",
            "service", "test",
            "sensor", "idleTime");

        for (int index = 0; index < 3; index++) {
            expected.add(Labels.of("host", "solomon-" + index));
            solomon.addMetric(labels.add("host", "solomon-" + index), AggrGraphDataArrayList.empty());
            awaitUnroll(consumer);
            assertThat(consumer.labels, equalTo(expected));
        }
    }

    @Test
    public void unrollDeleteLabels() throws Exception {
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        ConsumerStub consumer = assignAndUnroll(parent);
        assertThat(consumer.labels.size(), equalTo(0));

        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "local",
            "service", "test",
            "sensor", "idleTime");

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-3"), AggrGraphDataArrayList.empty());
        awaitUnroll(consumer);
        assertThat(consumer.labels.size(), equalTo(3));

        solomon.removeMetrics(Selectors.parse("host=solomon-2|solomon-3"));
        awaitUnroll(consumer);
        assertThat(consumer.labels, equalTo(ImmutableSet.of(Labels.of("host", "solomon-1"))));
    }

    @Test
    public void rescheduleAfterUpdate() throws Exception {
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, pid=*, host=*")
                .setGroupByLabel("host")
                .build();

        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "local",
            "service", "test",
            "sensor", "idleTime");

        solomon.addMetric(labels.add("pid", "1").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("pid", "1").add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("pid", "2").add("host", "solomon-3"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("pid", "3").add("host", "solomon-3"), AggrGraphDataArrayList.empty());

        ConsumerStub consumerV1 = assignAndUnroll(parent);
        assertThat(consumerV1.labels, iterableWithSize(3));

        ThresholdAlert update = parent.toBuilder()
                .setVersion(parent.getVersion())
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, pid=1, host=*")
                .build();
        ConsumerStub consumerV2 = assignAndUnroll(update);
        assertThat(consumerV2.labels,
                equalTo(
                        ImmutableSet.of(
                                Labels.of("host", "solomon-1"),
                                Labels.of("host", "solomon-2"))
                ));
    }

    @Test
    public void stopUnrollingAfterCancel() throws Exception {
        ThresholdAlert parent = randomActiveAlert()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "local",
            "service", "test",
            "sensor", "idleTime");

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        ConsumerStub consumer = new ConsumerStub();
        unrollExecutor.unroll(parent, consumer);
        awaitUnroll(consumer);

        consumer.canceled = true;
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-3"), AggrGraphDataArrayList.empty());

        boolean result = awaitUnroll(consumer, 50, TimeUnit.MILLISECONDS);
        assertThat(result, equalTo(false));
        assertThat(consumer.labels, equalTo(ImmutableSet.of(Labels.of("host", "solomon-1"))));
    }

    @Test
    public void unrollProjectOptional() throws InterruptedException {
        ThresholdAlert parent = randomActiveAlert()
                .setProjectId("solomon")
                .setSelectors("sensor='jvm.gc.timeMs', gc='PS MarkSweep'")
                .setGroupByLabels(ImmutableSet.of("cluster", "service", "host"))
                .build();

        Labels metric = Labels.of("sensor", "jvm.gc.timeMs", "gc", "PS MarkSweep");

        Labels include = Labels.of("cluster", "pre", "service", "stockpile", "host", "solomon-1");
        solomon.addMetric(include.addAll(metric).add("project", "solomon"), AggrGraphDataArrayList.empty());

        Labels exclude = Labels.of("cluster", "gr", "service", "storage", "host", "graphite-1");
        solomon.addMetric(exclude.addAll(metric).add("project", "graphite"), AggrGraphDataArrayList.empty());

        ConsumerStub consumer = new ConsumerStub();
        unrollExecutor.unroll(parent, consumer);
        awaitUnroll(consumer);
        assertThat(consumer.labels, hasItem(include));
        assertThat(consumer.labels, not(hasItem(exclude)));
    }

    private ConsumerStub assignAndUnroll(Alert alert) throws InterruptedException {
        ConsumerStub consumer = new ConsumerStub();
        unrollExecutor.unroll(alert, consumer);
        awaitUnroll(consumer);
        return consumer;
    }

    private void awaitUnroll(ConsumerStub consumer) throws InterruptedException {
        CountDownLatch sync = consumer.syncUnroll;
        clock.passedTime(1, TimeUnit.MINUTES);
        while (!sync.await(10, TimeUnit.MILLISECONDS)) {
            clock.passedTime(30, TimeUnit.SECONDS);
        }
    }

    private boolean awaitUnroll(ConsumerStub consumer, long time, TimeUnit unit) throws InterruptedException {
        CountDownLatch sync = consumer.syncUnroll;
        clock.passedTime(1, TimeUnit.MINUTES);
        return sync.await(time, unit);
    }

    private static class ConsumerStub implements UnrollExecutor.UnrollConsumer {
        private volatile Set<Labels> labels;
        private volatile CountDownLatch syncUnroll = new CountDownLatch(1);
        private volatile boolean canceled;

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void accept(UnrollResult result) {
            this.labels = result.labels;
            CountDownLatch prev = this.syncUnroll;
            this.syncUnroll = new CountDownLatch(1);
            prev.countDown();
        }
    }
}
