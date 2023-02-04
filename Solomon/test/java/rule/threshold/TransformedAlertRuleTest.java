package ru.yandex.solomon.alert.rule.threshold;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.AlertRuleFactory;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryImpl;
import ru.yandex.solomon.alert.rule.AlertRuleFairDeadlines;
import ru.yandex.solomon.alert.rule.usage.ProjectAlertRuleMetrics;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.TemplateFactory;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCache;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.stockpile.api.EStockpileStatusCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.solomon.alert.util.TimeSeriesTestSupport.point;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class TransformedAlertRuleTest {
    @Rule
    public TestName testName = new TestName();
    private SolomonClientStub solomon;
    private AlertRuleFactory alertRuleFactory;

    @Before
    public void setUp() throws Exception {
        solomon = new SolomonClientStub();
        MetricsClient metricsClient = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        TemplateFactory templateFactory = new MustacheTemplateFactory();
        FindCacheOptions cacheOptions = FindCacheOptions.newBuilder()
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
    }

    @After
    public void tearDown() throws Exception {
        solomon.close();
    }

    @Test
    public void groupLines() {
        Labels common = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime"
        );

        addMetric(common.add("host", "solomon-01"),
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 55),
                point("2017-09-07T11:15:00Z", 21),
                point("2017-09-07T11:30:00Z", 2), // window start
                point("2017-09-07T11:33:00Z", 3),
                point("2017-09-07T11:45:00Z", 4),
                point("2017-09-07T12:00:00Z", 5), // now
                point("2017-09-07T12:15:00Z", 421),
                point("2017-09-07T12:30:00Z", 31),
                point("2017-09-07T12:45:00Z", 4.4)
            )
        );

        addMetric(common.add("host", "solomon-02"),
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 1),
                point("2017-09-07T11:15:00Z", 4),
                point("2017-09-07T11:30:00Z", 2), // window start
                point("2017-09-07T11:33:00Z", 3),
                point("2017-09-07T11:45:00Z", 6),
                point("2017-09-07T12:00:00Z", 1), // now
                point("2017-09-07T12:15:00Z", 4),
                point("2017-09-07T12:30:00Z", 2),
                point("2017-09-07T12:45:00Z", 5)
            )
        );

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=solomon-*")
            .setTransformations("group_lines('sum', input{})")
            .setPredicateRule(PredicateRule.onThreshold(9.5)
                .withThresholdType(ThresholdType.AT_LEAST_ONE)
                .withComparison(Compare.GTE))
            .setPeriod(Duration.ofMinutes(30))
            .setAnnotations(Map.of("value", "{{pointValue}} {{alert.comparison}} {{alert.threshold}}"))
            .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(status.getAnnotations().get("value"), equalTo("10.0 GTE 9.5"));
    }

    @Test
    public void derivative() {
        Labels common = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "errors"
        );

        addMetric(common.add("host", "solomon-01"),
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 10),
                point("2017-09-07T11:15:00Z", 14),
                point("2017-09-07T11:30:00Z", 16), // window start
                point("2017-09-07T11:45:00Z", 21),
                point("2017-09-07T12:00:00Z", 21), // now
                point("2017-09-07T12:15:00Z", 100500),
                point("2017-09-07T12:30:00Z", 100501),
                point("2017-09-07T12:45:00Z", 100502)
            )
        );

        addMetric(common.add("host", "solomon-02"),
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 10),
                point("2017-09-07T11:15:00Z", 14),
                point("2017-09-07T11:30:00Z", 16), // window start
                point("2017-09-07T11:45:00Z", 88),
                point("2017-09-07T12:00:00Z", 89), // now
                point("2017-09-07T12:15:00Z", 89),
                point("2017-09-07T12:30:00Z", 95),
                point("2017-09-07T12:45:00Z", 98)
            )
        );

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=solomon, cluster=foo, service=misc, sensor=errors, host=solomon-*")
            .setTransformations("derivative(input{})")
            .setPredicateRule(PredicateRule.onThreshold(0.05)
                .withThresholdType(ThresholdType.AT_LEAST_ONE)
                .withComparison(Compare.GT))
            .setPeriod(Duration.ofMinutes(30))
            .setAnnotations(Map.of("value", "{{pointValue}} {{alert.comparison}} {{alert.threshold}}"))
            .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(status.getAnnotations().get("value"), equalTo("0.08 GT 0.05"));
    }

    @Test
    public void errorBeforeTransformationsAreApplied() throws Exception {
        Labels labels1 = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels1,
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 55),
                point("2017-09-07T11:15:00Z", 21),
                point("2017-09-07T11:30:00Z", 2), // window start
                point("2017-09-07T11:33:00Z", 123),
                point("2017-09-07T11:45:00Z", 444),
                point("2017-09-07T12:00:00Z", 5), // now
                point("2017-09-07T12:15:00Z", 421),
                point("2017-09-07T12:30:00Z", 31),
                point("2017-09-07T12:45:00Z", 4.4)
            )
        );

        MetricId errorMetricId = solomon.randomMetricId();
        Labels labels2 = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-02"
        );

        solomon.addMetric(errorMetricId, labels2, AggrGraphDataArrayList.empty());
        solomon.getStockpile().predefineStatusCodeForMetric(errorMetricId, EStockpileStatusCode.INTERNAL_ERROR);

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=solomon-*")
            .setPredicateRule(PredicateRule.onThreshold(50)
                .withThresholdType(ThresholdType.AT_LEAST_ONE)
                .withComparison(Compare.GTE))
            .setTransformations("input{}")
            .setPeriod(Duration.ofMinutes(30))
            .build();

        // alarm should be triggered at this time 2017-09-07T11:33:00Z on host solomon-01
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat("This test should fail when ",
            status.getCode(), equalTo(EvaluationStatus.Code.ERROR));
    }

    private ThresholdAlert.Builder newThresholdAlert() {
        return newThresholdAlert(testName.getMethodName());
    }

    private ThresholdAlert.Builder newThresholdAlert(String id) {
        return ThresholdAlert.newBuilder()
            .setId(id)
            .setProjectId("junk");
    }

    private void addMetric(Labels labels, AggrGraphDataArrayList source) {
        solomon.addMetric(labels, source);
    }

    private EvaluationStatus syncCheckResult(Alert alert, Instant now) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        AlertRule rule = alertRuleFactory.createAlertRule(alert);
        CompletableFuture<EvaluationStatus> future = rule.eval(now, deadlines);
        return future.join();
    }

}
