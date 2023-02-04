package ru.yandex.solomon.alert.notification.channel.datalens;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.TargetStatus;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdType;
import ru.yandex.solomon.alert.graph.GraphLoaderImpl;
import ru.yandex.solomon.alert.graph.Line;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.cloud.NotifyClientStub;
import ru.yandex.solomon.alert.notification.domain.email.DatalensEmailNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.metrics.client.CrossDcMetricsClient;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.misc.concurrent.CompletableFutures.join;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class DatalensEmailNotificationChannelTest {
    private NotifyClientStub notifyClient;
    private SolomonClientStub solomon;
    private DcMetricsClient metricsClient;
    private GraphLoaderImpl graphLoader;
    private DatalensEmailNotification notification;
    private NotificationChannel notificationChannel;

    @Before
    public void setUp() {
        notifyClient = new NotifyClientStub();

        solomon = new SolomonClientStub();
        metricsClient = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        graphLoader = new GraphLoaderImpl(metricsClient);
        var factory = new DatalensEmailNotificationChannelFactory(notifyClient, graphLoader);
        notification = DatalensEmailNotification.newBuilder()
                .setRecipient("uranix@yandex-team.ru")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setId("notification-channel")
                .setName("Random name")
                .setProjectId("datalens-alerts")
                .build();
        notificationChannel = factory.createChannel(notification);
    }

    @After
    public void tearDown() {
        solomon.close();
    }

    @Test
    public void testMessageWithNoData() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert();
        EvaluationState state = EvaluationState.newBuilder()
                .setAlertKey(alert.getKey())
                .setLatestEval(Instant.EPOCH)
                .setAlertVersion(alert.getVersion())
                .setSince(Instant.EPOCH)
                .setStatus(EvaluationStatus.ALARM)
                .build();

        Event event = eval(alert, state);

        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertNotNull(payload.thresholdAlertGraph);
    }

    @Test
    public void testMessageWithTooManyLines() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
            .setSelectors("project=solomon, cluster=production, service=test, host=solomon-dev-*")
            .setDelaySeconds(60)
            .build();

        Instant now = Instant.now();

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(now)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);

        for (int i = 0; i < 200; i++) {
            Labels labels = Labels.of(
                "project", "solomon",
                "cluster", "production",
                "service", "test",
                "host", "solomon-dev-" + i);
            solomon.addMetric(labels, AggrGraphDataArrayList.empty());
        }

        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertThat(payload.thresholdAlertGraph, nullValue());
    }

    @Test
    public void testMessageWithEmptyLines() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
                .setSelectors("project=solomon, cluster=production, service=test, host=solomon-dev-00")
                .setDelaySeconds(60)
                .build();

        Instant now = Instant.now();

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(now)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);

        Labels labels0 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-00");
        Labels labels1 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-01");
        solomon.addMetric(labels0, AggrGraphDataArrayList.empty());
        solomon.addMetric(labels1, AggrGraphDataArrayList.empty());

        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertThat(payload.thresholdAlertGraph.comparison, equalTo(alert.getPredicateRules().get(0).getComparison().name()));
        assertThat(payload.thresholdAlertGraph.alarmLevel, equalTo(alert.getPredicateRules().get(0).getThreshold()));
        if (alert.getPredicateRules().size() > 1) {
            assertThat(payload.thresholdAlertGraph.warnLevel, equalTo(alert.getPredicateRules().get(1).getThreshold()));
        } else {
            assertThat(payload.thresholdAlertGraph.warnLevel, nullValue());
        }
        assertThat(payload.thresholdAlertGraph.windowMillis, equalTo(alert.getPeriod().toMillis()));
        assertThat(payload.thresholdAlertGraph.nowMillis, equalTo(now.minusSeconds(alert.getDelaySeconds()).toEpochMilli()));
        assertThat(payload.thresholdAlertGraph.lines, iterableWithSize(1));

        var line = payload.thresholdAlertGraph.lines.get(0);

        assertThat(line.alias, equalTo(""));
        assertThat(line.labels, equalTo(labels0.toMap()));
        assertThat(line.timestampsMillis.length, equalTo(0));
        assertThat(line.values.length, equalTo(0));
    }

    @Test
    public void emptyCrossDc() {
        var solomon = new SolomonClientStub();
        var metricsClientSas = new DcMetricsClient("sas", solomon.getMetabase(), solomon.getStockpile());
        var metricsClientVla = new DcMetricsClient("vla", solomon.getMetabase(), solomon.getStockpile());
        var metricsClient = new CrossDcMetricsClient(Map.of("sas", metricsClientSas, "vla", metricsClientVla));

        graphLoader = new GraphLoaderImpl(metricsClient);

        var factory = new DatalensEmailNotificationChannelFactory(notifyClient, graphLoader);
        notification = DatalensEmailNotification.newBuilder()
            .setRecipient("bfbldg1mouscbh8gcnmq")
            .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
            .setId("notification-channel")
            .setName("Random name")
            .setProjectId("aoecngvoh58bgtr3s25a")
            .setFolderId("aoe5h5pn3otb41inm3tl")
            .build();
        notificationChannel = factory.createChannel(notification);

        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
            .setSelectors("project=solomon, cluster=production, service=test, host=solomon-dev-00")
            .setDelaySeconds(60)
            .build();

        Instant now = Instant.now();

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(now)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);

        Labels labels0 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-00");
        Labels labels1 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-01");
        solomon.addMetric(labels0, AggrGraphDataArrayList.empty());
        solomon.addMetric(labels1, AggrGraphDataArrayList.empty());

        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertThat(payload.thresholdAlertGraph, notNullValue());
        assertThat(payload.thresholdAlertGraph.comparison, equalTo(alert.getPredicateRules().get(0).getComparison().name()));
        assertThat(payload.thresholdAlertGraph.alarmLevel, equalTo(alert.getPredicateRules().get(0).getThreshold()));
        if (alert.getPredicateRules().size() > 1) {
            assertThat(payload.thresholdAlertGraph.warnLevel, equalTo(alert.getPredicateRules().get(1).getThreshold()));
        } else {
            assertThat(payload.thresholdAlertGraph.warnLevel, nullValue());
        }
        assertThat(payload.thresholdAlertGraph.windowMillis, equalTo(alert.getPeriod().toMillis()));
        assertThat(payload.thresholdAlertGraph.nowMillis, equalTo(now.minusSeconds(alert.getDelaySeconds()).toEpochMilli()));
        assertThat(payload.thresholdAlertGraph.lines, iterableWithSize(1));

        var line = payload.thresholdAlertGraph.lines.get(0);

        assertThat(line.alias, equalTo(""));
        assertThat(line.labels, equalTo(labels0.toMap()));
        assertThat(line.timestampsMillis.length, equalTo(0));
        assertThat(line.values.length, equalTo(0));
    }

    @Test
    public void testMessageWithLinesCorrectlyCropped() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
            .setSelectors("project=solomon, cluster=production, service=test, host=solomon-dev-*")
            .setPeriod(Duration.ofMinutes(15))
            .setDelaySeconds(60)
            .build();

        Instant now = Instant.parse("2019-12-19T10:20:14Z").plusMillis(ThreadLocalRandom.current().nextInt(0, 5000));

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(now)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);

        Labels labels0 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-00");
        Labels labels1 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-01");

        int size = 5000;
        AggrGraphDataArrayList data0 = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, size);
        AggrGraphDataArrayList data1 = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, size);

        Instant begin = now.minus(Duration.ofDays(5)).truncatedTo(ChronoUnit.DAYS);

        for (int i = 0; i < size; i++) {
            data0.addRecordShort(begin.plusSeconds(15 * i).toEpochMilli(), Math.sin(0.01 * i));
            data1.addRecordShort(begin.plusSeconds(15 * i).toEpochMilli(), Math.cos(0.01 * i));
        }

        solomon.addMetric(labels0, data0);
        solomon.addMetric(labels1, data1);

        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertThat(payload.thresholdAlertGraph.lines, iterableWithSize(2));

        Line[] lines = payload.thresholdAlertGraph.lines.stream()
                .sorted(Comparator.comparing(x -> x.labels.get("host")))
                .toArray(Line[]::new);

        var line0 = lines[0];
        var line1 = lines[1];

        assertThat(line0.alias, equalTo(""));
        assertThat(line1.alias, equalTo(""));
        assertThat(line0.labels, equalTo(labels0.toMap()));
        assertThat(line1.labels, equalTo(labels1.toMap()));
        assertThat(line0.timestampsMillis.length, equalTo(line0.values.length));
        assertThat(line1.timestampsMillis.length, equalTo(line1.values.length));

        long tsTo = now.toEpochMilli();
        long tsFrom = tsTo - 3 * alert.getPeriod().toMillis();

        for (var line : List.of(line0, line1)) {
            for (long ts : line.timestampsMillis) {
                Assert.assertTrue(ts < tsTo);
                Assert.assertTrue(ts >= tsFrom);
            }
        }


        List<Long> ts0 = Arrays.stream(line0.timestampsMillis).boxed().collect(toList());
        List<Long> ts1 = Arrays.stream(line1.timestampsMillis).boxed().collect(toList());

        assertThat(ts0, everyItem(allOf(lessThan(tsTo), greaterThanOrEqualTo(tsFrom))));
        assertThat(ts1, everyItem(allOf(lessThan(tsTo), greaterThanOrEqualTo(tsFrom))));
    }

    @Test
    public void testAggregateOnCropped() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
            .setSelectors("project=solomon, cluster=production, service=test, host=solomon-dev-*")
            .setPeriod(Duration.ofMinutes(15))
            .setDelaySeconds(60)
            .setPredicateRule(PredicateRule.onThreshold(100)
                .withComparison(Compare.GT)
                .withThresholdType(ThresholdType.MIN)
                .withTargetStatus(TargetStatus.ALARM))
            .build();

        Instant now = Instant.parse("2019-12-19T10:20:14Z").plusMillis(ThreadLocalRandom.current().nextInt(0, 5000));

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(now)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);

        Labels labels0 = Labels.of(
            "project", "solomon",
            "cluster", "production",
            "service", "test",
            "name", "cpu_usage",
            "host", "solomon-dev-00");

        int size = 5000;
        AggrGraphDataArrayList data0 = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, size);

        Instant begin = now.minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

        var point = new AggrPoint();
        point.columnSet = data0.columnSetMask();
        point.tsMillis = begin.toEpochMilli();
        for (int i = 0; point.tsMillis < now.toEpochMilli(); i++) {
            point.tsMillis += 15_000;
            point.valueNum = i;
            data0.addRecord(point);
        }

        solomon.addMetric(labels0, data0);

        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertThat(payload.thresholdAlertGraph.lines, iterableWithSize(1));

        var line0 = payload.thresholdAlertGraph.lines.get(0);

        assertEquals("", line0.alias);
        assertEquals(labels0.toMap(), line0.labels);
        assertEquals(line0.values.length, line0.timestampsMillis.length);
        assertEquals(8176, line0.value, 5.0);
    }

    @Test
    public void noData() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
            .setSelectors("project=solomon, cluster=production, service=test, host=solomon-dev-*")
            .setPeriod(Duration.ofMinutes(15))
            .setDelaySeconds(60)
            .setPredicateRule(PredicateRule.onThreshold(100)
                .withComparison(Compare.GT)
                .withThresholdType(ThresholdType.MIN)
                .withTargetStatus(TargetStatus.ALARM))
            .build();

        Instant now = Instant.parse("2019-12-19T10:20:14Z").plusMillis(ThreadLocalRandom.current().nextInt(0, 5000));

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(now)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);
        join(notificationChannel.send(Instant.EPOCH, event));

        var payload = (DatalensEmailNotificationChannel.Payload) notifyClient.getOutboxEmail(0).data;
        assertThat(payload.thresholdAlertGraph.lines, iterableWithSize(0));
        assertNotEquals(0, payload.thresholdAlertGraph.range.fromMillis);
        assertNotEquals(0, payload.thresholdAlertGraph.range.toMillis);
    }
}
