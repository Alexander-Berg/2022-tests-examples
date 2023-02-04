package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;


/**
 * @author Vladimir Gordiychuk
 */
public class SelFnGroupByTimeTest {

    @Test
    public void groupByLast() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:00:00Z", 9),
            point("2017-04-21T00:00:05Z", 3),
            point("2017-04-21T00:00:10Z", 6),

            point("2017-04-21T00:00:16Z", 3),
            point("2017-04-21T00:00:17Z", 2),
            point("2017-04-21T00:00:18Z", 1)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'last', graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-04-21T00:00:00Z", 6), //9, 3, 6
            point("2017-04-21T00:00:15Z", 1)  //3, 2, 1
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByMin() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:00:00Z", 9),
            point("2017-04-21T00:00:05Z", 3),
            point("2017-04-21T00:00:10Z", 6),

            point("2017-04-21T00:00:16Z", 3),
            point("2017-04-21T00:00:17Z", 2),
            point("2017-04-21T00:00:18Z", 1)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'min', graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-04-21T00:00:00Z", 3), //9, 3, 6
            point("2017-04-21T00:00:15Z", 1) //3, 2, 1
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByMax() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:00:00Z", 9),
            point("2017-04-21T00:00:05Z", 3),
            point("2017-04-21T00:00:10Z", 6),

