package ru.yandex.solomon.alert.cost;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.labels.LabelsFormat;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.stockpile.api.EStockpileStatusCode;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertCostEstimatorTest {
    @Rule
    public TestName name = new TestName();

    private SolomonClientStub solomon;
    private AlertCostEstimator estimator;

    @Before
    public void setUp() throws Exception {
        solomon = new SolomonClientStub();
        estimator = new AlertCostEstimatorImpl(solomon.getMetabase(), solomon.getStockpile(), EstimationOptions.empty());
    }

    @After
    public void tearDown() throws Exception {
        solomon.close();
    }

    @Test
    public void thresholdAlertHaveDefaultCost() throws Exception {
        Alert alert = newThresholdAlert()
                .setName(name.getMethodName())
                .setSelectors("project=solomon, cluster=slice05, service=stockpile, host=kikimr-01, sensor=idleTime")
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(123)
                        .withComparison(Compare.EQ)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE))
                .build();

        double result = estimate(alert);
        assertThat(result, allOf(not(equalTo(Double.NaN)), greaterThan(0d)));
    }

    @Test
    public void expressionAlertHaveDefaultCost() throws Exception {
        Alert alert = newExpressionAlert()
                .setName(name.getMethodName())
                .setCheckExpression("avg(group_lines('max', {project=solomon, cluster=slice05, service=stockpile, host='kikimr-01', sensor=idleTime})) > 5")
                .setPeriod(Duration.ofMinutes(10))
                .build();

        double result = estimate(alert);
        assertThat(result, allOf(not(equalTo(Double.NaN)), greaterThan(0d)));
    }

    @Test
    public void thresholdAlertHaveDefaultCostForCountPoints() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "slice05",
            "service", "stockpile",
            "host", "kikimr-02",
            "sensor", "idleTime"
        );

        // Zero count point by specified period doesn't means that point absent, because it can't be
        // new created metric or problems inside particular dc that lead to broke metrics collecting
        // by specified period of time
        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        Alert alert = newThresholdAlert()
                .setName(name.getMethodName())
                .setSelectors(Selectors.of(labels))
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(123)
                        .withComparison(Compare.EQ)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE))
                .build();

        double result = estimate(alert);
        assertThat(result, allOf(not(equalTo(Double.NaN)), greaterThan(0d)));
    }

    @Test
    public void expressionAlertHaveDefaultCostForCountPoints() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "slice05",
            "service", "stockpile",
            "host", "kikimr-02",
            "sensor", "idleTime"
        );

        // Zero count point by specified period doesn't means that point absent, because it can't be
        // new created metric or problems inside particular dc that lead to broke metrics collecting
        // by specified period of time
        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        Alert alert = newExpressionAlert()
                .setName(name.getMethodName())
                .setCheckExpression("avg(group_lines('max', " + labels + ")) <= 123")
                .setPeriod(Duration.ofMinutes(10))
                .build();

        double result = estimate(alert);
        assertThat(result, allOf(not(equalTo(Double.NaN)), greaterThan(0d)));
    }

    @Test
    public void thresholdMoreMetricsMoreCost() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "slice10",
            "service", "blobstorage",
            "sensor", "ioWait"
        );

        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "lowCost","host", "kikimr-01")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-01")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-02")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-03")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-04")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-05")),
            AggrGraphDataArrayList.empty());

        String commonLabelsStr = LabelsFormat.format(commonLabels);

        Alert lowCostAlert = newThresholdAlert()
                .setId("lowCost")
                .setProjectId("test")
                .setName(name.getMethodName())
                .setSelectors(commonLabelsStr + ", type=lowCost, host=*")
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(11)
                        .withComparison(Compare.NE)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES))
                .build();

        Alert highCostAlert = newThresholdAlert()
                .setId("highCost")
                .setProjectId("test")
                .setName(name.getMethodName())
                .setSelectors(commonLabelsStr + ", type=highCost, host=*")
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(11)
                        .withComparison(Compare.NE)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES))
                .build();

        double lowCost = estimate(lowCostAlert);
        double highCost = estimate(highCostAlert);

        assertThat("The cost of alert that use 5 metrics should be at least in 5 times higher " +
                        "than alert that use only one metric, because all of this metrics should be " +
                        "loaded from stockpile and alert rule should iterate over all points on each metric",
                lowCost, lessThan(highCost)
        );
    }

    @Test
    public void expressionMoreMetricsMoreCost() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "slice10",
            "service", "blobstorage",
            "sensor", "ioWait"
        );

        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "lowCost","host", "kikimr-01")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-01")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-02")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-03")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-04")),
            AggrGraphDataArrayList.empty());
        solomon.addMetric(
            commonLabels.addAll(Labels.of("type", "highCost","host", "kikimr-05")),
            AggrGraphDataArrayList.empty());

        String commonLabelsStr = LabelsFormat.format(commonLabels);

        Alert lowCostAlert = newExpressionAlert()
                .setId("lowCost")
                .setName(name.getMethodName())
                .setCheckExpression("avg(group_lines('max',{" + commonLabelsStr + ", type=lowCost, host='*'})) <= 11")
                .setPeriod(Duration.ofMinutes(10))
                .build();

        Alert highCostAlert = newExpressionAlert()
                .setId("highCost")
                .setName(name.getMethodName())
                .setCheckExpression("avg(group_lines('max', {" + commonLabelsStr + ", type=highCost, host='*'})) <= 11")
                .setPeriod(Duration.ofMinutes(10))
                .build();

        double lowCost = estimate(lowCostAlert);
        double highCost = estimate(highCostAlert);

        assertThat("The cost of alert that use 5 metrics should be at least in 5 times higher " +
                        "than alert that use only one metric, because all of this metrics should be " +
                        "loaded from stockpile and alert rule should iterate over all points on each metric",
                lowCost, lessThan(highCost)
        );
    }

    @Test
    public void thresholdMoreTimePeriodMoreCost() throws Exception {
        Labels labels = Labels.of("project", "junk","cluster", "foo","service", "bar","sensor", "period");
        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        Alert lowCostAlert = newThresholdAlert()
                .setId("lowCost")
                .setProjectId("test")
                .setName(name.getMethodName())
                .setSelectors(Selectors.of(labels))
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(11)
                        .withComparison(Compare.GT)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES))
                .build();

        Alert highCostAlert = newThresholdAlert()
                .setId("highCost")
                .setProjectId("test")
                .setName(name.getMethodName())
                .setSelectors(Selectors.of(labels))
                .setPeriod(Duration.ofHours(1))
                .setPredicateRule(PredicateRule.onThreshold(11)
                        .withComparison(Compare.GT)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES))
                .build();

        double lowCost = estimate(lowCostAlert);
        double highCost = estimate(highCostAlert);

        assertThat("The cost of alert that use 60 minute period should be at least in 5 times higher " +
                        "than alert that use only 10 minute period, because to evaluate alert rule need more" +
                        "memory",
                lowCost, lessThan(highCost)
        );
    }

    @Test
    public void expressionMoreTimePeriodMoreCost() throws Exception {
        Labels labels = Labels.of("project", "junk","cluster", "foo","service", "bar","sensor", "period");
        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        Alert lowCostAlert = newExpressionAlert()
                .setId("lowCost")
                .setName(name.getMethodName())
                .setCheckExpression("sum(group_lines('sum', " + labels + ")) > 1000")
                .setPeriod(Duration.ofMinutes(10))
                .build();

        Alert highCostAlert = newExpressionAlert()
                .setId("highCost")
                .setName(name.getMethodName())
                .setCheckExpression("sum(group_lines('sum', " + labels + ")) > 1000")
                .setPeriod(Duration.ofHours(1))
                .build();

        double lowCost = estimate(lowCostAlert);
        double highCost = estimate(highCostAlert);

        assertThat("The cost of alert that use 60 minute period should be at least in 5 times higher " +
                        "than alert that use only 10 minute period, because to evaluate alert rule need more" +
                        "memory",
                lowCost, lessThan(highCost)
        );
    }

    @Test
    public void thresholdMorePointsMoreCost() throws Exception {
        Instant now = Instant.parse("2017-09-13T09:00:00Z");
        solomon.addMetric(
            Labels.of("project", "junk","cluster", "foo","service", "10Sec","sensor", "points","host", "solomon-1"),
            generateTimeSeries(now, Duration.ofMinutes(10), TimeUnit.SECONDS.toMillis(10))
        );

        solomon.addMetric(
            Labels.of("project", "junk","cluster", "foo","service", "1Min","sensor", "points","host", "solomon-1"),
            generateTimeSeries(now, Duration.ofMinutes(10), TimeUnit.MINUTES.toMillis(1))
        );

        Alert lowCostAlert = newThresholdAlert()
                .setId("lowCost")
                .setName(name.getMethodName())
                .setSelectors("project=junk, cluster=foo, service=1Min, sensor=points, host=solomon-1")
                .setPeriod(Duration.ofMinutes(5))
                .setPredicateRule(PredicateRule.onThreshold(11)
                        .withComparison(Compare.GT)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES))
                .build();

        Alert highCostAlert = newThresholdAlert()
                .setId("highCost")
                .setName(name.getMethodName())
                .setSelectors("project=junk, cluster=foo, service=10Sec, sensor=points, host=solomon-1")
                .setPeriod(Duration.ofMinutes(5))
                .setPredicateRule(PredicateRule.onThreshold(11)
                        .withComparison(Compare.GT)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES))
                .build();

        double lowCost = estimate(now, lowCostAlert);
        double highCost = estimate(now, highCostAlert);
        assertThat("The cost of alert that use sensor with point every 10 sec should be at least in 5 times higher " +
                        "than alert that use metric with point every 1 min, because to evaluate alert rule need more" +
                        "memory",
                lowCost, lessThan(highCost)
        );
    }

    @Test
    public void expressionMorePointsMoreCost() throws Exception {
        Instant now = Instant.parse("2017-09-13T09:00:00Z");
        solomon.addMetric(
            Labels.of("project", "junk","cluster", "foo","service", "10Sec","sensor", "points","host", "solomon-1"),
            generateTimeSeries(now, Duration.ofMinutes(10), TimeUnit.SECONDS.toMillis(10))
        );

        solomon.addMetric(
            Labels.of("project", "junk","cluster", "foo","service", "1Min","sensor", "points","host", "solomon-1"),
            generateTimeSeries(now, Duration.ofMinutes(10), TimeUnit.MINUTES.toMillis(1))
        );

        Alert lowCostAlert = newExpressionAlert()
                .setId("lowCost")
                .setName(name.getMethodName())
                .setCheckExpression("avg(group_lines('avg', {project='junk', cluster='foo', service='1Min', sensor='points', host='solomon-1'})) > 11")
                .setPeriod(Duration.ofMinutes(5))
                .build();

        Alert highCostAlert = newExpressionAlert()
                .setId("highCost")
                .setName(name.getMethodName())
                .setCheckExpression("avg(group_lines('avg', {project='junk', cluster='foo', service='10Sec', sensor='points', host='solomon-1'})) > 11")
                .setPeriod(Duration.ofMinutes(5))
                .build();

        double lowCost = estimate(now, lowCostAlert);
        double highCost = estimate(now, highCostAlert);
        assertThat("The cost of alert that use metric with point every 10 sec should be at least in 5 times higher " +
                        "than alert that use metric with point every 1 min, because to evaluate alert rule need more" +
                        "memory",
                lowCost, lessThan(highCost)
        );
    }

    @Test
    public void thresholdMetabaseErrorLeadToUseDefaultCountMetrics() throws Exception {
        Alert alert = newThresholdAlert()
                .setName(name.getMethodName())
                .setSelectors("project=solomon, cluster=slice13, service=stockpile, host=kikimr-01, sensor=useTime")
                .setPeriod(Duration.ofMinutes(3))
                .setPredicateRule(PredicateRule.onThreshold(123)
                        .withComparison(Compare.EQ)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE))
                .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.INTERNAL_ERROR);

        double result = estimate(alert);
        assertThat(result, allOf(not(equalTo(Double.NaN)), greaterThan(0d)));
    }

    @Test
    public void thresholdStockpileErrorLeadToUseDefaultPointSteps() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "slice05",
            "service", "stockpile",
            "host", "kikimr-03",
            "sensor", "idleTime");

        MetricId metricId = solomon.randomMetricId();
        solomon.addMetric(metricId, labels, AggrGraphDataArrayList.empty());
        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.INTERNAL_ERROR);

        Alert alert = newThresholdAlert()
                .setName(name.getMethodName())
                .setSelectors(Selectors.of(labels))
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(123)
                        .withComparison(Compare.EQ)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE))
                .build();

        double result = estimate(alert);
        assertThat(result, allOf(not(equalTo(Double.NaN)), greaterThan(0d)));
    }

    @Test
    public void costOfExpressionAlertHigherThenCostOfThresholdAlert() throws Exception {
        Instant now = Instant.parse("2017-09-13T09:00:00Z");
        for (int index = 0; index < 3; index++) {
            solomon.addMetric(
                Labels.of("project", "junk","cluster", "foo","service", "fooBar","sensor", "points","host", "solomon-" + index),
                generateTimeSeries(now, Duration.ofMinutes(10), TimeUnit.MINUTES.toMillis(1))
            );
        }

        Alert thresholdAlert = newThresholdAlert()
                .setName(name.getMethodName())
                .setSelectors("project=junk, cluster=foo, service=fooBar, sensor=points, host=*")
                .setPeriod(Duration.ofMinutes(10))
                .setPredicateRule(PredicateRule.onThreshold(123)
                        .withComparison(Compare.GTE)
                        .withThresholdType(ThresholdType.MAX))
                .build();

        Alert expressionAlert = newExpressionAlert()
                .setName(name.getMethodName())
                .setCheckExpression("max(group_lines('max', {project=junk, cluster=foo, service=fooBar, sensor=points, host='*'})) >= 123")
                .setPeriod(Duration.ofMinutes(10))
                .build();

        double thresholdCost = estimate(now, thresholdAlert);
        double expressionCost = estimate(now, expressionAlert);

        assertThat("Expression rule required to load all lines before evaluate expression " +
                        "it's required more memory then threshold rule that check each metric in parallel",
                expressionCost, greaterThan(thresholdCost)
        );
    }

    private AggrGraphDataArrayList generateTimeSeries(Instant end, Duration period, long stepMillis) {
        int countPoints = Math.round(period.toMillis() / stepMillis);

        int mask = StockpileColumn.TS.mask()
                | StockpileColumn.VALUE.mask()
                | StockpileColumn.STEP.mask();

        AggrGraphDataArrayList result = new AggrGraphDataArrayList(mask, countPoints);
        long now = end.minus(period).toEpochMilli();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < countPoints; index++) {
            AggrPoint point = AggrPoint.builder()
                    .time(now)
                    .doubleValue(random.nextDouble())
                    .stepMillis(stepMillis)
                    .build();

            result.addRecordData(mask, point);
            now += stepMillis;
        }

        return result;
    }

    private double estimate(Alert alert) {
        return estimate(Instant.now(), alert);
    }

    private double estimate(Instant now, Alert alert) {
        Instant deadline = Instant.now().plusSeconds(30);
        return estimator.estimateCost(now, alert, deadline).join();
    }

    private ThresholdAlert.Builder newThresholdAlert() {
        return ThresholdAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId(UUID.randomUUID().toString());
    }

    private ExpressionAlert.Builder newExpressionAlert() {
        return ExpressionAlert.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId(UUID.randomUUID().toString());
    }
}
