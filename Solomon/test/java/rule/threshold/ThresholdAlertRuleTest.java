package ru.yandex.solomon.alert.rule.threshold;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.domain.NoPointsPolicy;
import ru.yandex.solomon.alert.domain.ResolvedEmptyPolicy;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.AlertRuleFactory;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryImpl;
import ru.yandex.solomon.alert.rule.AlertRuleFairDeadlines;
import ru.yandex.solomon.alert.rule.AlertTimeSeries;
import ru.yandex.solomon.alert.rule.ExplainResult;
import ru.yandex.solomon.alert.rule.usage.ProjectAlertRuleMetrics;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.TemplateFactory;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCache;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.stockpile.api.EStockpileStatusCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.solomon.alert.util.TimeSeriesTestSupport.point;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;


/**
 * @author Vladimir Gordiychuk
 */
public class ThresholdAlertRuleTest {
    @Rule
    public TestName testName = new TestName();
    private SolomonClientStub solomon;
    private MetricsClient metricsClient;
    private AlertRuleFactory alertRuleFactory;
    private FindCacheOptions cacheOptions;
    private int maxMetricsToLoad;

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
        maxMetricsToLoad = 100;

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
    public void noDataStateWhenMetabaseDoesNotHaveMetricsForSelector() throws Exception {
        // noise metrics
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "solomon-01");

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5),
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=bar, sensor=idleTime, host=kikimr-??")
                .setPredicateRule(PredicateRule.onThreshold(13)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        EvaluationStatus.Code status = syncCheck(alert, Instant.parse("2017-09-07T12:00:00Z"));

        assertThat(status, equalTo(EvaluationStatus.Code.NO_DATA));
    }

    @Test
    public void warnWhenMetabaseDoesNotHaveMetricsForSelector() {
        // noise metrics
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "solomon-01");

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5),
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=bar, sensor=idleTime, host=kikimr-??")
                .setPredicateRule(PredicateRule.onThreshold(13)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.WARN)
                .build();

        EvaluationStatus.Code status = syncCheck(alert, Instant.parse("2017-09-07T12:00:00Z"));

        assertThat(status, equalTo(EvaluationStatus.Code.WARN));
    }

    @Test
    public void noDataWhenAbsentPointsForCheckInterval() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "solomon-01");

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5),
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=bar, sensor=idleTime, host=solomon-*")
                .setPredicateRule(PredicateRule.onThreshold(13)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // another day, another time
        Instant now = Instant.parse("2017-09-10T10:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.NO_DATA));
    }

    @Test
    public void manualWhenAbsentPointsForCheckInterval() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "solomon-01");

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5),
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=bar, sensor=idleTime, host=solomon-*")
                .setPredicateRules(Stream.of(
                        PredicateRule.onThreshold(13)
                            .withThresholdType(ThresholdType.AT_LEAST_ONE)
                            .withComparison(Compare.GTE),
                        PredicateRule.onThreshold(0)
                                .withThresholdType(ThresholdType.COUNT)
                                .withComparison(Compare.EQ)
                                .withTargetStatus(TargetStatus.ALARM)))
                .setPeriod(Duration.ofMinutes(30))
                .setNoPointsPolicy(NoPointsPolicy.MANUAL)
                .setSeverity(AlertSeverity.CRITICAL)
                .build();

        // another day, another time
        Instant now = Instant.parse("2017-09-10T10:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void alarmOnSingleLine() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5), // window start
                        point("2017-09-07T11:33:00Z", 11),
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1), // now
                        point("2017-09-07T12:15:00Z", 5),
                        point("2017-09-07T12:30:00Z", 2),
                        point("2017-09-07T12:45:00Z", 13)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // point at 2017-09-07T11:45:00Z should lead to alarm
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void alarmOnSingleLineWithMultipleChecks() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "misc",
                "sensor", "idleTime",
                "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5), // window start
                        point("2017-09-07T11:33:00Z", 11),
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1), // now
                        point("2017-09-07T12:15:00Z", 5),
                        point("2017-09-07T12:30:00Z", 2),
                        point("2017-09-07T12:45:00Z", 13)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 100, TargetStatus.ALARM),
                        PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GTE, 5, TargetStatus.WARN)
                ))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // point at 2017-09-07T11:45:00Z should lead to alarm
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.WARN));
    }

    @Test
    public void alarmOnSingleLineWithMultipleChecksOK() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "misc",
                "sensor", "idleTime",
                "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5), // window start
                        point("2017-09-07T11:33:00Z", 11),
                        point("2017-09-07T11:45:00Z", 1.2),
                        point("2017-09-07T12:00:00Z", 23.1), // now
                        point("2017-09-07T12:15:00Z", 5),
                        point("2017-09-07T12:30:00Z", 2),
                        point("2017-09-07T12:45:00Z", 13)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 100, TargetStatus.ALARM),
                        PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GTE, 5, TargetStatus.WARN),
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.LT, 0, TargetStatus.ALARM)
                ))
                .setAnnotations(Map.of(
                    "value", "{{pointValue}}",
                    "time", "{{pointTime}}",
                    "test", "{{alert.comparison}}{{alert.threshold}}",
                    "id", "{{alert.id}}"))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // point at 2017-09-07T11:45:00Z should lead to alarm
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.OK));
        assertThat(status.getAnnotations().get("time"), equalTo("2017-09-07T11:45:00Z"));
        assertThat(status.getAnnotations().get("value"), equalTo("1.2"));
        assertThat(status.getAnnotations().get("test"), equalTo("LT0.0"));
        assertThat(status.getAnnotations().get("id"), equalTo(alert.getId()));
    }

    @Test
    public void genericAnnotationPreserved() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 1),
                point("2017-09-07T11:15:00Z", -3),
                point("2017-09-07T11:30:00Z", 5), // window start
                point("2017-09-07T11:33:00Z", 11),
                point("2017-09-07T11:45:00Z", 1.2),
                point("2017-09-07T12:00:00Z", 23.1), // now
                point("2017-09-07T12:15:00Z", 5),
                point("2017-09-07T12:30:00Z", 2),
                point("2017-09-07T12:45:00Z", 13)
            )
        );

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors(Selectors.of(labels))
            .setPredicateRules(Stream.of(
                PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 100, TargetStatus.ALARM),
                PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GTE, 5, TargetStatus.WARN),
                PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.LT, 0, TargetStatus.ALARM)
            ))
            .setAnnotations(Map.of("id", "{{alert.id}}", "name", "{{alert.name}}"))
            .setPeriod(Duration.ofMinutes(30))
            .build();

        // point at 2017-09-07T11:45:00Z should lead to alarm
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.OK));
        assertThat(status.getAnnotations().get("id"), equalTo(alert.getId()));
        assertThat(status.getAnnotations().get("name"), equalTo(alert.getName()));
    }

    @Test
    public void alarmOnSingleLineWithMultipleChecksNoData() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "misc",
                "sensor", "idleTime",
                "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5), // window start
                        point("2017-09-07T11:33:00Z", 11),
                        point("2017-09-07T11:45:00Z", 1.2),
                        point("2017-09-07T12:00:00Z", 23.1), // now
                        point("2017-09-07T12:15:00Z", 5),
                        point("2017-09-07T12:30:00Z", 2),
                        point("2017-09-07T12:45:00Z", 13)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRules(Stream.of(
                        PredicateRule.of(ThresholdType.COUNT, Compare.LT, 4, TargetStatus.NO_DATA),
                        PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 1, TargetStatus.ALARM)
                ))
                .setAnnotations(Map.of("value", "{{pointValue}}", "time", "{{pointTime}}", "test", "{{alert.comparison}}{{alert.threshold}}"))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // point at 2017-09-07T11:45:00Z should lead to alarm
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.NO_DATA));
        assertThat(status.getAnnotations().get("time"), equalTo("2017-09-07T11:45:00Z"));
        assertThat(status.getAnnotations().get("value"), equalTo("3.0"));
        assertThat(status.getAnnotations().get("test"), equalTo("LT4.0"));
    }

    @Test
    public void okOnSingleLineLeftWindowCheck() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 55),
                        point("2017-09-07T11:15:00Z", 21),
                        point("2017-09-07T11:30:00Z", 2), // window start
                        point("2017-09-07T11:33:00Z", 3),
                        point("2017-09-07T11:45:00Z", 4),
                        point("2017-09-07T12:00:00Z", 5), // now
                        point("2017-09-07T12:15:00Z", 1),
                        point("2017-09-07T12:30:00Z", 1),
                        point("2017-09-07T12:45:00Z", 1)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(10)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // from 2017-09-07T11:30:00Z to 2017-09-07T12:00:00Z without violation
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void okOnSingleLineRightWindowCheck() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 2),
                        point("2017-09-07T11:30:00Z", 2.5), // window start
                        point("2017-09-07T11:33:00Z", 3.3),
                        point("2017-09-07T11:45:00Z", 4.4),
                        point("2017-09-07T12:00:00Z", 5.2), // now
                        point("2017-09-07T12:15:00Z", 33),
                        point("2017-09-07T12:30:00Z", 50),
                        point("2017-09-07T12:45:00Z", 121)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(20)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // from 2017-09-07T11:30:00Z to 2017-09-07T12:00:00Z without violation
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void deadlineCausedByMetabase() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 2),
                        point("2017-09-07T12:45:00Z", 121)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(20)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.DEADLINE_EXCEEDED);

        Instant now = Instant.parse("2017-09-07T12:45:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(status.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.DEADLINE));
    }

    @Test
    public void deadlineCausedByStockpile() {
        MetricId metricId = randomMetricId();
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(metricId, labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 2),
                        point("2017-09-07T11:15:00Z", 3)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(20)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.DEADLINE_EXCEEDED);
        Instant now = Instant.parse("2017-09-07T11:15:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(status.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.DEADLINE));
    }

    @Test
    public void shardNotReadyCausedByStockpile() {
        MetricId metricId = randomMetricId();
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(metricId, labels,
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 1),
                point("2017-09-07T11:15:00Z", 2),
                point("2017-09-07T11:15:00Z", 3)
            )
        );

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors(Selectors.of(labels))
            .setPredicateRule(PredicateRule.onThreshold(20)
                    .withThresholdType(ThresholdType.AT_LEAST_ONE)
                    .withComparison(Compare.GTE))
            .setPeriod(Duration.ofMinutes(30))
            .build();

        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.SHARD_NOT_READY);
        Instant now = Instant.parse("2017-09-07T11:15:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(status.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.UNAVAILABLE));
    }

    @Test
    public void shardNotReadyCausedByMetabase() {
        MetricId metricId = randomMetricId();
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(metricId, labels,
            AggrGraphDataArrayList.of(
                point("2017-09-07T11:00:00Z", 1),
                point("2017-09-07T11:15:00Z", 2),
                point("2017-09-07T11:15:00Z", 3)
            )
        );

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors(Selectors.of(labels))
            .setPredicateRule(PredicateRule.onThreshold(20)
                    .withThresholdType(ThresholdType.AT_LEAST_ONE)
                    .withComparison(Compare.GTE))
            .setPeriod(Duration.ofMinutes(30))
            .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.SHARD_NOT_READY);
        Instant now = Instant.parse("2017-09-07T11:15:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(status.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.UNAVAILABLE));
    }

    @Test
    public void errorCausedByMetabase() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 2),
                        point("2017-09-07T12:45:00Z", 121)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(20)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.INTERNAL_ERROR);

        Instant now = Instant.parse("2017-09-07T12:45:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ERROR));
    }

    @Test
    @Ignore("Broken due to switch to readMany. No errors from individual metrics are possible now")
    public void errorCausedByStockpile() throws Exception {
        MetricId metricId = randomMetricId();
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(metricId, labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 2),
                        point("2017-09-07T12:45:00Z", 121)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(20)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .setAnnotations(Map.of("check", "{{alert.id}}~{{alert.aggregation}}~{{pointValue}}~{{alert.comparison}}~{{alert.threshold}}"))
                .build();

        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.CORRUPTED_BINARY_DATA);

        Instant now = Instant.parse("2017-09-07T12:45:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(status.getAnnotations(), hasEntry("check", alert.getId() + "~~NaN~~"));
    }

    @Test
    public void okOnMultipleLines() throws Exception {
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
                        point("2017-09-07T11:33:00Z", 3),
                        point("2017-09-07T11:45:00Z", 4),
                        point("2017-09-07T12:00:00Z", 5), // now
                        point("2017-09-07T12:15:00Z", 421),
                        point("2017-09-07T12:30:00Z", 31),
                        point("2017-09-07T12:45:00Z", 4.4)
                )
        );

        Labels labels2 = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-02"
        );

        addMetric(labels2,
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
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void namedSelectors() throws Exception {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "misc"
        );

        addMetric(labels.add("sensor", "idleTime"),
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

        addMetric(labels.add("sensor", "busyTime"),
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
                .setSelectors("idleTime{project=solomon, cluster=foo, service=misc}")
                .setPredicateRule(PredicateRule.onThreshold(6)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void alarmWhenAtLeastOneLineAlarm() throws Exception {
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

        Labels labels2 = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-02"
        );

        addMetric(labels2,
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
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // alarm should be triggered at this time 2017-09-07T11:33:00Z on host solomon-01
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void alarmHaveMorePriorityThanNoData() throws Exception {
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

        Labels labels2 = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-02"
        );


        addMetric(labels2,
                AggrGraphDataArrayList.empty() // by some reason host not report metrics now but still resolves via metabase for trigger check
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=solomon-*")
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // alarm should be triggered at this time 2017-09-07T11:33:00Z on host solomon-01
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    @Ignore("This behavior is no more possible since we've switched to readMany")
    public void alarmHaveMorePriorityThanError() throws Exception {
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

        MetricId errorMetricId = randomMetricId();
        Labels labels2 = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-02"
        );

        addMetric(errorMetricId, labels2, AggrGraphDataArrayList.empty());
        solomon.getStockpile().predefineStatusCodeForMetric(errorMetricId, EStockpileStatusCode.INTERNAL_ERROR);

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=solomon-*")
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // alarm should be triggered at this time 2017-09-07T11:33:00Z on host solomon-01
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat("Alarm status should have max severity, as only we found timeseries that " +
                        "trigger alarm we should report about it although another timeseries status",
                status, equalTo(EvaluationStatus.Code.ALARM)
        );
    }

    @Test
    public void noMetricPolicyPreservesAnnotation() {
        ThresholdAlert alert = newThresholdAlert()
                .setNoPointsPolicy(NoPointsPolicy.NO_DATA)
                .setSeverity(AlertSeverity.CRITICAL)
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.NO_DATA)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime")
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setAnnotations(Map.of("host", "solomon"))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.NO_DATA));
        assertThat(status.getAnnotations().get("host"), equalTo("solomon"));
    }

    @Test
    public void noPointsPolicyPreservesAnnotation() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "misc",
                "sensor", "idleTime"
        );

        addMetric(labels, AggrGraphDataArrayList.empty());

        ThresholdAlert alert = newThresholdAlert()
                .setSeverity(AlertSeverity.CRITICAL)
                .setNoPointsPolicy(NoPointsPolicy.NO_DATA)
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.NO_DATA)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime")
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setAnnotations(Map.of("host", "solomon"))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.NO_DATA));
        assertThat(status.getAnnotations().get("host"), equalTo("solomon"));
    }

    private ThresholdAlert.Builder newThresholdAlert() {
        return newThresholdAlert(testName.getMethodName());
    }

    private ThresholdAlert.Builder newThresholdAlert(String id) {
        return ThresholdAlert.newBuilder()
                .setId(id)
                .setProjectId("junk");
    }

    @Test
    public void checkMultiAlert() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "freeSpace");

        // disk=sda1 host=solomon-01
        addMetric(commonLabels.add("disk", "sda1").add("host", "solomon-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1000),
                        point("2017-09-07T11:15:00Z", 1000),
                        point("2017-09-07T11:30:00Z", 50), // window start
                        point("2017-09-07T11:33:00Z", 30),
                        point("2017-09-07T11:45:00Z", 20),
                        point("2017-09-07T12:00:00Z", 10), // now
                        point("2017-09-07T12:15:00Z", 421),
                        point("2017-09-07T12:30:00Z", 421),
                        point("2017-09-07T12:45:00Z", 500)
                )
        );

        // disk=sda2 host=solomon-01
        addMetric(commonLabels.add("disk", "sda2").add("host", "solomon-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1000),
                        point("2017-09-07T11:15:00Z", 2000),
                        point("2017-09-07T11:30:00Z", 1500), // window start
                        point("2017-09-07T11:33:00Z", 1300),
                        point("2017-09-07T11:45:00Z", 1500),
                        point("2017-09-07T12:00:00Z", 1770), // now
                        point("2017-09-07T12:15:00Z", 500),
                        point("2017-09-07T12:30:00Z", 600),
                        point("2017-09-07T12:45:00Z", 550)
                )
        );

        // disk=sda1 host=solomon-02
        addMetric(commonLabels.add("disk", "sda1").add("host", "solomon-02"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1000),
                        point("2017-09-07T11:15:00Z", 1000),
                        point("2017-09-07T11:30:00Z", 1000), // window start
                        point("2017-09-07T11:33:00Z", 1000),
                        point("2017-09-07T11:45:00Z", 1000),
                        point("2017-09-07T12:00:00Z", 1000), // now
                        point("2017-09-07T12:15:00Z", 1000),
                        point("2017-09-07T12:30:00Z", 1000),
                        point("2017-09-07T12:45:00Z", 1000)
                )
        );

        // disk=sda1 host=solomon-03
        addMetric(commonLabels.add("disk", "sda1").add("host", "solomon-03"), AggrGraphDataArrayList.empty());

        ThresholdAlert parent = newThresholdAlert("groupedResult")
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=freeSpace, host=*, disk=*")
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES)
                        .withComparison(Compare.LTE))
                .setPeriod(Duration.ofMinutes(30))
                .setGroupByLabels(Arrays.asList("disk", "host"))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("disk", "sda1", "host", "solomon-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.ALARM));
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("disk", "sda2", "host", "solomon-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.OK));
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("disk", "sda1", "host", "solomon-03"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.NO_DATA));
    }

    @Test
    public void groupKeyCanBeNotFull() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "inFlight");

        addMetric(labels.add("host", "solomon-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:15:00Z", 1000),
                        point("2017-09-07T11:30:00Z", 50), // window start
                        point("2017-09-07T11:33:00Z", 30),
                        point("2017-09-07T11:45:00Z", 20),
                        point("2017-09-07T12:00:00Z", 10), // now
                        point("2017-09-07T12:15:00Z", 421)
                )
        );

        addMetric(labels.add("host", "solomon-01").add("type", "grpc"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:15:00Z", 50),
                        point("2017-09-07T11:30:00Z", 123), // window start
                        point("2017-09-07T11:33:00Z", 333),
                        point("2017-09-07T11:45:00Z", 555),
                        point("2017-09-07T12:00:00Z", 123), // now
                        point("2017-09-07T12:15:00Z", 10)
                )
        );

        ThresholdAlert parent = newThresholdAlert("groupedResult")
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .setGroupByLabels(Arrays.asList("host", "type"))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("host", "solomon-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.OK));
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("type", "grpc", "host", "solomon-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void annotationTest() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "inFlight"
        );

        addMetric(labels.add("host", "solomon-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:15:00Z", 1000),
                        point("2017-09-07T11:30:00Z", 50), // window start
                        point("2017-09-07T11:33:00Z", 130),
                        point("2017-09-07T11:45:00Z", 120),
                        point("2017-09-07T12:00:00Z", 10), // now
                        point("2017-09-07T12:15:00Z", 421)
                )
        );

        ThresholdAlert alert = newThresholdAlert("annotationTest")
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .setAnnotations(ImmutableMap.<String, String>builder()
                        .put("from", "{{fromTime}}")
                        .put("to", "{{toTime}}")
                        .put("value", "{{pointValue}}")
                        .put("time", "{{pointTime}}")
                        .put("host", "{{labels.host}}")
                        .put("details", "At {{pointTime}} value {{pointValue}} >= 100 -> {{status.code}}")
                        .put("constantKey", "constantValue")
                        .put("message", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}")
                        .build())
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(result.getAnnotations(), hasEntry("from", "2017-09-07T11:30:00Z"));
        assertThat(result.getAnnotations(), hasEntry("to", "2017-09-07T12:00:00Z"));
        assertThat(result.getAnnotations(), hasEntry("value", "130.0"));
        assertThat(result.getAnnotations(), hasEntry("time", "2017-09-07T11:33:00Z"));
        assertThat(result.getAnnotations(), hasEntry("host", "solomon-01"));
        assertThat(result.getAnnotations(), hasEntry("details", "At 2017-09-07T11:33:00Z value 130.0 >= 100 -> ALARM"));
        assertThat(result.getAnnotations(), hasEntry("constantKey", "constantValue"));
        assertThat(result.getAnnotations(), hasEntry("message", "Everything broken!"));
    }

    @Test
    public void explainSingleMetric() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "inFlight",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:15:00Z", 1000),
                        point("2017-09-07T11:30:00Z", 50), // window start
                        point("2017-09-07T11:33:00Z", 30),
                        point("2017-09-07T11:45:00Z", 20),
                        point("2017-09-07T12:00:00Z", 10), // now
                        point("2017-09-07T12:15:00Z", 421)
                )
        );

        ThresholdAlert alert = newThresholdAlert("groupedResult")
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        AlertRule rule = alertRuleFactory.createAlertRule(alert);

        AggrGraphDataArrayList expectedSource = AggrGraphDataArrayList.of(
                point("2017-09-07T11:30:00Z", 50), // window start
                point("2017-09-07T11:33:00Z", 30),
                point("2017-09-07T11:45:00Z", 20));

        ExplainResult result = rule.explain(now, deadline()).join();
        assertThat(result.getStatus().getCode(), equalTo(EvaluationStatus.Code.OK));
        assertThat(result.getSeries(), hasItem(new AlertTimeSeries(labels, MetricType.DGAUGE, expectedSource)));
    }

    @Test
    public void explainMultipleMetric() {
        Labels commonLabels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "inFlight");

        addMetric(commonLabels.add("host", "solomon-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:15:00Z", 1000),
                        point("2017-09-07T11:30:00Z", 50), // window start
                        point("2017-09-07T11:33:00Z", 130),
                        point("2017-09-07T11:45:00Z", 120),
                        point("2017-09-07T12:00:00Z", 10), // now
                        point("2017-09-07T12:15:00Z", 421)
                )
        );

        addMetric(commonLabels.add("host", "solomon-02"),
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:15:00Z", 1),
                        point("2017-09-07T11:30:00Z", 2), // window start
                        point("2017-09-07T11:33:00Z", 3),
                        point("2017-09-07T11:45:00Z", 4),
                        point("2017-09-07T12:00:00Z", 5), // now
                        point("2017-09-07T12:15:00Z", 6)
                )
        );

        ThresholdAlert alert = newThresholdAlert("groupedResult")
                .setSelectors(Selectors.of(commonLabels))
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        AlertRule rule = alertRuleFactory.createAlertRule(alert);

        Labels expectedLabelsOne = commonLabels.add("host", "solomon-01");
        AggrGraphDataArrayList expectedSourceOne = AggrGraphDataArrayList.of(
                point("2017-09-07T11:30:00Z", 50),
                point("2017-09-07T11:33:00Z", 130),
                point("2017-09-07T11:45:00Z", 120));

        Labels expectedLabelsTwo = commonLabels.add("host", "solomon-02");
        AggrGraphDataArrayList expectedSourceTwo = AggrGraphDataArrayList.of(
                point("2017-09-07T11:30:00Z", 2),
                point("2017-09-07T11:33:00Z", 3),
                point("2017-09-07T11:45:00Z", 4));

        ExplainResult result = rule.explain(now, deadline()).join();
        assertThat(result.getStatus().getCode(), equalTo(EvaluationStatus.Code.ALARM));
        var first = new AlertTimeSeries(expectedLabelsOne, MetricType.DGAUGE, expectedSourceOne);
        var second = new AlertTimeSeries(expectedLabelsTwo, MetricType.DGAUGE, expectedSourceTwo);
        assertThat(result.getSeries(), hasItem(first));
        assertThat(result.getSeries(), hasItem(second));
    }

    @Test
    public void delaySeconds() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "solomon-01"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", -3),
                        point("2017-09-07T11:30:00Z", 5), // start window
                        point("2017-09-07T11:45:00Z", 91.2),
                        point("2017-09-07T12:00:00Z", 23.1) // now
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=bar, sensor=idleTime, host=solomon-*")
                .setPredicateRule(PredicateRule.onThreshold(10)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .setDelaySeconds(Math.toIntExact(TimeUnit.MINUTES.toSeconds(30L)))
                .build();

        // expected window {2017-09-07T11:00:00Z - 2017-09-07T11:30:00Z}
        assertThat(syncCheck(alert, Instant.parse("2017-09-07T12:00:00Z")), equalTo(EvaluationStatus.Code.OK));
        // expected window {2017-09-07T11:10:00Z - 2017-09-07T11:40:00Z}
        assertThat(syncCheck(alert, Instant.parse("2017-09-07T12:10:00Z")), equalTo(EvaluationStatus.Code.OK));
        // expected window {2017-09-07T11:30:00Z - 2017-09-07T12:00:00Z}
        assertThat(syncCheck(alert, Instant.parse("2017-09-07T12:30:00Z")), equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void limitMetricLoad() {
        Labels commonLabels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime");

        for (int index = 0; index < maxMetricsToLoad + 1; index++) {
            Labels labels = commonLabels.add("host", "solomon-" + index);
            AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                    point("2017-09-07T11:45:00Z", ThreadLocalRandom.current().nextDouble()));
            addMetric(labels, source);
        }

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(commonLabels))
                .setPredicateRule(PredicateRule.onThreshold(10)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        assertThat(syncCheck(alert, Instant.parse("2017-09-07T12:00:00Z")), equalTo(EvaluationStatus.Code.ERROR));
    }

    @Test
    public void useAlertLocationAsProjectByDefault() {
        Labels labels1 = Labels.of(
            "project", "alice",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01");

        addMetric(labels1,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 2),
                        point("2017-09-07T11:30:00Z", 2.5), // window start
                        point("2017-09-07T11:33:00Z", 3.3),
                        point("2017-09-07T11:45:00Z", 4.4),
                        point("2017-09-07T12:00:00Z", 5.2), // now
                        point("2017-09-07T12:15:00Z", 33)
                )
        );

        Labels labels2 = Labels.of(
            "project", "bob",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01");

        addMetric(labels2,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 10),
                        point("2017-09-07T11:15:00Z", 50),
                        point("2017-09-07T11:30:00Z", 100), // window start
                        point("2017-09-07T11:33:00Z", 200),
                        point("2017-09-07T11:45:00Z", 300),
                        point("2017-09-07T12:00:00Z", 400), // now
                        point("2017-09-07T12:15:00Z", 500)
                )
        );

        ThresholdAlert alice = newThresholdAlert()
                .setProjectId("alice")
                .setSelectors("cluster=foo, service=misc, sensor=idleTime, host=solomon-01")
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        ThresholdAlert bob = newThresholdAlert()
                .setProjectId("bob")
                .setSelectors("project=bob, cluster=foo, service=misc, sensor=idleTime, host=solomon-01")
                .setPredicateRule(PredicateRule.onThreshold(50)
                        .withThresholdType(ThresholdType.AT_LEAST_ONE)
                        .withComparison(Compare.GTE))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        // from 2017-09-07T11:30:00Z to 2017-09-07T12:00:00Z without violation
        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        assertThat(syncCheck(alice, now), equalTo(EvaluationStatus.Code.OK));
        assertThat(syncCheck(bob, now), equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void useAlertLocationAsDefaultProjectForSubAlert() {
        // alice
        Labels labelsA = Labels.of(
            "project", "alice",
            "cluster", "foo",
            "service", "misc",
            "sensor", "freeSpace",
            "host", "solomon-01");

        addMetric(labelsA,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:30:00Z", 50), // window start
                        point("2017-09-07T11:33:00Z", 30),
                        point("2017-09-07T12:00:00Z", 10) // now
                )
        );

        // bob
        Labels labelsB = Labels.of(
            "project", "bob",
            "cluster", "foo",
            "service", "misc",
            "sensor", "freeSpace",
            "host", "solomon-01");

        addMetric(labelsB,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:30:00Z", 5000), // window start
                        point("2017-09-07T11:33:00Z", 3000),
                        point("2017-09-07T12:00:00Z", 1000) // now
                )
        );

        ThresholdAlert alice = newThresholdAlert()
                .setProjectId("alice")
                .setSelectors("cluster=foo, service=misc, sensor=freeSpace, host=*")
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES)
                        .withComparison(Compare.LTE))
                .setPeriod(Duration.ofMinutes(30))
                .setGroupByLabel("host")
                .build();

        ThresholdAlert bob = newThresholdAlert()
                .setProjectId("bob")
                .setSelectors("cluster=foo, service=misc, sensor=freeSpace, host=*")
                .setPredicateRule(PredicateRule.onThreshold(100)
                        .withThresholdType(ThresholdType.AT_ALL_TIMES)
                        .withComparison(Compare.LTE))
                .setPeriod(Duration.ofMinutes(30))
                .setGroupByLabel("host")
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(alice)
                        .setGroupKey(Labels.of("host", "solomon-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.ALARM));
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(bob)
                        .setGroupKey(Labels.of("host", "solomon-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void countAggregate() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "pushToTopic",
            "topic", "test"
        );

        addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:00:00Z", 1),
                        point("2017-09-07T11:15:00Z", 1),
                        point("2017-09-07T11:30:00Z", 4),
                        point("2017-09-07T11:45:00Z", 1),
                        point("2017-09-07T12:00:00Z", 3)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors("project=solomon, cluster=foo, service=bar, sensor=pushToTopic, topic=test")
                .setPredicateRule(PredicateRule.onThreshold(3)
                        .withThresholdType(ThresholdType.COUNT)
                        .withComparison(Compare.LT))
                .setPeriod(Duration.ofDays(1))
                .addAnnotation("value", "{{pointValue}}")
                .build();

        Instant now = Instant.parse("2017-09-07T14:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.OK));
        assertThat(status.getAnnotations().get("value"), equalTo("5.0"));
    }

    @Test
    public void alarmOnSingleLineTestLongValue() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "pushToTopic",
            "topic", "test"
        );

        addMetric(labels,
            AggrGraphDataArrayList.of(
                lpoint("2017-09-07T11:00:00Z", 5),
                lpoint("2017-09-07T11:00:01Z", 6)
            )
        );

        ThresholdAlert alert = newThresholdAlert()
            .setSelectors("project=solomon, cluster=foo, service=bar, sensor=pushToTopic, topic=test")
            .setPredicateRule(PredicateRule.onThreshold(9)
                    .withThresholdType(ThresholdType.SUM)
                    .withComparison(Compare.LT))
            .setPeriod(Duration.ofDays(1))
            .addAnnotation("value", "{{pointValue}}")
            .build();

        Instant now = Instant.parse("2017-09-07T14:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        assertThat(status.getCode(), equalTo(EvaluationStatus.Code.OK));
        assertThat(status.getAnnotations().get("value"), equalTo("11.0"));
    }


    @Test
    public void namedSelectorsPreserveNameLabel() {
        Labels labels = Labels.of("project", "solomon", "cluster", "local", "service", "test");
        ThresholdAlert parent = newThresholdAlert()
            .setProjectId("solomon")
            .setSelectors("idleTime{cluster=local, service=test, host=*}")
            .setPredicateRules(Stream.of(
                PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GT, 100, TargetStatus.ALARM),
                PredicateRule.of(ThresholdType.AT_ALL_TIMES, Compare.GT, 10, TargetStatus.WARN)
            ))
            .setPeriod(Duration.ofDays(1))
            .setGroupByLabel("host")
            .build();

        addMetric(labels.add("sensor", "idleTime").add("host", "solomon-1"),
            AggrGraphDataArrayList.of(
                lpoint("2017-09-07T11:00:00Z", 5),
                lpoint("2017-09-07T11:00:01Z", 6)
            )
        );

        addMetric(labels.add("sensor", "idleTime").add("host", "solomon-2"),
            AggrGraphDataArrayList.of(
                lpoint("2017-09-07T11:00:00Z", 15),
                lpoint("2017-09-07T11:00:01Z", 16)
            )
        );
        addMetric(labels.add("sensor", "busyTime").add("host", "solomon-1"),
            AggrGraphDataArrayList.of(
                lpoint("2017-09-07T11:00:00Z", 1005),
                lpoint("2017-09-07T11:00:01Z", 1006)
            )
        );

        addMetric(labels.add("sensor", "busyTime").add("host", "solomon-2"),
            AggrGraphDataArrayList.of(
                lpoint("2017-09-07T11:00:00Z", 1015),
                lpoint("2017-09-07T11:00:01Z", 1016)
            )
        );

        Instant now = Instant.parse("2017-09-07T14:00:00Z");

        assertThat(syncCheck(SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("host", "solomon-1"))
                .build(), now), equalTo(EvaluationStatus.Code.OK));

        assertThat(syncCheck(SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("host", "solomon-2"))
                .build(), now), equalTo(EvaluationStatus.Code.WARN));
    }

    private AlertRuleDeadlines deadline() {
        return AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
    }

    private MetricId randomMetricId() {
        return solomon.randomMetricId();
    }

    private void addMetric(Labels labels, AggrGraphDataArrayList source) {
        solomon.addMetric(labels, source);
    }

    private void addMetric(MetricId metricId, Labels labels, AggrGraphDataArrayList source) {
        solomon.addMetric(metricId, labels, source);
    }

    private EvaluationStatus.Code syncCheck(ThresholdAlert alert, Instant now) {
        return syncCheckResult(alert, now).getCode();
    }

    private EvaluationStatus.Code syncCheck(SubAlert alert, Instant now) {
        return syncCheckResult(alert, now).getCode();
    }

    private EvaluationStatus syncCheckResult(ThresholdAlert alert, Instant now) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        AlertRule rule = alertRuleFactory.createAlertRule(alert);
        CompletableFuture<EvaluationStatus> future = rule.eval(now, deadlines);
        return future.join();
    }

    private EvaluationStatus syncCheckResult(SubAlert alert, Instant now) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        AlertRule rule = alertRuleFactory.createAlertRule(alert);
        CompletableFuture<EvaluationStatus> future = rule.eval(now, deadlines);
        return future.join();
    }
}
