package ru.yandex.solomon.alert.rule.expression;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.NoPointsPolicy;
import ru.yandex.solomon.alert.domain.ResolvedEmptyPolicy;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.expression.ExpressionAlert;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.AlertRuleFactory;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryImpl;
import ru.yandex.solomon.alert.rule.AlertRuleFairDeadlines;
import ru.yandex.solomon.alert.rule.AlertTimeSeries;
import ru.yandex.solomon.alert.rule.ExplainResult;
import ru.yandex.solomon.alert.rule.usage.ProjectAlertRuleMetrics;
import ru.yandex.solomon.alert.statuses.AlertingStatusesSelector;
import ru.yandex.solomon.alert.statuses.AlertingStatusesSelectorImpl;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.TemplateFactory;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.labels.LabelsFormat;
import ru.yandex.solomon.labels.shard.ShardKey;
import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.MetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.metrics.client.cache.CachingMetricsClientImpl;
import ru.yandex.solomon.metrics.client.cache.FindCacheOptions;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCache;
import ru.yandex.solomon.metrics.client.cache.MetabaseFindCacheImpl;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.stockpile.api.EStockpileStatusCode;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.util.TimeSeriesTestSupport.point;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;


/**
 * @author Vladimir Gordiychuk
 */
public class ExpressionAlertRuleTest {
    @Rule
    public TestName name = new TestName();

    private SolomonClientStub solomon;
    private MetricsClient metricsClient;
    private FindCacheOptions cacheOptions;
    private AlertRuleFactory alertRuleFactory;
    private Map<Alert, AlertRule> ruleByAlert = new HashMap<>();
    private int maxMetricsToLoad;

    @Before
    public void setUp() throws Exception {
        solomon = new SolomonClientStub();
        metricsClient = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        TemplateFactory templateFactory = new MustacheTemplateFactory();
        cacheOptions = FindCacheOptions.newBuilder()
                .setExpireTtl(1, TimeUnit.DAYS)
                .setRefreshInterval(10, TimeUnit.MILLISECONDS)
                .setMaxSize(1000)
                .build();
        maxMetricsToLoad = 100;
        MetabaseFindCache metabaseFindCache = new MetabaseFindCacheImpl(metricsClient, cacheOptions);
        AlertingStatusesSelector alertingStatuses = new AlertingStatusesSelectorImpl(new ShardKey(
                "solomon", "testing", "alerting_statuses"));
        MetricsClient cachingMetricsClient = new CachingMetricsClientImpl(metricsClient, metabaseFindCache);
        var featureFlags = new FeatureFlagHolderStub();
        alertRuleFactory = new AlertRuleFactoryImpl(
                cachingMetricsClient,
                new ProjectAlertRuleMetrics(),
                templateFactory,
                alertingStatuses,
                featureFlags);
    }

    @After
    public void tearDown() throws Exception {
        solomon.close();
        ruleByAlert.clear();
    }

