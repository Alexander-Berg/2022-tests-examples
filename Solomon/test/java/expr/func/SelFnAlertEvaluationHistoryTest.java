package ru.yandex.solomon.expression.expr.func;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.expr.IntrinsicsExternalizer;
import ru.yandex.solomon.expression.expr.func.util.SelFnAlertEvaluationHistory;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.labels.shard.ShardKey;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertEquals;

public class SelFnAlertEvaluationHistoryTest {

    @Test
    public void inlineSelectorsTest() {
        Program p = Program.fromSource("let statuses = alert_evaluation_history();").compile();
        assertEquals(0, p.getProgramSelectors().size());

        GraphDataLoaderStub dataLoaderStub = new GraphDataLoaderStub();
        dataLoaderStub.putSelectorValue("project==\"solomon\",cluster==\"testing\",service==\"alerting_statuses\"," +
                        "sensor==\"alert.evaluation.status\",projectId==\"bar\",alertId==\"foo\",parentId=\"-\"",
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d)));

        IntrinsicsExternalizer externalizer = IntrinsicsExternalizer.newBuilder(Interval.after(Instant.ofEpochMilli(500), Duration.ofSeconds(1)))
                .setAlertingStatuses(new ShardKey("solomon", "testing", "alerting_statuses"))
                .setAlertKey("foo", "bar", "")
                .build();

        Map<String, SelValue> result = p.prepare(externalizer)
                .evaluate(dataLoaderStub, Map.of());
        assertEquals(GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d)), result.get("statuses").castToGraphData().getGraphData());
    }

    @Test
    public void noMetadata() {
        Program p = Program.fromSource("let statuses = alert_evaluation_history();")
                .compile();
        Map<String, SelValue> result = p.prepare(Interval.before(Instant.now(), Duration.ofHours(1)))
                .evaluate(new GraphDataLoaderStub(), Map.of());
        assertEquals(GraphData.empty, result.get("statuses").castToGraphData().getGraphData());
    }

    @Test
    public void onlyLowerBits() {
        Program p = Program.fromSource("let statuses = alert_evaluation_history();").compile();
        assertEquals(0, p.getProgramSelectors().size());

        GraphDataLoaderStub dataLoaderStub = new GraphDataLoaderStub();
        dataLoaderStub.putSelectorValue("project==\"solomon\",cluster==\"testing\",service==\"alerting_statuses\"," +
                        "sensor==\"alert.evaluation.status\",projectId==\"bar\",alertId==\"foo\",parentId=\"-\"",
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d + 123 * SelFnAlertEvaluationHistory.ALERT_STATUS_DIVISOR)));

        IntrinsicsExternalizer externalizer = IntrinsicsExternalizer.newBuilder(Interval.after(Instant.ofEpochMilli(500), Duration.ofSeconds(1)))
                .setAlertingStatuses(new ShardKey("solomon", "testing", "alerting_statuses"))
                .setAlertKey("foo", "bar", "")
                .build();

        Map<String, SelValue> result = p.prepare(externalizer)
                .evaluate(dataLoaderStub, Map.of());
        assertEquals(GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d)), result.get("statuses").castToGraphData().getGraphData());
    }

    @Test
    public void otherAlert() {
        Program p =
                Program.fromSource("let statuses = alert_evaluation_history('solomon_cloud', 'diff-dc');").compile();
        assertEquals(0, p.getProgramSelectors().size());

        GraphDataLoaderStub dataLoaderStub = new GraphDataLoaderStub();
        dataLoaderStub.putSelectorValue("project==\"solomon\",cluster==\"testing\",service==\"alerting_statuses\"," +
                        "sensor==\"alert.evaluation.status\",projectId=\"solomon_cloud\",alertId=\"diff-dc\"",
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000),
                        42d + 123 * SelFnAlertEvaluationHistory.ALERT_STATUS_DIVISOR)));

        IntrinsicsExternalizer externalizer =
                IntrinsicsExternalizer.newBuilder(Interval.after(Instant.ofEpochMilli(500), Duration.ofSeconds(1)))
                .setAlertingStatuses(new ShardKey("solomon", "testing", "alerting_statuses"))
                .setAlertKey("foo", "bar", "")
                .build();

        Map<String, SelValue> result = p.prepare(externalizer)
                .evaluate(dataLoaderStub, Map.of());

        var statuses = result.get("statuses").castToVector();

        assertEquals(1, statuses.length());
        assertEquals(GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d)),
                statuses.item(0).castToGraphData().getGraphData());
    }

    @Test
    public void globsAreAccepted() {
        Program p = Program.fromSource("let statuses = alert_evaluation_history('solomon_cloud', 'fetcher-diff-dc|coremon-diff-dc');").compile();
        assertEquals(0, p.getProgramSelectors().size());

        GraphDataLoaderStub dataLoaderStub = new GraphDataLoaderStub();
        dataLoaderStub.putSelectorValue("project==\"solomon\",cluster==\"testing\",service==\"alerting_statuses\"," +
                        "sensor==\"alert.evaluation.status\",projectId=\"solomon_cloud\",alertId=\"fetcher-diff-dc|coremon-diff-dc\"",
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d + 123 * SelFnAlertEvaluationHistory.ALERT_STATUS_DIVISOR)),
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 12d + 31 * SelFnAlertEvaluationHistory.ALERT_STATUS_DIVISOR))
        );

        IntrinsicsExternalizer externalizer = IntrinsicsExternalizer.newBuilder(Interval.after(Instant.ofEpochMilli(500), Duration.ofSeconds(1)))
                .setAlertingStatuses(new ShardKey("solomon", "testing", "alerting_statuses"))
                .setAlertKey("foo", "bar", "")
                .build();

        Map<String, SelValue> result = p.prepare(externalizer)
                .evaluate(dataLoaderStub, Map.of());

        var statuses = result.get("statuses").castToVector();

        assertEquals(2, statuses.length());
        assertEquals(GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d)), statuses.item(0).castToGraphData().getGraphData());
        assertEquals(GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 12d)), statuses.item(1).castToGraphData().getGraphData());
    }

    @Test
    public void severalCalls() {
        Program p = Program.fromSource("""
            let first = alert_evaluation_history('solomon_cloud', 'fetcher-diff-dc');
            let second = alert_evaluation_history('solomon_cloud', 'coremon-diff-dc');
        """).compile();
        assertEquals(0, p.getProgramSelectors().size());

        GraphDataLoaderStub dataLoaderStub = new GraphDataLoaderStub();
        dataLoaderStub.putSelectorValue("project==\"solomon\",cluster==\"testing\",service==\"alerting_statuses\"," +
                        "sensor==\"alert.evaluation.status\",projectId=\"solomon_cloud\",alertId=\"fetcher-diff-dc\"",
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 42d + 123 * SelFnAlertEvaluationHistory.ALERT_STATUS_DIVISOR)));
        dataLoaderStub.putSelectorValue("project==\"solomon\",cluster==\"testing\",service==\"alerting_statuses\"," +
                        "sensor==\"alert.evaluation.status\",projectId=\"solomon_cloud\",alertId=\"coremon-diff-dc\"",
                GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), 12d + 31 * SelFnAlertEvaluationHistory.ALERT_STATUS_DIVISOR))
        );

        IntrinsicsExternalizer externalizer = IntrinsicsExternalizer.newBuilder(Interval.after(Instant.ofEpochMilli(500), Duration.ofSeconds(1)))
                .setAlertingStatuses(new ShardKey("solomon", "testing", "alerting_statuses"))
                .setAlertKey("foo", "bar", "")
                .build();

        Map<String, SelValue> result = p.prepare(externalizer)
                .evaluate(dataLoaderStub, Map.of());

        for (var var : List.of("first", "second")) {
            var value = result.get(var).castToVector();
            assertEquals(1, value.length());
            assertEquals(GraphData.of(new DataPoint(Instant.ofEpochMilli(1000), var.equals("first") ? 42d : 12d)), value.item(0).castToGraphData().getGraphData());
        }
    }
}
