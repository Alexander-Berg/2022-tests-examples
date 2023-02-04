package ru.yandex.solomon.expression.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Vladimir Gordiychuk
 */
public class NamedGraphDataTest {
    private GraphDataLoaderStub dataLoaderStub;

    @Before
    public void setUp() throws Exception {
        dataLoaderStub = new GraphDataLoaderStub();
    }

    @Test
    public void loadSingleNamedGraphData() {
        GraphData source = GraphData.of(
            point("2017-04-04T14:00:00Z", 10),
            point("2017-04-04T15:00:00Z", 13),
            point("2017-04-04T16:00:00Z", 20)
        );

        NamedGraphData waitNamedGraphData = NamedGraphData.newBuilder()
            .setGraphData(source)
            .setLabels(Labels.of("cluster", "man", "service", "sys", "host", "solomon-man-01"))
            .setAlias("solomon-man-01")
            .build();

        dataLoaderStub.putSelectorValue("cluster='man', service='sys', host='solomon-man-01'", waitNamedGraphData);

        Interval interval = source.getTimeline().interval();
        NamedGraphData result =
            execExpr("{cluster=man, service=sys, host='solomon-man-01'}", interval)
                .castToGraphData()
                .getNamedGraphData();

        Assert.assertThat(result, CoreMatchers.equalTo(waitNamedGraphData));
    }

    @Test
    public void loadMultipleNamedGraphData() {
        NamedGraphData first =
            NamedGraphData.of(
                "solomon-man-01",
                Labels.of("cluster", "man", "host", "solomon-man-01"),
                point("2017-04-04T14:00:00Z", 10)
            );
        NamedGraphData second =
            NamedGraphData.of(
                "solomon-man-02",
                Labels.of("cluster", "man", "host", "solomon-man-02"),
                point("2017-04-04T14:00:00Z", 10)
            );

        dataLoaderStub.putSelectorValue("cluster='man', host='*'", first, second);
        Interval interval = Interval.after(Instant.parse("2017-04-04T14:00:00Z"), Duration.ofDays(30));

        SelValueVector vector = execExpr("{cluster='man', host='*'}", interval).castToVector();

        Assert.assertThat(vector.item(0).castToGraphData().getNamedGraphData(), CoreMatchers.equalTo(first));
        Assert.assertThat(vector.item(1).castToGraphData().getNamedGraphData(), CoreMatchers.equalTo(second));
    }

    @Test
    public void movingAverageSameGraphIdentity() {
        GraphData graphData = GraphData.of(
            point("2017-04-04T14:00:00Z", 10),
            point("2017-04-04T14:01:00Z", 10)
        );

        Labels labels = Labels.of("cluster", "sas", "host", "solomon-man-01");
        String alias = "solomon-man-01";

        NamedGraphData namedGraphData = NamedGraphData.newBuilder()
            .setGraphData(graphData)
            .setLabels(labels)
            .setAlias(alias)
            .build();

        dataLoaderStub.putSelectorValue("cluster='sas', host='solomon-man-01'", namedGraphData);
        Interval interval = Interval.after(Instant.parse("2017-04-04T14:00:00Z"), Duration.ofDays(30));

        NamedGraphData result = execExpr("moving_avg({cluster=sas, host='solomon-man-01'}, 1h)", interval)
            .castToGraphData()
            .getNamedGraphData();

        Assert.assertThat(result, CoreMatchers.equalTo(namedGraphData));
    }

    @Test
    public void movingPercentileSameGraphIdentity() {
        GraphData graphData = GraphData.of(
            point("2017-04-04T14:00:00Z", 10),
            point("2017-04-04T14:01:00Z", 10)
        );

        Labels labels = Labels.of("cluster", "sas", "host", "solomon-man-02");
        String alias = "solomon-man-02";

        NamedGraphData namedGraphData = NamedGraphData.newBuilder()
            .setGraphData(graphData)
            .setLabels(labels)
            .setAlias(alias)
            .build();

        dataLoaderStub.putSelectorValue("cluster=sas, host='solomon-man-02'", namedGraphData);
        Interval interval = Interval.after(Instant.parse("2017-04-04T14:00:00Z"), Duration.ofDays(30));

        NamedGraphData result = execExpr("moving_percentile({cluster=sas, host='solomon-man-02'}, 1h, 100)", interval)
            .castToGraphData()
            .getNamedGraphData();

        Assert.assertThat(result, CoreMatchers.equalTo(namedGraphData));
    }

    @Test
    public void loadComplexGraphData() {
        Labels labels = Labels.of("host", "solomon-sas-01", "cluster", "prestable");

        AggrGraphDataArrayList graphData = AggrGraphDataArrayList.of(
            AggrPoints.point(
                "2018-08-17T20:00:00Z",
                Histogram.newInstance(new double[]{10, 20, 30}, new long[]{50, 10, 1})
            ),
            AggrPoints.point(
                "2018-08-17T20:05:00Z",
                Histogram.newInstance(new double[]{10, 20, 30}, new long[]{50, 10, 1})
            ),
            AggrPoints.point(
                "2018-08-17T20:10:00Z",
                Histogram.newInstance(new double[]{10, 20, 30}, new long[]{50, 10, 1})
            )
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            AggrPoints.point(
                "2018-08-17T20:00:00Z",
                Histogram.newInstance(new double[]{10, 20, 30}, new long[]{50, 10, 1})
            ),
            AggrPoints.point(
                "2018-08-17T20:05:00Z",
                Histogram.newInstance(new double[]{10, 20, 30}, new long[]{50, 10, 1})
            )
        );

        NamedGraphData namedGraphData = NamedGraphData.newBuilder()
                .setType(MetricType.HIST)
                .setAlias("alias")
                .setLabels(labels)
                .setGraphData(graphData)
                .build();

        dataLoaderStub.putSelectorValue("cluster='prestable', host='solomon-sas-01'", namedGraphData);

        Interval interval = Interval.after(Instant.parse("2018-08-17T20:00:00Z"), Duration.ofMinutes(5));

        NamedGraphData result = execExpr("{cluster=prestable, host='solomon-sas-01'}", interval)
            .castToGraphData()
            .getNamedGraphData();

        Assert.assertThat(result.getAlias(), CoreMatchers.equalTo("alias"));
        Assert.assertThat(result.getType(), CoreMatchers.equalTo(MetricType.HIST));
        Assert.assertThat(result.getDataType(), CoreMatchers.equalTo(ru.yandex.solomon.model.protobuf.MetricType.HIST));
        Assert.assertThat(result.getLabels(), CoreMatchers.equalTo(labels));
        Assert.assertThat(result.getAggrGraphDataArrayList(), CoreMatchers.equalTo(expected));
    }

    private SelValue execExpr(String expression, Interval interval) {
        Program p = Program.fromSource("").withExternalExpression(expression).compile();
        PreparedProgram preparedProgram = p.prepare(interval);

        Map<String, SelValue> evaluationResult = preparedProgram.evaluate(dataLoaderStub, Collections.emptyMap());
        return evaluationResult.get(preparedProgram.expressionToVarName(expression));
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