    @Test
    public void noDataWhenBySelectorNotAbleFindMetrics() throws Exception {
        // noise metric
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-01"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(10))
                .setCheckExpression("max({project=solomon, cluster=foo, service=bar, sensor=usedTime, host='solomon-01'}) >= 3")
                .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.NO_DATA));
    }

    @Test
    public void warnWhenNotAbleFindMetrics() {
        // noise metric
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "kikimr-01"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(10))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.WARN)
                .setCheckExpression("max({project=solomon, cluster=foo, service=bar, sensor=usedTime, host='solomon-01'}) >= 3")
                .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.WARN));
    }

    @Test
    public void okWhenFoundSomeMetrics() {
        Labels oks = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "requests",
                "code", "200"
        );

        solomon.addMetric(oks,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(10))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.OK)
                .setProgram("let oks = requests{code='200'}; let errs = requests{code='500'};")
                .setCheckExpression("last(errs) > last(oks)")
                .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void manualWhenNotAbleFindMetrics() {
        // noise metric
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "kikimr-01"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(10))
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.MANUAL)
                .setCheckExpression("size({project=solomon, cluster=foo, service=bar, sensor=usedTime, host='solomon-01'}) < 1")
                .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void noRaceInPolicies() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "solomon-01"
        );

        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        for (var sensor : List.of("alpha", "beta", "gamma")) {
            Labels junk = labels.toBuilder().add("sensor", sensor).build();
            solomon.addMetric(junk, AggrGraphDataArrayList.of(
                    point("2017-09-08T09:30:00Z", 42)
            ));
        }

        for (int tries = 0; tries < 20; tries++) {
            ExpressionAlert alert = newExpressionAlert()
                    .setProjectId("solomon")
                    .setPeriod(Duration.ofMinutes(10))
                    .setResolvedEmptyPolicy(ResolvedEmptyPolicy.NO_DATA)
                    .setNoPointsPolicy(NoPointsPolicy.OK)
                    .setSeverity(AlertSeverity.CRITICAL)
                    .setProgram("""
                            let series = {cluster=foo, service=bar, sensor=usedTime, host='solomon-01'} +
                                         {cluster=foo, service=bar, sensor=idleTime, host='solomon-01'};
                            let alpha = {cluster=foo, service=bar, sensor=alpha, host='solomon-01'};
                            let beta = {cluster=foo, service=bar, sensor=beta, host='solomon-01'};
                            let gamma = {cluster=foo, service=bar, sensor=gamma, host='solomon-01'};
                            alarm_if(false);
                            """)
                    .build();

            Instant now = Instant.parse("2017-09-08T09:33:05Z")
                    // randomizing load interval to trigger random GraphDataLoadRequest::hashCode
                    .plusMillis(ThreadLocalRandom.current().nextInt(1000));
            EvaluationStatus.Code result = syncCheck(alert, now);
            assertThat(result, equalTo(EvaluationStatus.Code.NO_DATA));
        }
    }

    @Test
    public void okOnWindowLeft() throws Exception {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-04"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 2), // window start
                        point("2017-09-08T09:10:15Z", 8.1),
                        point("2017-09-08T09:10:30Z", 5.3),
                        point("2017-09-08T09:10:45Z", 4.2), // now
                        point("2017-09-08T09:11:00Z", 20),
                        point("2017-09-08T09:11:15Z", 30)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(45))
                .setCheckExpression("max({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-04'}) >= 10")
                .build();

        Instant now = Instant.parse("2017-09-08T09:10:45Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void okOnWindowRight() throws Exception {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-03"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 22),
                        point("2017-09-08T09:10:15Z", 123),
                        point("2017-09-08T09:10:30Z", 3), // window start
                        point("2017-09-08T09:10:45Z", 4.2),
                        point("2017-09-08T09:11:00Z", 5),
                        point("2017-09-08T09:11:15Z", 2.5) // now
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofSeconds(45))
                .setCheckExpression("max({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-03'}) >= 6")
                .build();

        Instant now = Instant.parse("2017-09-08T09:11:15Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void okOnWindow() throws Exception {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-02"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 3.5),
                        point("2017-09-08T09:11:00Z", 8.8),
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:13:00Z", 50),
                        point("2017-09-08T09:14:00Z", 22),
                        point("2017-09-08T09:15:00Z", 43), // now
                        point("2017-09-08T09:16:00Z", 1.2),
                        point("2017-09-08T09:17:00Z", 2.3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(3))
                .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-02'}) <= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void alarmOnWindow() throws Exception {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-13"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 3.5),
                        point("2017-09-08T09:11:00Z", 8.8),
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:13:00Z", 50),
                        point("2017-09-08T09:14:00Z", 22),
                        point("2017-09-08T09:15:00Z", 43), // now
                        point("2017-09-08T09:16:00Z", 1.2),
                        point("2017-09-08T09:17:00Z", 2.3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(3))
                .setCheckExpression("max({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-13'}) >= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void emptyIsOk() {
        ExpressionAlert alert = newExpressionAlert().build();

        assertThat(syncCheck(alert, Instant.now()), equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void multiStatus() {
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 3.5),
                        point("2017-09-08T09:11:00Z", 8.8),
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:13:00Z", 50),
                        point("2017-09-08T09:14:00Z", 22),
                        point("2017-09-08T09:15:00Z", 43), // now
                        point("2017-09-08T09:16:00Z", 1.2),
                        point("2017-09-08T09:17:00Z", 0.3)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(3))
                .setProgram("" +
                        "let data = idleTime{project=kikimr, cluster=foo, service=bar};\n" +
                        "no_data_if(count(data) == 0);\n" +
                        "let mv = max(data);\n" +
                        "alarm_if(mv >= 5);\n" +
                        "warn_if(mv >= 1);")
                .setAnnotations(
                        Map.of("value", "{{expression.mv}}",
                                "message", "" +
                                        "{{#isAlarm}}Everything's broken!{{/isAlarm}}" +
                                        "{{#isNoData}}Where's the data?{{/isNoData}}" +
                                        "{{#isWarn}}You'd better check your data{{/isWarn}}" +
                                        "{{#isOk}}Good night!{{/isOk}}"))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        {
            EvaluationStatus result = syncCheckResult(alert, now);
            assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ALARM));
            assertThat(result.getAnnotations().get("value"), not(isEmptyString()));
            assertThat(result.getAnnotations().get("message"), equalTo("Everything's broken!"));
        }
        {
            EvaluationStatus result = syncCheckResult(alert, now.plusSeconds(4 * 60));
            assertThat(result.getCode(), equalTo(EvaluationStatus.Code.WARN));
            assertThat(result.getAnnotations().get("value"), not(isEmptyString()));
            assertThat(result.getAnnotations().get("message"), equalTo("You'd better check your data"));
        }
        {
            EvaluationStatus result = syncCheckResult(alert, now.plusSeconds(5 * 60));
            assertThat(result.getCode(), equalTo(EvaluationStatus.Code.OK));
            assertThat(result.getAnnotations().get("value"), not(isEmptyString()));
            assertThat(result.getAnnotations().get("message"), equalTo("Good night!"));
        }
        {
            EvaluationStatus result = syncCheckResult(alert, now.plusSeconds(10 * 60));
            assertThat(result.getCode(), equalTo(EvaluationStatus.Code.NO_DATA));
            assertThat(result.getAnnotations().get("value"), isEmptyString());
            assertThat(result.getAnnotations().get("message"), equalTo("Where's the data?"));
        }

    }

    @Test
    public void noPointsPolicyAlarm() {
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "cluster"
        );

        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let data = {project=kikimr, cluster=foo, service=bar, sensor=idleTime, host=cluster};\n" +
                        "let aggrValue = max(data);\n")
                .setCheckExpression("aggrValue > 5 && aggrValue < 10")
                .setPeriod(Duration.ofMinutes(3))
                .setNoPointsPolicy(NoPointsPolicy.ALARM)
                .setSeverity(AlertSeverity.CRITICAL)
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void noPointsPolicyNoData() {
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "cluster"
        );

        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let data = {project=kikimr, cluster=foo, service=bar, sensor=idleTime, host=cluster};\n" +
                        "let aggrValue = last(histogram_sum(data));\n")
                .setCheckExpression("aggrValue > 5 && aggrValue < 10")
                .setPeriod(Duration.ofMinutes(3))
                .setNoPointsPolicy(NoPointsPolicy.NO_DATA)
                .setSeverity(AlertSeverity.CRITICAL)
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.NO_DATA));
    }


    @Test
    public void okOnRangeCondition() throws Exception {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "cluster"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 11),
                        point("2017-09-08T09:11:00Z", 32),
                        point("2017-09-08T09:12:00Z", 21), // window start
                        point("2017-09-08T09:13:00Z", 6),
                        point("2017-09-08T09:14:00Z", 55),
                        point("2017-09-08T09:15:00Z", 42), // now
                        point("2017-09-08T09:16:00Z", 4),
                        point("2017-09-08T09:17:00Z", 5)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let data = {project=kikimr, cluster=foo, service=bar, sensor=idleTime, host=cluster};\n" +
                        "let aggrValue = max(data);\n")
                .setCheckExpression("aggrValue > 5 && aggrValue < 10")
                .setPeriod(Duration.ofMinutes(3))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void alarmOnRangeCondition() throws Exception {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "cluster"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 11),
                        point("2017-09-08T09:11:00Z", -21),
                        point("2017-09-08T09:12:00Z", 21), // window start
                        point("2017-09-08T09:13:00Z", 8), // trigger alarm
                        point("2017-09-08T09:14:00Z", 55),
                        point("2017-09-08T09:15:00Z", 42), // now
                        point("2017-09-08T09:16:00Z", 4),
                        point("2017-09-08T09:17:00Z", 5)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let data = {project=kikimr, cluster=foo, service=bar, sensor=idleTime, host=cluster};\n" +
                        "let aggrValue = min(data);\n")
                .setCheckExpression("aggrValue > 5 && aggrValue < 10")
                .setPeriod(Duration.ofMinutes(3))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void okOnMultipleLines() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime"
        );

        solomon.addMetric(commonLabels.add("host", "kikimr-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 51),
                        point("2017-09-08T09:11:00Z", 55),
                        point("2017-09-08T09:12:00Z", 6), // window start
                        point("2017-09-08T09:13:00Z", 2),
                        point("2017-09-08T09:14:00Z", 5),
                        point("2017-09-08T09:15:00Z", 3), // now
                        point("2017-09-08T09:16:00Z", 61),
                        point("2017-09-08T09:17:00Z", 65)
                )
        );

        solomon.addMetric(commonLabels.add("host", "kikimr-02"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 41),
                        point("2017-09-08T09:11:00Z", 33),
                        point("2017-09-08T09:12:00Z", 2), // window start
                        point("2017-09-08T09:13:00Z", 3),
                        point("2017-09-08T09:14:00Z", 4),
                        point("2017-09-08T09:15:00Z", 8), // now
                        point("2017-09-08T09:16:00Z", 55),
                        point("2017-09-08T09:17:00Z", 92)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("max(group_lines('sum', {" + commonLabelsStr + ", host='kikimr-*'})) >= 15")
                .setPeriod(Duration.ofMinutes(3))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void alarmOnMultipleLines() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime"
        );

        solomon.addMetric(commonLabels.add("host", "kikimr-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 51),
                        point("2017-09-08T09:11:00Z", 55),
                        point("2017-09-08T09:12:00Z", 6), // window start
                        point("2017-09-08T09:13:00Z", 2),
                        point("2017-09-08T09:14:00Z", 5),
                        point("2017-09-08T09:15:00Z", 3), // now
                        point("2017-09-08T09:16:00Z", 61),
                        point("2017-09-08T09:17:00Z", 65)
                )
        );

        solomon.addMetric(commonLabels.add("host", "kikimr-02"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 41),
                        point("2017-09-08T09:11:00Z", 33),
                        point("2017-09-08T09:12:00Z", 2), // window start
                        point("2017-09-08T09:13:00Z", 3),
                        point("2017-09-08T09:14:00Z", 4),
                        point("2017-09-08T09:15:00Z", 8), // now
                        point("2017-09-08T09:16:00Z", 55),
                        point("2017-09-08T09:17:00Z", 92)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("max(group_lines('sum', {" + commonLabelsStr + ", host='kikimr-*'})) <= 15")
                .setPeriod(Duration.ofMinutes(3))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void deadlineCausedByMetabase() throws Exception {
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "kikimr-02"
        );
        solomon.addMetric(labels, AggrGraphDataArrayList.of());
        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.DEADLINE_EXCEEDED);

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofHours(1))
                .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-02'}) <= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(result.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.DEADLINE));
    }

    @Test
    public void deadlineCausedByStockpile() throws Exception {
        MetricId metricId = solomon.randomMetricId();
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-01");
        solomon.addMetric(metricId, labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.DEADLINE_EXCEEDED);
        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(5))
                .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-01'}) <= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(result.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.DEADLINE));
    }

    @Test
    public void unavailableCausedByStockpile() throws Exception {
        MetricId metricId = solomon.randomMetricId();
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-01");
        solomon.addMetric(metricId, labels,
            AggrGraphDataArrayList.of(
                point("2017-09-08T09:31:19Z", 1),
                point("2017-09-08T09:32:45Z", 2),
                point("2017-09-08T09:33:05Z", 3)
            )
        );

        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.SHARD_NOT_READY);
        ExpressionAlert alert = newExpressionAlert()
            .setPeriod(Duration.ofMinutes(5))
            .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-01'}) <= 5")
            .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(result.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.UNAVAILABLE));
    }

    @Test
    public void unavailableCausedByMetabase() throws Exception {
        MetricId metricId = solomon.randomMetricId();
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-01");
        solomon.addMetric(metricId, labels,
            AggrGraphDataArrayList.of(
                point("2017-09-08T09:31:19Z", 1),
                point("2017-09-08T09:32:45Z", 2),
                point("2017-09-08T09:33:05Z", 3)
            )
        );

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.NODE_UNAVAILABLE);
        ExpressionAlert alert = newExpressionAlert()
            .setPeriod(Duration.ofMinutes(5))
            .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-01'}) <= 5")
            .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(result.getErrorCode(), equalTo(EvaluationStatus.ErrorCode.UNAVAILABLE));
    }

    @Test
    public void errorCausedByMetabase() throws Exception {
        Labels labels = Labels.of(
                "project", "kikimr",
                "cluster", "foo",
                "service", "bar",
                "sensor", "idleTime",
                "host", "kikimr-02"
        );
        solomon.addMetric(labels, AggrGraphDataArrayList.of());
        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.INTERNAL_ERROR);

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofHours(1))
                .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-02'}) <= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        for (int index = 0; index < 3; index++) {
            EvaluationStatus.Code result = syncCheck(alert, now);
            assertThat(result, equalTo(EvaluationStatus.Code.ERROR));
        }
    }

    @Test
    public void errorCausedByProgram() {
        for (int i = 0; i < 5; i++) {
            Labels labels = Labels.of(
                    "project", "kikimr",
                    "cluster", "foo",
                    "service", "bar",
                    "sensor", "idleTime",
                    "host", "kikimr-0" + i);
            solomon.addMetric(labels, AggrGraphDataArrayList.empty());
        }

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofHours(1))
                .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-*'}) <= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        for (int index = 0; index < 3; index++) {
            EvaluationStatus result = syncCheckResult(alert, now);
            assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ERROR));
            assertThat(result.getDescription(), startsWith("evaluation error"));
        }
    }

    @Test
    public void reusePreviousResolveUntilMetabaseNotReady() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime");
        solomon.addMetric(commonLabels.add("host", "kikimr-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 51),
                        point("2017-09-08T09:11:00Z", 55),
                        point("2017-09-08T09:12:00Z", 6), // window start
                        point("2017-09-08T09:13:00Z", 2),
                        point("2017-09-08T09:14:00Z", 5),
                        point("2017-09-08T09:15:00Z", 3), // now
                        point("2017-09-08T09:16:00Z", 61),
                        point("2017-09-08T09:17:00Z", 65)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("max(group_lines('sum', {" + commonLabelsStr + ", host='kikimr-*'})) >= 15")
                .setPeriod(Duration.ofMinutes(3))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.DEADLINE_EXCEEDED);
        assertThat(syncCheck(alert, now), equalTo(EvaluationStatus.Code.OK));

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.OK);
        solomon.addMetric(commonLabels.add("host", "kikimr-02"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 41),
                        point("2017-09-08T09:11:00Z", 33),
                        point("2017-09-08T09:12:00Z", 100), // window start
                        point("2017-09-08T09:13:00Z", 200),
                        point("2017-09-08T09:14:00Z", 300),
                        point("2017-09-08T09:15:00Z", 400), // now
                        point("2017-09-08T09:16:00Z", 55),
                        point("2017-09-08T09:17:00Z", 92)
                )
        );

        EvaluationStatus.Code previos;
        do {
            previos = syncCheck(alert, now);
        } while (previos == EvaluationStatus.Code.OK);

        assertThat(previos, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void errorCausedByStockpile() throws Exception {
        MetricId metricId = solomon.randomMetricId();
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-01"
        );

        solomon.addMetric(metricId, labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        solomon.getStockpile().predefineStatusCodeForMetric(metricId, EStockpileStatusCode.INTERNAL_ERROR);
        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(5))
                .setCheckExpression("min({project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-01'}) <= 5")
                .build();

        Instant now = Instant.parse("2017-09-08T09:33:05Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ERROR));
    }

    @Test
    public void errorCausedByOneOfTheMetric() throws Exception {
        MetricId failedMetricId = solomon.randomMetricId();
        Labels failedLabels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-01"
        );

        solomon.addMetric(failedMetricId, failedLabels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        MetricId goodMetricId = solomon.randomMetricId();
        Labels goodLabels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-02"
        );

        solomon.addMetric(goodMetricId, goodLabels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 5),
                        point("2017-09-08T09:32:45Z", 1),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );

        solomon.getStockpile().predefineStatusCodeForMetric(failedMetricId, EStockpileStatusCode.INTERNAL_ERROR);

        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("max(group_lines('sum', {project=kikimr, cluster=foo, service=bar, sensor=idleTime, host='kikimr-??'})) <= 15")
                .setPeriod(Duration.ofMinutes(2))
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ERROR));
    }

    @Test
    public void okOnMultiMultiLines() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "common",
            "service", "blob",
            "sensor", "idleTime"
        );

        // dc=MAN sum per dc = 50
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "MAN","host", "kikimr-01")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 10),
                        point("2017-09-08T09:32:45Z", 15),
                        point("2017-09-08T09:33:05Z", 20)
                )
        );
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "MAN","host", "kikimr-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:22Z", 4),
                        point("2017-09-08T09:32:50Z", 0.5),
                        point("2017-09-08T09:33:51Z", 0.5)
                )
        );

        // dc=SAS sum per dc = 50
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "SAS","host", "kikimr-01")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:09Z", 5),
                        point("2017-09-08T09:32:34Z", 15),
                        point("2017-09-08T09:33:41Z", 10)
                )
        );
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "SAS","host", "kikimr-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:31Z", 10),
                        point("2017-09-08T09:32:46Z", 10),
                        point("2017-09-08T09:33:55Z", 0)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let man = {" + commonLabelsStr + ", dc=MAN, host='*'};\n" +
                        "let sas = {" + commonLabelsStr + ", dc=SAS, host='*'};\n" +
                        "let manSum = sum(group_lines('sum', man));\n" +
                        "let sasSum = sum(group_lines('sum', sas));\n")
                .setCheckExpression("manSum != sasSum") // manSum = 50 and sasSum = 50
                .setPeriod(Duration.ofMinutes(10))
                .build();

        Instant now = Instant.parse("2017-09-08T09:40:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void alarmOnMultiMultiLines() throws Exception {
        Labels commonLabels = Labels.of("project", "kikimr","cluster", "common","service", "blob","sensor", "idleTime");
        // dc=MAN
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "MAN","host", "kikimr-01")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 2),
                        point("2017-09-08T09:32:45Z", 1),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "MAN","host", "kikimr-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:22Z", 5),
                        point("2017-09-08T09:32:50Z", 3),
                        point("2017-09-08T09:33:51Z", 8)
                )
        );

        // dc=SAS sum per dc = 50
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "SAS","host", "kikimr-01")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:09Z", 10),
                        point("2017-09-08T09:32:34Z", 51),
                        point("2017-09-08T09:33:41Z", 21)
                )
        );
        solomon.addMetric(commonLabels.addAll(Labels.of("dc", "SAS","host", "kikimr-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:31Z", 44),
                        point("2017-09-08T09:32:46Z", 21),
                        point("2017-09-08T09:33:55Z", 13)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let man = {" + commonLabelsStr + ", dc=MAN, host='*'};\n" +
                        "let sas = {" + commonLabelsStr + ", dc=SAS, host='*'};\n" +
                        "let manSum = sum(group_lines('sum', man));\n" +
                        "let sasSum = sum(group_lines('sum', sas));\n")
                .setCheckExpression("manSum != sasSum") // manSum = 50 and sasSum = 50
                .setPeriod(Duration.ofMinutes(10))
                .build();

        Instant now = Instant.parse("2017-09-08T09:40:00Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void groupByHostCheck() throws Exception {
        Labels commonLabels = Labels.of(
            "project", "kikimr",
            "cluster", "common",
            "service", "blob"
        );

        // host=kikimr-01
        // inFlight=5;2;7
        solomon.addMetric(commonLabels.addAll(Labels.of("sensor", "requestStarted", "host", "kikimr-01")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:00:00Z", 10),
                        point("2017-09-08T09:01:00Z", 20),
                        point("2017-09-08T09:02:00Z", 40)
                )
        );
        solomon.addMetric(commonLabels.addAll(Labels.of("sensor", "requestCompleted","host", "kikimr-01")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:00:00Z", 5),
                        point("2017-09-08T09:01:00Z", 18),
                        point("2017-09-08T09:02:00Z", 33)
                )
        );

        // host=kikimr-02
        // InFlight=8;12;20
        solomon.addMetric(commonLabels.addAll(Labels.of("sensor", "requestStarted","host", "kikimr-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:00:00Z", 10),
                        point("2017-09-08T09:01:00Z", 20),
                        point("2017-09-08T09:02:00Z", 40)
                )
        );
        solomon.addMetric(commonLabels.addAll(Labels.of("sensor", "requestCompleted","host", "kikimr-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:00:00Z", 2),
                        point("2017-09-08T09:01:00Z", 8),
                        point("2017-09-08T09:02:00Z", 20)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert parent = newExpressionAlert()
                .setProgram("" +
                        "let started = {" + commonLabelsStr + ", sensor=requestStarted};\n" +
                        "let completed = {" + commonLabelsStr + ", sensor=requestCompleted};\n" +
                        "let inFlight = started - completed;\n")
                .setCheckExpression("max(inFlight) >= 15")
                .setPeriod(Duration.ofMinutes(5))
                .setGroupByLabel("host")
                .build();


        Instant now = Instant.parse("2017-09-08T09:03:00Z");
        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("host", "kikimr-01"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.OK)
        );

        assertThat(
                syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("host", "kikimr-02"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.ALARM)
        );
    }

    @Test
    public void subAlertWithSameSelector() {
        Labels commonLabels = Labels.of(
            "project", "solomon",
            "cluster", "prod",
            "service", "stockpile"
        );


        solomon.addMetric(commonLabels.addAll(Labels.of("sensor", "readyShards","host", "solomon-02")),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T08:55:00Z", 10),
                        point("2017-09-08T08:56:00Z", 10),
                        point("2017-09-08T08:57:00Z", 10),
                        point("2017-09-08T08:58:00Z", 10),
                        point("2017-09-08T08:59:00Z", 10),

                        point("2017-09-08T09:01:00Z", 1),
                        point("2017-09-08T09:02:00Z", 2),
                        point("2017-09-08T09:03:00Z", 3),
                        point("2017-09-08T09:04:00Z", 4),
                        point("2017-09-08T09:05:00Z", 5)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert parent = newExpressionAlert()
                .setProgram("" +
                        "let current = {" + commonLabelsStr  + ", host!=cluster, sensor=readyShards};\n" +
                        "let past = shift({" + commonLabelsStr + ", host!=cluster, sensor=readyShards}, 5m);")
                .setCheckExpression("avg(past) - avg(current) > 0")
                .setPeriod(Duration.ofMinutes(5))
                .setGroupByLabel("host")
                .build();

        Instant now = Instant.parse("2017-09-08T09:05:00Z");
        assertThat(syncCheck(SubAlert.newBuilder()
                        .setParent(parent)
                        .setGroupKey(Labels.of("host", "solomon-02"))
                        .build(), now),
                equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void expressionWithoutMetrics() {
        ExpressionAlert alert = newExpressionAlert()
                .setCheckExpression("random01() < 0.5")
                .setPeriod(Duration.ofMinutes(1))
                .build();

        AlertRule rule = alertRuleFactory.createAlertRule(alert);
        Set<EvaluationStatus.Code> codes = new HashSet<>();

        Instant now = Instant.parse("2017-09-08T09:05:00Z");
        for (int index = 0; index < 100; index++) {
            codes.add(rule.eval(now, deadline())
                    .thenApply(EvaluationStatus::getCode)
                    .join());

            now = now.plus(1, ChronoUnit.MINUTES);
        }

        assertThat(codes,
                allOf(
                        iterableWithSize(2),
                        hasItem(EvaluationStatus.Code.OK),
                        hasItem(EvaluationStatus.Code.ALARM)));
    }

    @Test
    public void annotationTrafficLights() {
        Labels commonLabels = Labels.of("project", "kikimr","cluster", "foo","service", "bar","sensor", "idleTime");
        solomon.addMetric(commonLabels.add("host", "kikimr-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 51),
                        point("2017-09-08T09:11:00Z", 55),
                        point("2017-09-08T09:12:00Z", 6), // window start
                        point("2017-09-08T09:13:00Z", 2),
                        point("2017-09-08T09:14:00Z", 5),
                        point("2017-09-08T09:15:00Z", 8), // now
                        point("2017-09-08T09:16:00Z", 61),
                        point("2017-09-08T09:17:00Z", 65)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let metrics = {" + commonLabelsStr + ", host='*'};\n" +
                        "let overLineSum = group_lines('sum', metrics);\n" +
                        "let last = last(overLineSum);\n" +
                        "let color = (last >= 10) ? 'red' : ((last >= 5) ? 'yellow' : 'green');")
                .setCheckExpression("last >= 10")
                .setPeriod(Duration.ofMinutes(3))
                .setAnnotations(
                        ImmutableMap.<String, String>builder()
                                .put("from", "{{fromTime}}")
                                .put("to", "{{toTime}}")
                                .put("color", "{{expression.color}}")
                                .put("last", "{{expression.last}}")
                                .put("overLineSum", "{{{expression.overLineSum}}}")
                                .put("constantKey", "constantValue")
                                .put("message", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}")
                                .build()
                )
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.OK));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("from", "2017-09-08T09:12:00Z"));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("to", "2017-09-08T09:15:00Z"));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("last", "5.0"));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("color", "yellow"));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("constantKey", "constantValue"));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("message", "Good night!"));
    }

    @Test
    public void annotationCompacted() {
        Labels commonLabels = Labels.of("project", "kikimr","cluster", "foo","service", "bar","sensor", "idleTime");
        for (int i = 0; i < 20; i++) {
            solomon.addMetric(commonLabels.add("host", "kikimr-" + i), generateMetric("2017-09-08T09:10:00Z", 100, 1, 1000 * i));
        }

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("let metrics = {" + commonLabelsStr + ", host='*'};\nlet val = last(group_lines('sum', metrics));")
                .setCheckExpression("val >= 10")
                .setPeriod(Duration.ofMinutes(10))
                .setAnnotations(
                        ImmutableMap.<String, String>builder()
                                .put("from", "{{fromTime}}")
                                .put("to", "{{toTime}}")
                                .put("metrics", "{{{expression.metrics}}}")
                                .put("message", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}")
                                .build()
                )
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ALARM));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("from", "2017-09-08T09:05:00Z"));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("to", "2017-09-08T09:15:00Z"));
        String metricsAnnotation = result.getAnnotations().get("metrics");
        MatcherAssert.assertThat(metricsAnnotation, containsString("<15 more elements>"));
        MatcherAssert.assertThat(metricsAnnotation, containsString("<50 more>"));
        MatcherAssert.assertThat(metricsAnnotation.length(), lessThan(6 * (10 * 32 + 20) + 30));
        MatcherAssert.assertThat(result.getAnnotations(), hasEntry("message", "Everything broken!"));
    }

    @Test
    public void processTemplateWhenError() {
        Labels commonLabels = Labels.of("project", "kikimr","cluster", "foo","service", "bar","sensor", "idleTime");
        solomon.addMetric(commonLabels.add("host", "kikimr-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 51),
                        point("2017-09-08T09:11:00Z", 55),
                        point("2017-09-08T09:12:00Z", 6), // window start
                        point("2017-09-08T09:13:00Z", 2),
                        point("2017-09-08T09:14:00Z", 5),
                        point("2017-09-08T09:15:00Z", 8), // now
                        point("2017-09-08T09:16:00Z", 61),
                        point("2017-09-08T09:17:00Z", 65)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let metrics = {" + commonLabelsStr + ", host='*'};\n" +
                        "let overLineSum = group_lines('sum', metrics);\n" +
                        "let last = last(overLineSum);\n" +
                        "let color = (last >= 10) ? 'red' : ((last >= 5) ? 'yellow' : 'green');")
                .setCheckExpression("last >= 10")
                .setPeriod(Duration.ofMinutes(3))
                .setAnnotations(
                        ImmutableMap.<String, String>builder()
                                .put("service", "hi")
                                .put("message", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}")
                                .build()
                )
                .build();

        solomon.getMetabase().predefineStatusCode(EMetabaseStatusCode.INTERNAL_ERROR);
        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(alert, now);
        assertThat(result.getCode(), equalTo(EvaluationStatus.Code.ERROR));
        assertThat(result.getAnnotations(), hasEntry("causedBy", "Metabase - INTERNAL_ERROR: [test] Predefined status code on stub on find metrics {cluster='foo', project='kikimr', sensor='idleTime', service='bar', host='*'}"));
        assertThat(result.getAnnotations(), hasEntry("service", "hi"));
    }

    @Test
    public void explainWithoutMetrics() {
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("let is_red = true;")
                .setCheckExpression("is_red")
                .setPeriod(Duration.ofMinutes(1))
                .build();

        AlertRule rule = alertRuleFactory.createAlertRule(alert);

        Instant now = Instant.now();
        ExplainResult result = rule.explain(now, deadline()).join();
        assertThat(result.getStatus().getCode(), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(result.getSeries(), emptyIterable());
    }

    @Test
    public void explainTimeseriesWindow() {
        Labels commonLabels = Labels.of("project", "kikimr","cluster", "foo","service", "bar","sensor", "idleTime");
        solomon.addMetric(commonLabels.add("host", "kikimr-01"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 51),
                        point("2017-09-08T09:11:00Z", 55),
                        point("2017-09-08T09:12:00Z", 6), // window start
                        point("2017-09-08T09:13:00Z", 2),
                        point("2017-09-08T09:14:00Z", 5),
                        point("2017-09-08T09:15:00Z", 8), // now
                        point("2017-09-08T09:16:00Z", 61),
                        point("2017-09-08T09:17:00Z", 65)
                )
        );

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let metrics = {" + commonLabelsStr + ", host='*'};\n" +
                        "let overLineSum = group_lines('sum', metrics);\n" +
                        "let last = last(overLineSum);\n" +
                        "let color = (last >= 10) ? 'red' : ((last >= 5) ? 'yellow' : 'green');")
                .setCheckExpression("last >= 10")
                .setPeriod(Duration.ofMinutes(3))
                .setAnnotations(
                        ImmutableMap.<String, String>builder()
                                .put("from", "{{fromTime}}")
                                .put("to", "{{toTime}}")
                                .put("color", "{{expression.color}}")
                                .put("last", "{{expression.last}}")
                                .put("overLineSum", "{{{expression.overLineSum}}}")
                                .put("constantKey", "constantValue")
                                .put("message", "{{#isAlarm}}Everything broken!{{/isAlarm}}{{#isOk}}Good night!{{/isOk}}")
                                .build()
                )
                .build();

        Labels labels = commonLabels.add("host", "kikimr-01");
        AggrGraphDataArrayList expectedTimeSeries = AggrGraphDataArrayList.of(
                point("2017-09-08T09:12:00Z", 6),
                point("2017-09-08T09:13:00Z", 2),
                point("2017-09-08T09:14:00Z", 5)
        );

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        AlertRule rule = alertRuleFactory.createAlertRule(alert);
        ExplainResult result = rule.explain(now, deadline()).join();
        assertThat(result.getStatus().getCode(), equalTo(EvaluationStatus.Code.OK));
        var metrics = new AlertTimeSeries("metrics", labels, MetricType.DGAUGE, expectedTimeSeries);
        var overLineSum = new AlertTimeSeries("overLineSum", labels, MetricType.DGAUGE, expectedTimeSeries);
        assertThat(result.getSeries(), hasItem(metrics));
        assertThat(result.getSeries(), hasItem(overLineSum));
    }

    @Test
    public void delaySeconds() {
        Labels labels = Labels.of(
            "project", "solomon",
            "cluster", "foo",
            "service", "bar",
            "sensor", "usedTime",
            "host", "solomon-01"
        );

        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:10:00Z", 1),
                        point("2017-09-08T09:20:00Z", 2),
                        point("2017-09-08T09:30:00Z", 3),
                        point("2017-09-08T09:40:00Z", 6),
                        point("2017-09-08T09:50:00Z", 7),
                        point("2017-09-08T10:00:00Z", 8)
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(30))
                .setProgram("let usedTime = {project=solomon, cluster=foo, service=bar, sensor=usedTime, host='solomon-01'};")
                .setCheckExpression("last(usedTime) >= 7")
                .setDelaySeconds(Math.toIntExact(TimeUnit.MINUTES.toSeconds(30L)))
                .build();

        // expected window {2017-09-08T09:00:00Z - 2017-09-08T09:30:00Z}
        MatcherAssert.assertThat(syncCheck(alert, Instant.parse("2017-09-08T10:00:00Z")), IsEqual.equalTo(EvaluationStatus.Code.OK));
        // expected window {2017-09-08T09:10:00Z - 2017-09-08T09:40:00Z}
        MatcherAssert.assertThat(syncCheck(alert, Instant.parse("2017-09-08T10:10:00Z")), IsEqual.equalTo(EvaluationStatus.Code.OK));
        // expected window {2017-09-08T09:30:00Z - 2017-09-08T10:00:00Z}
        MatcherAssert.assertThat(syncCheck(alert, Instant.parse("2017-09-08T10:30:00Z")), IsEqual.equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void limitMetricsLoad() {
        Labels commonLabels = Labels.of("project", "kikimr","cluster", "foo","service", "bar","sensor", "idleTime");
        for (int index = 0; index < maxMetricsToLoad + 1; index++) {
            Labels labels = commonLabels.add("host", "solomon-" + index);
            AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                    point("2017-09-07T11:45:00Z", ThreadLocalRandom.current().nextDouble()));
            solomon.addMetric(labels, source);
        }

        String commonLabelsStr = LabelsFormat.format(commonLabels);
        ExpressionAlert alert = newExpressionAlert()
                .setProgram("" +
                        "let metrics = {" + commonLabelsStr + ", host='*'};\n" +
                        "let overLineSum = group_lines('sum', metrics);\n" +
                        "let last = last(overLineSum);")
                .setCheckExpression("last >= 10")
                .setPeriod(Duration.ofMinutes(3))
                .build();

        assertThat(syncCheck(alert, Instant.parse("2017-09-07T12:00:00Z")), equalTo(EvaluationStatus.Code.ERROR));
    }

    @Test
    public void useDefaultProject() {
        solomon.addMetric(Labels.of("project", "alice", "sensor", "idleTime"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:14:00Z", 22),
                        point("2017-09-08T09:15:00Z", 43) // now
                )
        );

        // should not be used
        solomon.addMetric(Labels.of("project", "bob","sensor", "idleTime"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:12:00Z", 100), // window start
                        point("2017-09-08T09:14:00Z", 200),
                        point("2017-09-08T09:15:00Z", 400) // now
                )
        );

        solomon.addMetric(Labels.of("project", "alice","sensor", "useTime"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:14:00Z", 20),
                        point("2017-09-08T09:15:00Z", 30) // now
                )
        );

        ExpressionAlert alert = newExpressionAlert()
                .setProjectId("alice")
                .setPeriod(Duration.ofMinutes(3))
                .setProgram("let diff = {sensor=='idleTime'} - {sensor=='useTime'};")
                .setCheckExpression("max(diff) <= 50")
                .build();

        ExpressionAlert bob = newExpressionAlert()
                .setProjectId("bob")
                .setPeriod(Duration.ofMinutes(3))
                .setProgram("let diff = {sensor=='idleTime'} - {project='alice', sensor=='useTime'};")
                .setCheckExpression("max(diff) <= 50")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        assertThat(syncCheck(alert, now), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(syncCheck(bob, now), equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void useDefaultProjectForSubAlert() {
        solomon.addMetric(Labels.of("project", "alice","sensor", "idleTime","host", "test"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:14:00Z", 22),
                        point("2017-09-08T09:15:00Z", 43) // now
                )
        );

        // should not be used
        solomon.addMetric(Labels.of("project", "bob","sensor", "idleTime","host", "test"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:12:00Z", 100), // window start
                        point("2017-09-08T09:14:00Z", 200),
                        point("2017-09-08T09:15:00Z", 400) // now
                )
        );

        solomon.addMetric(Labels.of("project", "alice","sensor", "useTime","host", "test"),
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:12:00Z", 10), // window start
                        point("2017-09-08T09:14:00Z", 20),
                        point("2017-09-08T09:15:00Z", 30) // now
                )
        );

        ExpressionAlert alice = newExpressionAlert()
                .setProjectId("alice")
                .setPeriod(Duration.ofMinutes(3))
                .setProgram("let diff = {sensor=='idleTime'} - {sensor=='useTime'};")
                .setCheckExpression("max(diff) <= 50")
                .setGroupByLabel("host")
                .build();

        ExpressionAlert bob = newExpressionAlert()
                .setProjectId("bob")
                .setPeriod(Duration.ofMinutes(3))
                .setProgram("let diff = {sensor=='idleTime'} - {project='alice', sensor=='useTime'};")
                .setCheckExpression("max(diff) <= 50")
                .setGroupByLabel("host")
                .build();

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        assertThat(syncCheck(SubAlert.newBuilder()
                .setParent(alice)
                .setGroupKey(Labels.of("host", "test"))
                .build(), now), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(syncCheck(SubAlert.newBuilder()
                .setParent(bob)
                .setGroupKey(Labels.of("host", "test"))
                .build(), now), equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void okOnWindowWithLongPoints() {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-04"
        );

        solomon.addMetric(labels,
            AggrGraphDataArrayList.of(
                lpoint("2017-09-08T09:10:00Z", 2), // window start
                lpoint("2017-09-08T09:10:15Z", 8),
                lpoint("2017-09-08T09:10:30Z", 5),
                lpoint("2017-09-08T09:10:45Z", 4), // now
                lpoint("2017-09-08T09:11:00Z", 20),
                lpoint("2017-09-08T09:11:15Z", 30)
            )
        );

        ExpressionAlert alert = newExpressionAlert()
            .setPeriod(Duration.ofMinutes(45))
            .setCheckExpression("max({project='kikimr', cluster='foo', service='bar', sensor='idleTime', host='kikimr-04'}) >= 9")
            .build();

        Instant now = Instant.parse("2017-09-08T09:10:45Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.OK));
    }

    @Test
    public void excludeMetricFromUnroll() {
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test");
        String labelsStr = LabelsFormat.format(labels);
        ExpressionAlert parent = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setPeriod(Duration.ofMinutes(3))
                .setProgram(new StringBuilder()
                        .append("let source = {" + labelsStr + ", sensor=quoteUsed};\n")
                        .append("let etalon = {" + labelsStr + ", sensor=quoteLimit, host='-'};\n")
                        .append("let used = source / etalon;\n")
                        .toString())
                .setCheckExpression("last(used) > 0.8")
                .setGroupByLabel("host")
                .build();

        solomon.addMetric(labels.addAll(Labels.of("sensor", "quoteUsed","host", "solomon-1")), AggrGraphDataArrayList.of(
                point("2017-09-08T09:12:00Z", 10), // window start
                point("2017-09-08T09:14:00Z", 11),
                point("2017-09-08T09:15:00Z", 12) // now
        ));
        solomon.addMetric(labels.addAll(Labels.of("sensor", "quoteUsed","host", "solomon-2")), AggrGraphDataArrayList.of(
                point("2017-09-08T09:12:00Z", 98), // window start
                point("2017-09-08T09:14:00Z", 99),
                point("2017-09-08T09:15:00Z", 98) // now
        ));
        solomon.addMetric(labels.add("sensor", "quoteLimit"), AggrGraphDataArrayList.of(
                point("2017-09-08T09:12:00Z", 100), // window start
                point("2017-09-08T09:14:00Z", 100),
                point("2017-09-08T09:15:00Z", 100) // now
        ));

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        assertThat(syncCheck(SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("host", "solomon-1"))
                .build(), now), equalTo(EvaluationStatus.Code.OK));

        assertThat(syncCheck(SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("host", "solomon-2"))
                .build(), now), equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void alertEvaluationHistory() {
        Labels labels = Labels.of(Map.of(
                "project", "solomon",
                "cluster", "testing",
                "service", "alerting_statuses",
                "sensor", "alert.evaluation.status",
                "projectId", "test",
                "alertId", "alertEvaluationHistory"
        ));
        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );
        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(10))
                .setCheckExpression("max(alert_evaluation_history()) >= 3")
                .build();
        Instant now = Instant.parse("2017-09-08T09:33:06Z");
        EvaluationStatus.Code result = syncCheck(alert, now);
        assertThat(result, equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void noMetricPolicyPreservesAnnotation() {
        ExpressionAlert alert = newExpressionAlert()
                .setNoPointsPolicy(NoPointsPolicy.NO_DATA)
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.NO_DATA)
                .setSeverity(AlertSeverity.CRITICAL)
                .setProgram("let data = {project=solomon, cluster=foo, service=misc, sensor=idleTime}; alarm_if(avg(data) > 42);")
                .setAnnotations(Map.of("host", "solomon"))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        MatcherAssert.assertThat(status.getCode(), IsEqual.equalTo(EvaluationStatus.Code.NO_DATA));
        MatcherAssert.assertThat(status.getAnnotations().get("host"), IsEqual.equalTo("solomon"));
    }

    @Test
    public void noPointsPolicyPreservesAnnotation() {
        Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "foo",
                "service", "misc",
                "sensor", "idleTime"
        );

        solomon.addMetric(labels, AggrGraphDataArrayList.empty());

        ExpressionAlert alert = newExpressionAlert()
                .setNoPointsPolicy(NoPointsPolicy.NO_DATA)
                .setSeverity(AlertSeverity.CRITICAL)
                .setResolvedEmptyPolicy(ResolvedEmptyPolicy.NO_DATA)
                .setProgram("let data = {project=solomon, cluster=foo, service=misc, sensor=idleTime}; alarm_if(avg(data) > 42);")
                .setAnnotations(Map.of("host", "solomon"))
                .setPeriod(Duration.ofMinutes(30))
                .build();

        Instant now = Instant.parse("2017-09-07T12:00:00Z");
        EvaluationStatus status = syncCheckResult(alert, now);
        MatcherAssert.assertThat(status.getCode(), IsEqual.equalTo(EvaluationStatus.Code.NO_DATA));
        MatcherAssert.assertThat(status.getAnnotations().get("host"), IsEqual.equalTo("solomon"));
    }

    @Test
    public void alertEvaluationHistoryIsHidden() {
        Labels labels = Labels.of(Map.of(
                "project", "solomon",
                "cluster", "testing",
                "service", "alerting_statuses",
                "sensor", "alert.evaluation.status",
                "projectId", "test",
                "alertId", "alertEvaluationHistoryIsHidden"
        ));
        solomon.addMetric(labels,
                AggrGraphDataArrayList.of(
                        point("2017-09-08T09:31:19Z", 1),
                        point("2017-09-08T09:32:45Z", 2),
                        point("2017-09-08T09:33:05Z", 3)
                )
        );
        ExpressionAlert alert = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(10))
                .setCheckExpression("max(alert_evaluation_history()) >= 3")
                .build();
        Instant now = Instant.parse("2017-09-08T09:33:06Z");
        ExplainResult result = syncExplainResult(alert, now);
        assertThat(result.getStatus().getCode(), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(result.getSeries(), hasSize(0));
    }

    @Test
    public void alertEvaluationHistoryNodataDescriptionIsVerbose() {
        String code = new StringBuilder()
            .append("let alarms = alert_evaluation_history();")
            .toString();
        ExpressionAlert parent = newExpressionAlert()
                .setPeriod(Duration.ofMinutes(3))
                .setProgram(code)
                .setCheckExpression("count(alarms) > 0")
                .build();
        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        EvaluationStatus result = syncCheckResult(parent, now);
        assertThat(result.getDescription(), equalTo("No metrics found by selectors: {project=='solomon', " +
                "cluster=='testing', service=='alerting_statuses', sensor=='alert.evaluation.status', " +
                "projectId=='test', alertId=='alertEvaluationHistoryNodataDescriptionIsVerbose', parentId='-'}"));
    }

    @Test
    public void alertEvaluationHistoryWithSubalerts() {
        ExpressionAlert parent = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setPeriod(Duration.ofMinutes(3))
                .setProgram("let hist = alert_evaluation_history();\n")
                .setCheckExpression("avg(hist) > 3")
                .setGroupByLabel("host")
                .build();
        SubAlert sub1 = SubAlert.newBuilder().setParent(parent).setGroupKey(Labels.of("host", "solomon-1")).build();
        SubAlert sub2 = SubAlert.newBuilder().setParent(parent).setGroupKey(Labels.of("host", "solomon-2")).build();
        SubAlert sub3 = SubAlert.newBuilder().setParent(parent).setGroupKey(Labels.of("host", "solomon-3")).build();
        solomon.addMetric(Labels.of(Map.of(
                "project", "solomon", "cluster", "testing", "service", "alerting_statuses", "sensor", "alert.evaluation.status",
                "alertId", sub1.getId(), "projectId", parent.getProjectId(), "parentId", parent.getId())
                ), AggrGraphDataArrayList.of(
                point("2017-09-08T09:12:00Z", 1),
                point("2017-09-08T09:13:00Z", 4),
                point("2017-09-08T09:14:00Z", 1))
        );
        solomon.addMetric(Labels.of(Map.of(
                "project", "solomon", "cluster", "testing", "service", "alerting_statuses", "sensor", "alert.evaluation.status",
                "alertId", sub2.getId(), "projectId", parent.getProjectId(), "parentId", parent.getId())
                ), AggrGraphDataArrayList.of(
                point("2017-09-08T09:12:00Z", 4),
                point("2017-09-08T09:13:00Z", 4),
                point("2017-09-08T09:14:00Z", 4))
        );
        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        assertThat(syncCheck(sub1, now), equalTo(EvaluationStatus.Code.OK));
        assertThat(syncCheck(sub2, now), equalTo(EvaluationStatus.Code.ALARM));
        assertThat(syncCheck(sub3, now), equalTo(EvaluationStatus.Code.NO_DATA));
    }

    @Test
    public void overrideOnlyNotExactMatch() {
        Labels labels = Labels.of("project", "solomon","cluster", "local","service", "test");
        String labelsStr = LabelsFormat.format(labels);
        ExpressionAlert parent = AlertTestSupport.randomExpressionAlert(ThreadLocalRandom.current())
            .toBuilder()
            .setPeriod(Duration.ofMinutes(3))
            .setProgram(new StringBuilder()
                .append("let usage = {" + labelsStr + ", sensor='memory.usage', shardId='*', module='*'};\n")
                .append("let limit = {" + labelsStr + ", sensor=='memory.limit', shardId=='total', module='*'};\n")
                .append("let used = usage / limit;\n")
                .toString())
            .setCheckExpression("last(used) > 0.8")
            .setGroupByLabels(List.of("module", "shardId"))
            .build();

        // memory.usage{shardId=1, module=cache}
        solomon.addMetric(labels.addAll(Labels.of("sensor", "memory.usage","shardId", "1", "module", "cache")), AggrGraphDataArrayList.of(
            point("2017-09-08T09:12:00Z", 10), // window start
            point("2017-09-08T09:14:00Z", 11),
            point("2017-09-08T09:15:00Z", 12) // now
        ));
        // memory.usage{shardId=1, module=writeQueue}
        solomon.addMetric(labels.addAll(Labels.of("sensor", "memory.usage","shardId", "1", "module", "writeQueue")), AggrGraphDataArrayList.of(
            point("2017-09-08T09:12:00Z", 1), // window start
            point("2017-09-08T09:14:00Z", 10),
            point("2017-09-08T09:15:00Z", 20) // now
        ));
        // memory.limit{shardId=total, module=cache}
        solomon.addMetric(labels.addAll(Labels.of("sensor", "memory.limit", "shardId", "total", "module", "cache")), AggrGraphDataArrayList.of(
            point("2017-09-08T09:12:00Z", 100), // window start
            point("2017-09-08T09:14:00Z", 100),
            point("2017-09-08T09:15:00Z", 100) // now
        ));
        // memory.limit{shardId=total, module=writeQueue}
        solomon.addMetric(labels.addAll(Labels.of("sensor", "memory.limit", "shardId", "total", "module", "writeQueue")), AggrGraphDataArrayList.of(
            point("2017-09-08T09:12:00Z", 10), // window start
            point("2017-09-08T09:14:00Z", 10),
            point("2017-09-08T09:15:00Z", 10) // now
        ));

        Instant now = Instant.parse("2017-09-08T09:15:00Z");
        assertThat(syncCheck(SubAlert.newBuilder()
            .setParent(parent)
            .setGroupKey(Labels.of("shardId", "1", "module", "cache"))
            .build(), now), equalTo(EvaluationStatus.Code.OK));

        assertThat(syncCheck(SubAlert.newBuilder()
            .setParent(parent)
            .setGroupKey(Labels.of("shardId", "1", "module", "writeQueue"))
            .build(), now), equalTo(EvaluationStatus.Code.ALARM));
    }

    @Test
    public void lazyAnnotations() {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-04");

        solomon.addMetric(labels,
            AggrGraphDataArrayList.of(
                lpoint("2017-09-08T09:10:00Z", 2), // window start
                lpoint("2017-09-08T09:10:15Z", 8),
                lpoint("2017-09-08T09:10:30Z", 5),
                lpoint("2017-09-08T09:10:45Z", 4), // now
                lpoint("2017-09-08T09:11:00Z", 20),
                lpoint("2017-09-08T09:11:15Z", 30)
            )
        );

        ExpressionAlert alert = newExpressionAlert()
            .setPeriod(Duration.ofMinutes(45))
            .setProgram("let source = {project='kikimr', cluster='foo', service='bar', sensor='idleTime', host='kikimr-04'};")
            .setCheckExpression("max(source) >= 9")
            .addAnnotation("details", "{{{expression.source}}}")
            .build();

        Instant now = Instant.parse("2017-09-08T09:10:45Z");
        var status = syncCheckResult(alert, now);
        assertEquals(
            "[Timeseries{timestamps=[2017-09-08T09:10:00.000Z, 2017-09-08T09:10:15.000Z, 2017-09-08T09:10:30.000Z], values=[2.0, 8.0, 5.0]}]",
            status.getAnnotations().get("details"));
    }

    @Test
    public void lazyConditionAnnotations() {
        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime",
            "host", "kikimr-04");

        solomon.addMetric(labels,
            AggrGraphDataArrayList.of(
                lpoint("2017-09-08T09:10:00Z", 2), // window start
                lpoint("2017-09-08T09:10:15Z", 8),
                lpoint("2017-09-08T09:10:30Z", 5),
                lpoint("2017-09-08T09:10:45Z", 4), // now
                lpoint("2017-09-08T09:11:00Z", 20),
                lpoint("2017-09-08T09:11:15Z", 30)
            )
        );

        ExpressionAlert alert = newExpressionAlert()
            .setPeriod(Duration.ofMinutes(45))
            .setProgram(new StringBuilder()
                .append("let source = {project='kikimr', cluster='foo', service='bar', sensor='idleTime', host='kikimr-04'};\n")
                .append("let a = min(source) <= 4;\n")
                .append("let b = min(source) >= 9;\n")
                .toString())
            .setCheckExpression("a || b")
            .addAnnotation("details", "{{#expression.a}} checkOne{{/expression.a}}{{#expression.b}} checkTwo{{/expression.b}}")
            .build();

        Instant now = Instant.parse("2017-09-08T09:10:45Z");
        var status = syncCheckResult(alert, now);
        assertEquals(" checkOne", status.getAnnotations().get("details"));
    }

    private EvaluationStatus.Code syncCheck(Alert alert, Instant now) {
        var status = syncCheckResult(alert, now);
        System.out.println(status + " at " + now);
        return status.getCode();
    }

    private EvaluationStatus syncCheckResult(Alert alert, Instant now) {
        AlertRule rule = ruleByAlert.computeIfAbsent(alert, alertRuleFactory::createAlertRule);
        CompletableFuture<EvaluationStatus> future = rule.eval(now, deadline());
        return future.join();
    }

    private ExplainResult syncExplainResult(Alert alert, Instant now) {
        AlertRule rule = ruleByAlert.computeIfAbsent(alert, alertRuleFactory::createAlertRule);
        CompletableFuture<ExplainResult> future = rule.explain(now, deadline());
        return future.join();
    }

    private AlertRuleDeadlines deadline() {
        return AlertRuleFairDeadlines.ignoreLag(Instant.now(), 30, TimeUnit.SECONDS);
    }

    private ExpressionAlert.Builder newExpressionAlert() {
        return ExpressionAlert.newBuilder()
                .setId(name.getMethodName())
                .setProjectId("test");
    }

    private AggrGraphDataArrayList generateMetric(String from, int length, int a, int b) {
        AggrPoint[] points = new AggrPoint[length];
        Instant start = Instant.parse(from);
        for (int i = 0; i < length; i++) {
            points[i] = point(start.plusSeconds(5L * i), a * i + b);
        }

        return AggrGraphDataArrayList.of(points);
    }
}
