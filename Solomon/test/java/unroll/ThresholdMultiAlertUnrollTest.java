package ru.yandex.solomon.alert.unroll;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.UnrollDeadlines;
import ru.yandex.solomon.labels.LabelsFormat;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class ThresholdMultiAlertUnrollTest {
    private SolomonClientStub solomon;
    private MultiAlertUnrollFactory factory;

    @Before
    public void setUp() throws Exception {
        solomon = new SolomonClientStub();
        MetricsClient metricsClient = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(metricsClient);
        factory = new MultiAlertUnrollFactoryImpl(cachingMetricsClient);
    }

    @After
    public void tearDown() throws Exception {
        if (solomon != null) {
            solomon.close();
        }
    }

    @Test
    public void absentMetricsBySelector() throws Exception {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        Set<Labels> result = resolved(alert);
        assertThat(result, iterableWithSize(0));
    }

    @Test
    public void resolveGroups() throws Exception {
        Labels labels = Labels.of("project", "solomon", "cluster", "local", "service", "test", "sensor", "idleTime");
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors(LabelsFormat.format(labels) + ", host=*")
                .setGroupByLabel("host")
                .build();

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-3"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
                iterableWithSize(3),
                hasItem(Labels.of("host", "solomon-1")),
                hasItem(Labels.of("host", "solomon-2")),
                hasItem(Labels.of("host", "solomon-3"))
        ));
    }

    @Test
    public void resolveUniqueGroups() throws Exception {
        Labels labels = Labels.of("project", "solomon", "cluster", "local", "service", "test", "sensor", "idleTime");
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors(LabelsFormat.format(labels) + ", pid=*, host=*")
                .setGroupByLabel("host")
                .build();

        solomon.addMetric(labels.add("pid", "1").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("pid", "2").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("pid", "3").add("host", "solomon-2"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
                iterableWithSize(2),
                hasItem(Labels.of("host", "solomon-1")),
                hasItem(Labels.of("host", "solomon-2"))
        ));
    }

    @Test
    public void resolveOnlyExistsGroups() throws Exception {
        Labels labels = Labels.of("project", "solomon", "cluster", "local", "service", "test", "sensor", "freeSpace");
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors(LabelsFormat.format(labels) + ", disk=*, host=*")
                .setGroupByLabels(Arrays.asList("host", "disk"))
                .build();

        solomon.addMetric(labels.add("disk", "sda1").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("disk", "sda2").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("disk", "sda5").add("host", "solomon-2"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
                iterableWithSize(3),
                hasItem(Labels.of("disk", "sda1", "host", "solomon-1")),
                hasItem(Labels.of("disk", "sda2", "host", "solomon-1")),
                hasItem(Labels.of("disk", "sda5", "host", "solomon-2"))
        ));
    }

    @Test(expected = IllegalStateException.class)
    public void metabaseResponseNotOk() throws Exception {
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setSelectors("project=solomon, cluster=local, service=test, sensor=idleTime, host=*")
                .setGroupByLabel("host")
                .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.INTERNAL_ERROR);
        Set<Labels> result = resolved(alert);
        fail("When metabase response not OK completable future should be interrupted, otherwise already " +
                "resolved multi alerts will be removed from persistence layer. Result: " + result);
    }

    @Test
    public void unrollProjectOptional() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProjectId("solomon")
                .setSelectors("sensor='jvm.gc.timeMs', gc='PS MarkSweep'")
                .setGroupByLabels(ImmutableSet.of("cluster", "service", "host"))
                .build();

        Labels metric = Labels.of("sensor", "jvm.gc.timeMs", "gc", "PS MarkSweep");

        Labels include = Labels.of("cluster", "pre", "service", "stockpile", "host", "solomon-1");
        solomon.addMetric(include.addAll(metric).add("project", "solomon"), AggrGraphDataArrayList.empty());

        Labels exclude = Labels.of("cluster", "gr", "service", "storage", "host", "graphite-1");
        solomon.addMetric(exclude.addAll(metric).add("project", "graphite"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(parent);
        Assert.assertThat(result, hasItem(include));
        Assert.assertThat(result, not(hasItem(exclude)));
    }

    @Test
    public void unrollNamed() {
        Labels labels = Labels.of("project", "solomon", "cluster", "local", "service", "test", "sensor", "idleTime");
        ThresholdAlert alert = AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setProjectId("solomon")
            .setSelectors("idleTime{cluster=local, service=test, host=*}")
            .setGroupByLabel("host")
            .build();

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-3"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
            iterableWithSize(3),
            hasItem(Labels.of("host", "solomon-1")),
            hasItem(Labels.of("host", "solomon-2")),
            hasItem(Labels.of("host", "solomon-3"))
        ));
    }

    private Set<Labels> resolved(Alert alert) {
        try {
            AlertRuleDeadlines deadlines = UnrollDeadlines.of(Instant.now(), 30, TimeUnit.SECONDS);
            return factory.create(alert)
                    .unroll(deadlines)
                    .join().labels;
        } catch (CompletionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

}
