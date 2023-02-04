package ru.yandex.solomon.alert.unroll;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class ExpressionMultiAlertUnrollTest {
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
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("")
                .setCheckExpression("max({project=solomon, cluster=local, service=test, sensor=idleTime}) > 10")
                .setGroupByLabel("host")
                .build();

        Set<Labels> result = resolved(alert);
        assertThat(result, iterableWithSize(0));
    }

    @Test
    public void resolveGroups() throws Exception {
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test","sensor", "idleTime");
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("")
                .setCheckExpression("avg(" + labels + ") >= 100500")
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
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test","sensor", "idleTime");
        String labelsStr = LabelsFormat.format(labels);

        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("let crossPid = {" + labelsStr + ", pid='*'};")
                .setCheckExpression("avg(group_lines('sum', crossPid)) > 123")
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
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test","sensor", "freeSpace");
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("")
                .setCheckExpression("last(" + labels + ") < 10M")
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

    @Test
    public void resolveExactGroups() {
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "stockpile","sensor", "uptime");
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProjectId("solomon")
                .setProgram("")
                .setCheckExpression("last(uptime{cluster='local', service=='stockpile'}) < 1k")
                .setGroupByLabels(Arrays.asList("host", "service"))
                .build();

        solomon.addMetric(labels.add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("host", "solomon-3"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
                iterableWithSize(3),
                hasItem(Labels.of("host", "solomon-1", "service", "stockpile")),
                hasItem(Labels.of("host", "solomon-2", "service", "stockpile")),
                hasItem(Labels.of("host", "solomon-3", "service", "stockpile"))
        ));
    }

    @Test
    public void multiLoadGroup() throws Exception {
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test");
        String labelsStr = LabelsFormat.format(labels);
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram(new StringBuilder()
                        .append("let started = {" + labelsStr + ", sensor=requestStarted, host!=cluster};\n")
                        .append("let completed = {" + labelsStr + ", sensor=requestCompleted, host!=cluster};\n")
                        .append("let inFlight = started - completed;\n")
                        .toString())
                .setCheckExpression("max(inFlight) > 10k")
                .setGroupByLabel("host")
                .build();

        solomon.addMetric(labels.add("sensor", "requestStarted").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("sensor", "requestCompleted").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("sensor", "requestStarted").add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("sensor", "requestCompleted").add("host", "solomon-3"), AggrGraphDataArrayList.empty());


        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
                iterableWithSize(3),
                hasItem(Labels.of("host", "solomon-1")),
                hasItem(Labels.of("host", "solomon-2"))
        ));
    }

    @Test(expected = IllegalStateException.class)
    public void metabaseResponseNotOk() throws Exception {
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("")
                .setCheckExpression("max({project=solomon, cluster=local, service=test, sensor=idleTime}) > 10")
                .setGroupByLabel("host")
                .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.INTERNAL_ERROR);
        Set<Labels> result = resolved(alert);
        fail("When metabase response not OK completable future should be interrupted, otherwise already " +
                "resolved multi alerts will be removed from persistence layer. Result: " + result);
    }

    @Test
    public void excludeMetricFromUnroll() {
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test");
        String labelsStr = LabelsFormat.format(labels);

        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram(new StringBuilder()
                        .append("let source = {" + labelsStr + ", sensor=quoteUsed};\n")
                        .append("let etalon = {" + labelsStr + ", sensor=quoteLimit, host='-'};\n")
                        .append("let free = etalon - source;\n")
                        .toString())
                .setCheckExpression("last(free) < 10k")
                .setGroupByLabel("host")
                .build();

        solomon.addMetric(labels.add("sensor", "quoteUsed").add("host", "solomon-1"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("sensor", "quoteUsed").add("host", "solomon-2"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("sensor", "quoteUsed").add("host", "solomon-3"), AggrGraphDataArrayList.empty());
        solomon.addMetric(labels.add("sensor", "quoteLimit"), AggrGraphDataArrayList.empty());

        Set<Labels> result = resolved(alert);
        assertThat(result, allOf(
                iterableWithSize(3),
                hasItem(Labels.of("host", "solomon-1")),
                hasItem(Labels.of("host", "solomon-2")),
                hasItem(Labels.of("host", "solomon-3"))
        ));
    }

    @Test
    public void unrollProjectOptional() throws InterruptedException {
        ExpressionAlert parent = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProjectId("solomon")
                .setProgram(new StringBuilder()
                        .append("let ms = {sensor='jvm.gc.timeMs', gc='PS MarkSweep'};\n")
                        .toString())
                .setCheckExpression("avg(ms) >= 500")
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
    public void unrollWithPartialExclude() {
        // https://st.yandex-team.ru/SOLOMON-4486
        Labels labels = Labels.of("project", "p","cluster", "c","service", "s");
        ExpressionAlert alert = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setProgram("" +
                                "let data = {project='p', cluster='c', service='s', metric='request_with_tag', event='a|b', ui='*'};\n" +
                                "let total = {project='p', cluster='c', service='s', metric='total_request', event='-', ui='*'};")
                .setCheckExpression("false")
                .setGroupByLabels(ImmutableSet.of("event", "ui"))
                .build();

        List<String> uis = List.of("desktop", "touch");
        List<String> events = List.of("a", "b");
        List<String> browsers = List.of("YB", "GC");

        Labels dataLabels = labels.add("metric", "request_with_tag");
        for (String ui : uis) {
            for (String event : events) {
                    solomon.addMetric(dataLabels.add("event", event).add("ui", ui), AggrGraphDataArrayList.empty());
            }
        }

        Labels totalLabels = labels.add("metric", "total_request");
        for (String ui : uis) {
            for (String browser : browsers) {
                solomon.addMetric(totalLabels.add("ui", ui).add("browser", browser), AggrGraphDataArrayList.empty());
            }
        }

        Set<Labels> result = resolved(alert);
        assertThat(result, not(hasItem(Labels.of("ui", "touch"))));
        assertThat(result, not(hasItem(Labels.of("ui", "desktop"))));
        assertThat(result, hasSize(4));
    }

    @Test
    public void notValidExpression() {
        ExpressionAlert parent = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setProjectId("solomon")
            .setProgram("let ms")
            .setCheckExpression("avg(ms)")
            .setGroupByLabels(ImmutableSet.of("cluster", "service", "host"))
            .build();

        AlertRuleDeadlines deadlines = UnrollDeadlines.of(Instant.now(), 30, TimeUnit.SECONDS);

        var result = factory.create(parent)
            .unroll(deadlines)
            .thenApply(r -> null)
            .exceptionally(e -> e);

        assertNotNull(result);
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