            point("2017-04-21T00:00:16Z", 3),
            point("2017-04-21T00:00:17Z", 2),
            point("2017-04-21T00:00:18Z", 1)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'max', graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-04-21T00:00:00Z", 9), //9, 3, 6
            point("2017-04-21T00:00:15Z", 3)  //3, 2, 1
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByTimeSmallerThanStep() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:01:00Z", 4),
            point("2017-04-21T00:03:00Z", 2),
            point("2017-04-21T00:05:00Z", 5)
        );

        SelValueVector vector = execExpr("group_by_time(1m, 'last', graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-04-21T00:01:00Z", 4),
            point("2017-04-21T00:02:00Z", Double.NaN),
            point("2017-04-21T00:03:00Z", 2),
            point("2017-04-21T00:04:00Z", Double.NaN),
            point("2017-04-21T00:05:00Z", 5)
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void excludeFirstNanPoint() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:00:01Z", 9),
            point("2017-04-21T00:00:02Z", 3),
            point("2017-04-21T00:00:03Z", 6),

            point("2017-04-21T00:00:16Z", 3),
            point("2017-04-21T00:00:17Z", 2),
            point("2017-04-21T00:00:18Z", 1)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'sum', graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-04-21T00:00:00Z", 9 + 3 + 6),
            point("2017-04-21T00:00:15Z", 3 + 2 + 1)
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void excludeLastNanPoint() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:00:00Z", 9),
            point("2017-04-21T00:00:01Z", 3),
            point("2017-04-21T00:00:02Z", 6),

            point("2017-04-21T00:00:16Z", 3),
            point("2017-04-21T00:00:17Z", 2),
            point("2017-04-21T00:00:29Z", 1)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'sum', graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-04-21T00:00:00Z", 9 + 3 + 6),
            point("2017-04-21T00:00:15Z", 3 + 2 + 1)
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByWhenExistOnlySinglePoint() throws Exception {
        GraphData source = GraphData.of(point("2017-04-21T00:00:42Z", 42));
        GraphData expected = GraphData.of(point("2017-04-21T00:00:00Z", 42));

        SelValueVector vector = execExpr("group_by_time(1m, 'last', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByWhenExistOnlySinglePointResultWithSameTime() throws Exception {
        GraphData source = GraphData.of(point("2017-04-21T00:00:00Z", 42));
        GraphData expected = GraphData.of(point("2017-04-21T00:00:00Z", 42));

        SelValueVector vector = execExpr("group_by_time(1m, 'last', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByAlignMultipleLinesToCommonTimeline() throws Exception {
        GraphData lineOne = GraphData.of(
            point("2017-04-21T00:00:05Z", 4),
            point("2017-04-21T00:00:30Z", 2),
            point("2017-04-21T00:00:38Z", 5),
            point("2017-04-21T00:00:55Z", 3)
        );

        GraphData lineTwo = GraphData.of(
            point("2017-04-21T00:00:00Z", 8),
            point("2017-04-21T00:00:10Z", 6),
            point("2017-04-21T00:00:14Z", 2),
            point("2017-04-21T00:00:25Z", 5)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'last', graphData);", lineOne, lineTwo);

        GraphData expectedOne = GraphData.of(
            point("2017-04-21T00:00:00Z", 4),
            point("2017-04-21T00:00:15Z", Double.NaN),
            point("2017-04-21T00:00:30Z", 5),
            point("2017-04-21T00:00:45Z", 3)
        );

        GraphData expectedTwo = GraphData.of(
            point("2017-04-21T00:00:00Z", 2),
            point("2017-04-21T00:00:15Z", 5),
            point("2017-04-21T00:00:30Z", Double.NaN)
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expectedOne));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(expectedTwo));
    }

    @Ignore
    public void intervalLessThan15SecondNotAvailable() throws Exception {
        GraphData source = GraphData.of(point("2017-04-21T00:00:42Z", 42));
        SelValueVector vector = execExpr("group_by_time(5s, 'last', graphData);", source);
    }

    @Test(expected = EvaluationException.class)
    public void groupBySmallIntervalOnHugeRangeNotAvailable() throws Exception {
        GraphData source = GraphData.of(
            point("2010-04-21T00:00:00Z", 42),
            point("2011-04-21T00:00:00Z", 43),
            point("2012-04-21T00:00:00Z", 44),
            point("2013-04-21T00:00:00Z", 44),
            point("2014-04-21T00:00:00Z", 45),
            point("2015-04-21T00:00:00Z", 46),
            point("2016-04-21T00:00:00Z", 47),
            point("2017-04-21T00:00:00Z", 48)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'last', graphData);", source);
        Assert.fail("Current test should fail because on 7 year interval with 15s step will be allocate ~100mb memory " +
            "only for timestamp, and function should restrict eat to avoid OOM"
        );
    }

    @Test
    public void groupByLastWhenOneOfLineIsEmpty() throws Exception {
        GraphData lineTwo = GraphData.of(
            point("2017-04-21T00:00:05Z", 4),
            point("2017-04-21T00:00:30Z", 2),
            point("2017-04-21T00:00:38Z", 5),
            point("2017-04-21T00:00:55Z", 3)
        );

        SelValueVector vector = execExpr("group_by_time(15s, 'last', graphData);",
            GraphData.empty, lineTwo, GraphData.empty
        );

        GraphData expectedTwo = GraphData.of(
            point("2017-04-21T00:00:00Z", 4),
            point("2017-04-21T00:00:15Z", Double.NaN),
            point("2017-04-21T00:00:30Z", 5),
            point("2017-04-21T00:00:45Z", 3)
        );

        GraphData expectedOne = GraphData.empty;

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(expectedOne));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(expectedTwo));
    }

    @Test
    public void groupTheSameAsSourceWhenAllLinesEmpty() throws Exception {
        SelValueVector vector = execExpr("group_by_time(15s, 'last', graphData);",
            GraphData.empty, GraphData.empty
        );

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(GraphData.empty));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void groupByTimePerSingleLine() throws Exception {
        GraphData source = GraphData.of(
            point("2017-04-21T00:00:05Z", 4),
            point("2017-04-21T00:00:30Z", 2),
            point("2017-04-21T00:00:38Z", 5),
            point("2017-04-21T00:00:55Z", 3)
        );

        GraphData result = execExprOnSingleLine("group_by_time(15s, 'last', graphData);", source);


        GraphData expected = GraphData.of(
            point("2017-04-21T00:00:00Z", 4),
            point("2017-04-21T00:00:15Z", Double.NaN),
            point("2017-04-21T00:00:30Z", 5),
            point("2017-04-21T00:00:45Z", 3)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void groupByTimeSaveLabels() throws Exception {
        NamedGraphData lineOne = NamedGraphData.of(
                Labels.of("sensor", "errorCount", "host", "test-1"),
                point("2017-04-21T00:00:05Z", 4),
                point("2017-04-21T00:00:30Z", 2),
                point("2017-04-21T00:00:38Z", 5),
                point("2017-04-21T00:00:55Z", 3)
        );

        NamedGraphData lineTwo = NamedGraphData.of(
                Labels.of("sensor", "errorCount", "host", "test-2"),
                point("2017-04-21T00:00:00Z", 8),
                point("2017-04-21T00:00:10Z", 6),
                point("2017-04-21T00:00:14Z", 2),
                point("2017-04-21T00:00:25Z", 5)
        );

        SelValueVector vector = ProgramTestSupport.expression("group_by_time(15s, 'last', graphData);")
                .onMultipleLines(lineOne, lineTwo)
                .exec()
                .getAsVector();

        Assert.assertThat(vector.item(0).castToGraphData().getNamedGraphData().getLabels(), CoreMatchers.equalTo(lineOne.getLabels()));
        Assert.assertThat(vector.item(1).castToGraphData().getNamedGraphData().getLabels(), CoreMatchers.equalTo(lineTwo.getLabels()));
    }

    @Test
    public void groupWithWrongKind() {
        GraphData graphData = GraphData.of(
            point("2019-10-31T19:00:00Z", 4),
            point("2019-10-31T19:00:05Z", 5),
            point("2019-10-31T19:00:10Z", 8),
            point("2019-10-31T19:00:15Z", 7),
            point("2019-10-31T19:00:20Z", 6)
        );

        NamedGraphData source = NamedGraphData.newBuilder()
            .setGraphData(graphData)
            .setType(MetricType.IGAUGE)
            .build();

        NamedGraphData result = ProgramTestSupport.expression("group_by_time(15s, 'last', graphData);")
            .onSingleLine(source)
            .exec()
            .getAsNamedSingleLine();

        GraphData expected = GraphData.of(
            point("2019-10-31T19:00:00Z", 8), // 4, 5, 8
            point("2019-10-31T19:00:15Z", 6) // 7, 6
        );

        Assert.assertThat(result.getGraphData(), CoreMatchers.equalTo(expected));
        Assert.assertThat(result.getType(), CoreMatchers.equalTo(MetricType.IGAUGE));
        // TODO: DGAUGE->IGAUGE
        Assert.assertThat(result.getDataType(), CoreMatchers.equalTo(ru.yandex.solomon.model.protobuf.MetricType.DGAUGE));
    }

    private GraphData execExprOnSingleLine(String expr, GraphData source) {
        return ProgramTestSupport.expression(expr)
            .onSingleLine(source)
            .exec()
            .getAsSingleLine();
    }

    private SelValueVector execExpr(String expression, GraphData... source) {
        return ProgramTestSupport.expression(expression)
            .onMultipleLines(source)
            .exec()
            .getAsVector();
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
