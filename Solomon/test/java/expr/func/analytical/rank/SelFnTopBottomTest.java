package ru.yandex.solomon.expression.expr.func.analytical.rank;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.math.protobuf.OperationTop;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.Timeline;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnTopBottomTest {

    @Test
    public void topRestrictedByLineNumber() throws Exception {
        GraphData[] source = new GraphData[]{
            GraphData.of(point("2017-03-01T00:00:00Z", 1)),
            GraphData.of(point("2017-03-02T00:00:00Z", 2)),
            GraphData.of(point("2017-03-03T00:00:00Z", 3)),
            GraphData.of(point("2017-03-04T00:00:00Z", 4)),
            GraphData.of(point("2017-03-05T00:00:00Z", 5))
        };

        SelValueVector vector = execExpr("top(2, 'avg', graphData);", source);
        Assert.assertThat(vector.length(), CoreMatchers.equalTo(2));
    }

    @Test
    public void returnLinesAsIsWhenNotEnoughLines() throws Exception {
        GraphData[] source = new GraphData[]{
            GraphData.of(point("2017-03-01T00:00:00Z", 1)),
            GraphData.of(point("2017-03-02T00:00:00Z", 2)),
            GraphData.of(point("2017-03-03T00:00:00Z", 3))
        };

        SelValueVector vector = execExpr("top(5, 'avg', graphData);", source);
        Assert.assertThat(vector.length(), CoreMatchers.equalTo(3));
    }


    @Test
    public void returnLinesAsIsWhenNotEnoughLinesAlias() throws Exception {
        GraphData[] source = new GraphData[]{
                GraphData.of(point("2017-03-01T00:00:00Z", 1)),
                GraphData.of(point("2017-03-02T00:00:00Z", 2)),
                GraphData.of(point("2017-03-03T00:00:00Z", 3))
        };

        SelValueVector vector = execExpr("top_avg(5, graphData);", source);
        Assert.assertThat(vector.length(), CoreMatchers.equalTo(3));
    }

    @Test
    public void top3Avg() {
        String time1 = "2017-03-01T00:00:00Z";
        String time2 = "2017-03-02T00:00:00Z";
        String time3 = "2017-03-03T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, 10), point(time2, -10), point(time3, 100)),
            GraphData.of(point(time1, 30), point(time2, 50), point(time3, 40)),
            GraphData.of(point(time1, 99), point(time2, 99), point(time3, 99)),
            GraphData.of(point(time1, 2), point(time2, Double.NaN), point(time3, 8)),
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)),
            GraphData.of(point(time1, -12), point(time2, -99), point(time3, -100)),
            GraphData.of(point(time1, 50), point(time2, 60), point(time3, 70)),
        };

        SelValueVector vector = execExpr("top(3, 'avg', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[2]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[6]));
        Assert.assertThat(vector.item(2).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[1]));
    }

    @Test
    public void top2Max() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)),
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),
            GraphData.of(point(time1, 2), point(time2, 60), point(time3, 3)),
        };

        SelValueVector vector = execExpr("top(2, 'max', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[5]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[0]));
    }

    @Test
    public void bottom2Max() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),   //40.55
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),         //15
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),         //2 MIN
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)), //NaN
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),     //-10 MIN
            GraphData.of(point(time1, 2), point(time2, 60), point(time3, 3)),          //60
        };

        SelValueVector vector = execExpr("bottom(2, 'max', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[4]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[2]));
    }

    @Test
    public void top2Min() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),   //12 MAX
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),         //0
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),         //-99
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)), //NaN
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),     //-40
            GraphData.of(point(time1, 4), point(time2, 60), point(time3, 3)),          //3  MAX
        };

        SelValueVector vector = execExpr("top(2, 'min', graphData);", source);
        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[0]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[5]));
    }

    @Test
    public void bottom2Min() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),   //12
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),         //0
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),         //-99 MIN
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)), //NaN
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),     //-40 MIN
            GraphData.of(point(time1, 4), point(time2, 60), point(time3, 3)),          //3
        };

        SelValueVector vector = execExpr("bottom(2, 'min', graphData);", source);
        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[2]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[4]));
    }

    @Test
    public void bottom2MinAlias() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
                GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),   //12
                GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),         //0
                GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),         //-99 MIN
                GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)), //NaN
                GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),     //-40 MIN
                GraphData.of(point(time1, 4), point(time2, 60), point(time3, 3)),          //3
        };

        SelValueVector vector = execExpr("bottom_min(2, graphData);", source);
        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[2]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[4]));
    }

    @Test
    public void top2Sum() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 1), point(time3, 2)),         //3
            GraphData.of(point(time1, -10), point(time2, 10), point(time3, 2)),         //2
            GraphData.of(point(time1, -10), point(time2, -10), point(time3, -10)),      //-30
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)),  //NaN
            GraphData.of(point(time1, 5), point(time2, 5), point(time3, 5)),            //15 MAX
            GraphData.of(point(time1, 1), point(time2, 5), point(time3, 4)),            //10  MAX
        };

        SelValueVector vector = execExpr("top(2, 'sum', graphData);", source);
        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[4]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[5]));
    }

    @Test
    public void top2Last() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)),
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),
            GraphData.of(point(time1, 4), point(time2, 60), point(time3, 10)),
        };

        SelValueVector vector = execExpr("top(2, 'last', graphData);", source);
        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[1]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[0]));
    }

    @Test
    public void top2Std() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),   //20
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),         //7
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),         //58 MAX
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)), //NaN
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),     //16
            GraphData.of(point(time1, 4), point(time2, 60), point(time3, 10)),         //30 MAX
        };

        SelValueVector vector = execExpr("top(2, 'std', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[2]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[5]));
    }

    @Test
    public void top1Count() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-12T00:00:00Z";
        String time3 = "2017-03-13T00:00:00Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, 40.5)),
            GraphData.of(point(time1, Double.NaN)),
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN)),
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),
            GraphData.of(point(time1, 4), point(time2, 60)),
        };

        SelValueVector vector = execExpr("top(1, 'count', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[3]));
    }

    @Test
    public void top2integrate() throws Exception {
        String time1 = "2017-03-11T00:00:00Z";
        String time2 = "2017-03-11T00:00:15Z";
        String time3 = "2017-03-11T00:00:30Z";

        GraphData[] source = new GraphData[]{
            GraphData.of(point(time1, Double.NaN), point(time2, 40.55), point(time3, 12)),   //698.25 MAX
            GraphData.of(point(time1, 10), point(time2, 0), point(time3, 15)),         //187.5
            GraphData.of(point(time1, -99), point(time2, 2), point(time3, 2)),         //-697.5
            GraphData.of(point(time1, Double.NaN), point(time2, Double.NaN), point(time3, Double.NaN)), //0
            GraphData.of(point(time1, -12), point(time2, -40), point(time3, -10)),     //-765.0
            GraphData.of(point(time1, 4), point(time2, 60), point(time3, 10)),         //1005.0 MAX
        };

        SelValueVector vector = execExpr("top(2, 'integrate', graphData);", source);

        Assert.assertThat(vector.item(0).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[5]));
        Assert.assertThat(vector.item(1).castToGraphData().getGraphData(), CoreMatchers.equalTo(source[0]));
    }

    @Test
    public void topPushedToStockpile() {
        var pp = Program.fromSourceWithReturn("top(2, 'max', {sensor=testData})", false)
                .compile()
                .prepare(Interval.millis(1000, 2000));
        var loadRequest = pp.getLoadRequests().iterator().next();
        var expected = OperationTop.newBuilder()
                .setAsc(false)
                .setLimit(2)
                .setTimeAggregation(Aggregation.MAX)
                .build();
        Assert.assertEquals(expected, loadRequest.getRankFilter());
    }

    @Test
    public void topNotPushedToStockpile() {
        var pp = Program.fromSourceWithReturn("top(2, 'max', derivative({sensor=testData}))", false)
                .compile()
                .prepare(Interval.millis(1000, 2000));
        var loadRequest = pp.getLoadRequests().iterator().next();
        Assert.assertNull(loadRequest.getRankFilter());
    }

    @Test
    public void topNotPushedToStockpile2() {
        var pp = Program.fromSourceWithReturn("bottom(2, 'integrate', {sensor=testData})", false)
                .compile()
                .prepare(Interval.millis(1000, 2000));
        var loadRequest = pp.getLoadRequests().iterator().next();
        Assert.assertNull(loadRequest.getRankFilter());
    }

    @Test
    public void bottomPushedToStockpile() {
        var pp = Program.fromSourceWithReturn("bottom(42, 'avg', {sensor=testData})", false)
                .compile()
                .prepare(Interval.millis(1000, 2000));
        var loadRequest = pp.getLoadRequests().iterator().next();
        var expected = OperationTop.newBuilder()
                .setAsc(true)
                .setLimit(42)
                .setTimeAggregation(Aggregation.AVG)
                .build();
        Assert.assertEquals(expected, loadRequest.getRankFilter());
    }

    private SelValueVector execExpr(String expression, GraphData... source) {
        Interval interval = Arrays.stream(source)
            .map(GraphData::getTimeline)
            .map(Timeline::interval)
            .reduce(Interval::convexHull)
            .orElseGet(() -> Interval.before(Instant.now(), Duration.ofDays(1)));

        return ProgramTestSupport.expression(expression)
            .onMultipleLines(source)
            .forTimeInterval(interval).exec()
            .getAsVector();
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
