package ru.yandex.solomon.alert.rule;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.rule.usage.ProjectAlertRuleMetrics;
import ru.yandex.solomon.alert.statuses.AlertingStatusesSelector;
import ru.yandex.solomon.alert.statuses.AlertingStatusesSelectorImpl;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.TemplateFactory;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.labels.shard.ShardKey;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCache;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.solomon.alert.util.TimeSeriesTestSupport.point;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertRuleFactoryImplTest {

    @Rule
    public TestName name = new TestName();

    private SolomonClientStub solomon;
    private AlertRuleFactoryImpl ruleFactory;

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
        AlertingStatusesSelector alertingStatuses = new AlertingStatusesSelectorImpl(
                new ShardKey("solomon", "testing", "alerting_statuses"));
        MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(metricsClient, metabaseFindCache);
        var featureFlags = new FeatureFlagHolderStub();
        ruleFactory = new AlertRuleFactoryImpl(
                cachingMetricsClient,
                new ProjectAlertRuleMetrics(),
                templateFactory,
                alertingStatuses,
                featureFlags);
    }

    @After
    public void tearDown() throws Exception {
        solomon.close();
    }

    @Test
    public void thresholdOk() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01");

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:30:00Z", 1),
                        point("2017-09-07T11:33:00Z", 2),
                        point("2017-09-07T11:45:00Z", 3),
                        point("2017-09-07T12:00:00Z", 4)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.GTE, 10, TargetStatus.ALARM))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void thresholdAlarm() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01");

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:30:00Z", 1),
                        point("2017-09-07T11:33:00Z", 2),
                        point("2017-09-07T11:45:00Z", 3),
                        point("2017-09-07T12:00:00Z", 4)
                )
        );

        ThresholdAlert alert = newThresholdAlert()
                .setSelectors(Selectors.of(labels))
                .setPredicateRule(PredicateRule.of(ThresholdType.AT_LEAST_ONE, Compare.LTE, 3, TargetStatus.ALARM))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void expressionOk() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01");

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:30:00Z", 5),
                        point("2017-09-07T11:33:00Z", 3),
                        point("2017-09-07T11:45:00Z", 12),
                        point("2017-09-07T12:00:00Z", 4)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("max(" + labels + ") >= 50")
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void expressionAlarm() throws Exception {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "misc",
            "sensor", "idleTime",
            "host", "solomon-01");

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-07T11:30:00Z", 5),
                        point("2017-09-07T11:33:00Z", 3),
                        point("2017-09-07T11:45:00Z", 12),
                        point("2017-09-07T12:00:00Z", 4)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("max(" + labels + ") <= 40")
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus.Code status = syncCheck(alert, now);
        assertThat(status, equalTo(EvaluationStatus.Code.ALARM));
    }

    private EvaluationStatus.Code syncCheck(Alert alert, Instant now) {
        AlertRuleDeadlines deadlines = AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
        AlertRule rule = ruleFactory.createAlertRule(alert);
        return rule.eval(now, deadlines).join().getCode();
    }

    private ThresholdAlert.Builder newThresholdAlert() {
        return ThresholdAlert.newBuilder()
                .setId(name.getMethodName())
                .setProjectId("junk");
    }

    private ExpressionAlert.Builder newExpressionAlert() {
        return ExpressionAlert.newBuilder()
                .setId(name.getMethodName())
                .setProjectId("junk");
    }
}
