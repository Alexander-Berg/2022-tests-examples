package ru.yandex.solomon.alert.rule.threshold;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.FluentPredicate;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.AlertRuleFactory;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryImpl;
import ru.yandex.solomon.alert.rule.AlertRuleFairDeadlines;
import ru.yandex.solomon.alert.rule.AlertTimeSeries;
import ru.yandex.solomon.alert.rule.SimulationResult;
import ru.yandex.solomon.alert.rule.SimulationStatus;
import ru.yandex.solomon.alert.rule.usage.ProjectAlertRuleMetrics;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.TemplateFactory;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.metrics.client.CrossDcMetricsClient;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCache;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.Timeline;
import ru.yandex.solomon.util.time.Interval;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ThresholdAlertSimulatorTest {
    @Rule
    public TestName testName = new TestName();
    private SolomonClientStub solomon;
    private MetricsClient metricsClient;
    private AlertRuleFactory alertRuleFactory;
    private FindCacheOptions cacheOptions;
    private Duration gridStep;

    private ThresholdAlert.Builder newThresholdAlert() {
        return newThresholdAlert(testName.getMethodName());
    }

    private ThresholdAlert.Builder newThresholdAlert(String id) {
        return ThresholdAlert.newBuilder()
            .setId(id)
            .setProjectId("junk");
    }

    @Before
    public void setUp() throws Exception {
        solomon = new SolomonClientStub();
        metricsClient = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        TemplateFactory templateFactory = new MustacheTemplateFactory();
        cacheOptions = FindCacheOptions.newBuilder()
            .setMaxSize(1000)
            .setExpireTtl(0, TimeUnit.MILLISECONDS)
            .setRefreshInterval(0, TimeUnit.MILLISECONDS)
            .build();

        MetabaseFindCache metabaseFindCache = new MetabaseFindCacheImpl(metricsClient, cacheOptions);
        MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(metricsClient, metabaseFindCache);
        var featureFlags = new FeatureFlagHolderStub();
        alertRuleFactory = new AlertRuleFactoryImpl(
                cachingMetricsClient,
                new ProjectAlertRuleMetrics(),
                templateFactory,
                null,
                featureFlags);

        Labels common = Labels.of("project", "test", "cluster", "test", "service", "test");
        Labels usage = common.add("sensor", "cpu.usage");
        Labels empty = common.add("sensor", "empty");
        Labels firstLabels = usage.add("host", "pluto");
        Labels secondLabels = usage.add("host", "neptune");

        AggrGraphDataArrayList firstSeries = new AggrGraphDataArrayList();
        AggrGraphDataArrayList secondSeries = new AggrGraphDataArrayList();

        long from = Instant.parse("2019-11-30T00:00:00Z").toEpochMilli();
        long to = Instant.parse("2019-12-03T00:00:00Z").toEpochMilli();
        long step = Duration.ofSeconds(15).toMillis();

        double w20m = 2 * Math.PI / Duration.ofMinutes(20).toMillis();
        double w30m = 2 * Math.PI / Duration.ofMinutes(30).toMillis();
        double w3h = 2 * Math.PI / Duration.ofHours(3).toMillis();
        double w5h = 2 * Math.PI / Duration.ofHours(5).toMillis();
        double w8h = 2 * Math.PI / Duration.ofHours(8).toMillis();
        double w12h = 2 * Math.PI / Duration.ofHours(12).toMillis();

        Interval firstOff = new Interval(Instant.parse("2019-12-01T04:18:05Z"), Instant.parse("2019-12-01T14:22:35Z"));
        Interval secondOff = new Interval(Instant.parse("2019-12-01T08:14:25Z"), Instant.parse("2019-12-01T10:51:03Z"));

        for (long t = from; t < to; t += step) {
            Instant at = Instant.ofEpochMilli(t);

            double firstVal = 50 + 10 * Math.sin(w12h * t) + 14 * Math.sin(w3h * t) + 20 * Math.cos(w5h * t) + Math.sin(w30m * t);
            double secondVal = 50 + 10 * Math.sin(w8h * t) + 14 * Math.sin(w5h * t) + 20 * Math.cos(w3h * t) + Math.cos(w20m * t);

            if (!firstOff.containsOpenClose(at)) {
                firstSeries.addRecordShort(t, firstVal);
            }
            if (!secondOff.containsOpenClose(at)) {
                secondSeries.addRecordShort(t, secondVal);
            }
        }

        solomon.addMetric(empty, AggrGraphDataArrayList.empty());
        solomon.addMetric(firstLabels, firstSeries);
        solomon.addMetric(secondLabels, secondSeries);

        gridStep = Duration.ofMinutes(17);
    }

    @After
    public void tearDown() throws Exception {
        solomon.close();
    }

    private SimulationResult syncSimulate(ThresholdAlert alert, Instant from, Instant to, Duration gridStep) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        return makeRule(alert).simulate(from, to, gridStep, deadlines).join();
    }

    private SimulationResult syncSimulate(AlertRule rule, Instant from, Instant to, Duration gridStep) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        return rule.simulate(from, to, gridStep, deadlines).join();
    }

    private AlertRule makeRule(ThresholdAlert alert) {
        return alertRuleFactory.createAlertRule(alert);
    }

    private CompletableFuture<EvaluationStatus.Code> eval(AlertRule rule, Instant at) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        return rule.eval(at, deadlines).thenApply(EvaluationStatus::getCode);
    }

    @Test
    public void missing() {
        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=test, cluster=test, service=test, sensor=missing")
            .setPredicateRule(FluentPredicate.when(ThresholdType.AVG).is(Compare.GTE).than(13).signal(TargetStatus.ALARM))
            .setPeriod(Duration.ofMinutes(30))
            .build();

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-02T00:00:00Z");
        Duration gridStep = Duration.ofMinutes(4);

        // GCD(gridStep, period) = 2m

        SimulationResult result = syncSimulate(alert, from, to, gridStep);

        assertThat(result.getCode(), equalTo(SimulationStatus.NO_METRICS));
    }

    private static GraphData lineToGraphData(AlertTimeSeries line) {
        return NamedGraphData.of(AggrGraphDataArrayList.of(line.getSource())).getGraphData();
    }

    @Test
    public void empty() {
        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=test, cluster=test, service=test, sensor=empty")
            .setPredicateRule(FluentPredicate.when(ThresholdType.AVG).is(Compare.GTE).than(42).signal(TargetStatus.ALARM))
            .setPeriod(Duration.ofMinutes(30))
            .build();

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-02T00:00:00Z");
        Duration gridStep = Duration.ofMinutes(4);

        // GCD(gridStep, period) = 2m

        SimulationResult result = syncSimulate(alert, from, to, gridStep);

        assertThat(result.getCode(), equalTo(SimulationStatus.OK));
        assertThat(result.getResultingLines().size(), equalTo(1));
        assertThat(result.getResultingLines().get(0).getLabels(), equalTo(Labels.of(
            "project", "test", "cluster", "test", "service", "test", "sensor", "empty"
        )));
        assertThat(result.getResultingLines().get(0).getSource().getRecordCount(), equalTo(360));
        GraphData gd = lineToGraphData(result.getResultingLines().get(0));
        assertTrue(gd.getValues().stream().allMatch(Double::isNaN));
    }

    @Test
    public void emptyCrossDc() {
        MetricsClient metricsClientSas = new DcMetricsClient("sas", solomon.getMetabase(), solomon.getStockpile());
        MetricsClient metricsClientVla = new DcMetricsClient("vla", solomon.getMetabase(), solomon.getStockpile());
        metricsClient = new CrossDcMetricsClient(Map.of("sas", metricsClientSas, "vla", metricsClientVla));
        MetabaseFindCache metabaseFindCache = new MetabaseFindCacheImpl(metricsClient, cacheOptions);
        MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(metricsClient, metabaseFindCache);
        var featureFlags = new FeatureFlagHolderStub();
        alertRuleFactory = new AlertRuleFactoryImpl(
                cachingMetricsClient,
                new ProjectAlertRuleMetrics(),
                new MustacheTemplateFactory(),
                null,
                featureFlags);

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=test, cluster=test, service=test, sensor=empty")
            .setPredicateRule(FluentPredicate.when(ThresholdType.AVG).is(Compare.GTE).than(42).signal(TargetStatus.ALARM))
            .setPeriod(Duration.ofMinutes(30))
            .build();

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-02T00:00:00Z");
        Duration gridStep = Duration.ofMinutes(4);

        // GCD(gridStep, period) = 2m

        SimulationResult result = syncSimulate(alert, from, to, gridStep);

        assertThat(result.getCode(), equalTo(SimulationStatus.OK));
        assertThat(result.getResultingLines().size(), equalTo(1));
        assertThat(result.getResultingLines().get(0).getLabels(), equalTo(Labels.of(
            "project", "test", "cluster", "test", "service", "test", "sensor", "empty"
        )));
        assertThat(result.getResultingLines().get(0).getSource().getRecordCount(), equalTo(360));
        GraphData gd = lineToGraphData(result.getResultingLines().get(0));
        assertTrue(gd.getValues().stream().allMatch(Double::isNaN));
    }

    private void twoLines(ThresholdType thresholdType, double warn, double alarm) {
        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=test, cluster=test, service=test, sensor=cpu.usage")
            .setPredicateRules(Stream.of(
                FluentPredicate.when(thresholdType).is(Compare.GT).than(alarm).signal(TargetStatus.ALARM),
                FluentPredicate.when(thresholdType).is(Compare.GT).than(warn).signal(TargetStatus.WARN)
            ))
            .setPeriod(Duration.ofMinutes(30))
            .setDelaySeconds(300)
            .build();

        AlertRule rule = makeRule(alert);

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-02T00:00:00Z");

        // GCD(gridStep, period, delay) = 1m

        SimulationResult result = syncSimulate(rule, from, to, gridStep);

        GraphData first = lineToGraphData(result.getResultingLines().get(0));
        GraphData second = lineToGraphData(result.getResultingLines().get(1));

        Timeline timeline = first.getTimeline();

        List<EvaluationStatus.Code> direct = timeline.getPointsMillis().stream()
            .mapToObj(ts -> eval(rule, Instant.ofEpochMilli(ts)))
            .collect(collectingAndThen(toList(), CompletableFutures::allOf))
            .join();

        assertThat(result.getStatuses(), equalTo(direct));

        HashSet<EvaluationStatus.Code> distinctStatuses = new HashSet<>(result.getStatuses());
        assertThat(distinctStatuses, containsInAnyOrder(
            EvaluationStatus.Code.OK,
            EvaluationStatus.Code.ALARM,
            EvaluationStatus.Code.WARN,
            EvaluationStatus.Code.NO_DATA
        ));

        for (int i = 0; i < timeline.getPointCount(); i++) {
            if (result.getStatuses().get(i) == EvaluationStatus.Code.NO_DATA) {
                assertThat("At point " + i, first.getValues().at(i), equalTo(Double.NaN));
                assertThat("At point " + i, second.getValues().at(i), equalTo(Double.NaN));
            }

            if (!Double.isNaN(first.getValues().at(i)) || !Double.isNaN(second.getValues().at(i))) {
                assertThat("At point " + i, result.getStatuses().get(i), not(equalTo(EvaluationStatus.Code.NO_DATA)));
            }
        }
    }

    @Test
    public void hugeInterval() {
        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=test, cluster=test, service=test, sensor=cpu.usage")
            .setPredicateRules(Stream.of(
                FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(80).signal(TargetStatus.ALARM),
                FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(65).signal(TargetStatus.WARN)
            ))
            .setPeriod(Duration.ofMinutes(30))
            .setDelaySeconds(300)
            .build();

        Instant from = Instant.parse("2019-02-18T07:58:40Z");
        Instant to = Instant.parse("2019-12-08T17:06:31Z");
        Duration gridStep = Duration.ofMillis(253480716L);

        // GCD(gridStep, period, delay) = 1m

        SimulationResult result = syncSimulate(alert, from, to, gridStep);

        assertThat(result.getCode(), equalTo(SimulationStatus.DATA_LOAD_ERROR));
    }

    @Test
    public void lastPointIsNotTruncated() {
        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=test, cluster=test, service=test, sensor=cpu.usage")
                .setPredicateRules(Stream.of(
                        FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(80).signal(TargetStatus.ALARM),
                        FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(65).signal(TargetStatus.WARN)
                ))
                .setPeriod(Duration.ofMinutes(30))
                .setDelaySeconds(300)
                .build();

        Instant from = Instant.parse("2019-11-30T12:55:18Z");
        Instant to = Instant.parse("2019-11-30T13:55:18Z");
        Duration gridStep = Duration.ofMinutes(1);

        SimulationResult result = syncSimulate(alert, from, to, gridStep);

        var iterator = result.getResultingLines().get(0).getSource().iterator();
        RecyclableAggrPoint point = RecyclableAggrPoint.newInstance();
        while (iterator.next(point)) {
            // no op
        }
        assertThat(point.tsMillis, equalTo(to.truncatedTo(ChronoUnit.MINUTES).toEpochMilli()));
        point.recycle();
    }

    @Test
    public void twoLinesAvg() {
        twoLines(ThresholdType.AVG, 65, 80);
    }

    @Test
    public void twoLinesMax() {
        twoLines(ThresholdType.MAX, 65, 80);
    }

    @Test
    public void twoLinesMin() {
        twoLines(ThresholdType.MIN, 65, 80);
    }

    @Test
    public void twoLinesCount() {
        twoLines(ThresholdType.COUNT, 80, 100);
    }

    @Test
    public void twoLinesSum() {
        twoLines(ThresholdType.SUM, 7000, 9000);
    }

    @Test
    public void transformation() {
        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=test, cluster=test, service=test, sensor=cpu.usage")
            .setPredicateRules(Stream.of(
                FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(65).signal(TargetStatus.ALARM),
                FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(55).signal(TargetStatus.WARN)
            ))
            .setTransformations("0.5 * group_lines('sum', input{})")
            .setPeriod(Duration.ofMinutes(30))
            .setDelaySeconds(300)
            .build();

        AlertRule rule = makeRule(alert);

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-02T00:00:00Z");

        // GCD(gridStep, period, delay) = 1m

        SimulationResult result = syncSimulate(rule, from, to, gridStep);

        assertThat(result.getResultingLines(), iterableWithSize(1));
        GraphData halftotal = lineToGraphData(result.getResultingLines().get(0));

        Timeline timeline = halftotal.getTimeline();

        List<EvaluationStatus.Code> direct = timeline.getPointsMillis().stream()
            .mapToObj(ts -> eval(rule, Instant.ofEpochMilli(ts)))
            .collect(collectingAndThen(toList(), CompletableFutures::allOf))
            .join();

        assertThat(result.getStatuses(), equalTo(direct));

        HashSet<EvaluationStatus.Code> distinctStatuses = new HashSet<>(result.getStatuses());
        assertThat(distinctStatuses, containsInAnyOrder(
            EvaluationStatus.Code.OK,
            EvaluationStatus.Code.ALARM,
            EvaluationStatus.Code.WARN,
            EvaluationStatus.Code.NO_DATA
        ));

        for (int i = 0; i < timeline.getPointCount(); i++) {
            if (result.getStatuses().get(i) == EvaluationStatus.Code.NO_DATA) {
                assertThat("At point " + i, halftotal.getValues().at(i), equalTo(Double.NaN));
            }

            if (!Double.isNaN(halftotal.getValues().at(i))) {
                assertThat("At point " + i, result.getStatuses().get(i), not(equalTo(EvaluationStatus.Code.NO_DATA)));
            }
        }
    }

    @Test
    public void countWithLargeGrid() {
        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=test, cluster=test, service=test, sensor=test")
                .setPredicateRules(Stream.of(
                        FluentPredicate.when(ThresholdType.AVG).is(Compare.GT).than(0).signal(TargetStatus.ALARM)
                ))
                .setTransformations("diff(input{})")
                .setPeriod(Duration.ofMinutes(5))
                .setDelaySeconds(0)
                .build();

        AlertRule rule = makeRule(alert);

        Instant from = Instant.parse("2020-06-29T18:42:39.000Z");
        Instant to = Instant.parse("2020-06-30T23:03:12.000Z");

        AggrGraphDataArrayList source = new AggrGraphDataArrayList();
        int k = 0;
        for (Instant i = from.truncatedTo(ChronoUnit.MINUTES).minusSeconds(300); i.isBefore(to); i = i.plusSeconds(60)) {
            source.addRecordShort(i.toEpochMilli(), Math.sin(3 * k));
            k++;
        }
        Labels common = Labels.of("project", "test", "cluster", "test", "service", "test");
        solomon.addMetric(common.add("sensor", "test"), source);

        SimulationResult result = syncSimulate(rule, from, to, Duration.ofMillis(2748333));

        assertThat(result.getResultingLines(), iterableWithSize(1));
        GraphData diffAvg = lineToGraphData(result.getResultingLines().get(0));

        var values = Arrays.stream(diffAvg.getValues().toArray()).boxed().toArray();
        assertThat(values, not(arrayContaining(Double.NaN)));
    }

    @Test
    public void transformWithLargeGrid() {
        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=test, cluster=test, service=test, sensor=uptime")
                .setPredicateRules(Stream.of(
                        FluentPredicate.when(ThresholdType.COUNT).is(Compare.LT).than(90).signal(TargetStatus.ALARM)
                ))
                .setPeriod(Duration.ofMinutes(5))
                .build();

        AlertRule rule = makeRule(alert);

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-02T00:00:00Z");

        AggrGraphDataArrayList series = new AggrGraphDataArrayList();
        int val = 5762;
        for (Instant t = from.minusSeconds(1003); t.isBefore(to); t = t.plusSeconds(5)) {
            series.addRecordShort(t.toEpochMilli(), val++);
        }

        Labels common = Labels.of("project", "test", "cluster", "test", "service", "test");

        solomon.addMetric(common.add("sensor", "uptime"), series);

        for (int gridMinutes : new int[] {1, 15, 30, 120}) {
            SimulationResult result = syncSimulate(rule, from, to, Duration.ofMinutes(gridMinutes));

            AggrGraphDataArrayList list = AggrGraphDataArrayList.of(result.getResultingLines().get(0).getSource());
            Assert.assertTrue("With grid = " + gridMinutes + "m",
                    list.toGraphDataShort().getValues().allAre(60d));
        }
    }

    @Test
    public void badTransformation() {
        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=test, cluster=test, service=test, sensor=uptime")
                .setTransformations("histogram_sum(input{})")
                .setPredicateRules(Stream.of(
                        FluentPredicate.when(ThresholdType.COUNT).is(Compare.LT).than(90).signal(TargetStatus.ALARM)
                ))
                .setPeriod(Duration.ofMinutes(5))
                .build();

        AlertRule rule = makeRule(alert);

        Instant from = Instant.parse("2019-12-01T00:00:00Z");
        Instant to = Instant.parse("2019-12-01T01:00:00Z");

        AggrGraphDataArrayList series = new AggrGraphDataArrayList();
        int val = 5762;
        for (Instant t = from.minusSeconds(1003); t.isBefore(to); t = t.plusSeconds(60)) {
            series.addRecordShort(t.toEpochMilli(), val++);
        }

        Labels common = Labels.of("project", "test", "cluster", "test", "service", "test");

        solomon.addMetric(common.add("sensor", "uptime"), series);

        SimulationResult result = syncSimulate(rule, from, to, Duration.ofMinutes(60_000));

        Assert.assertEquals(SimulationStatus.INVALID_REQUEST, result.getCode());
    }
}
