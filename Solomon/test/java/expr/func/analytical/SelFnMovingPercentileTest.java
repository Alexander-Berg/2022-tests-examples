package ru.yandex.solomon.expression.expr.func.analytical;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;


/**
 * @author Vladimir Gordiychuk
 */
public class SelFnMovingPercentileTest extends WindowTestBase {

    @Test
    public void movingPercentile() {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1123),
            point("2017-03-01T01:00:00Z", 44),
            point("2017-03-01T02:00:00Z", 55),
            point("2017-03-01T03:00:00Z", 12),
            point("2017-03-01T04:00:00Z", 9),
            point("2017-03-01T05:00:00Z", 0),
            point("2017-03-01T06:00:00Z", -97),
            point("2017-03-01T07:00:00Z", -63),
            point("2017-03-01T08:00:00Z", 129),
            point("2017-03-01T09:00:00Z", 75),
            point("2017-03-01T10:00:00Z", 156),
            point("2017-03-01T11:00:00Z", -1234),
            point("2017-03-01T12:00:00Z", 55),
            point("2017-03-01T13:00:00Z", 1),
            point("2017-03-01T14:00:00Z", -37)
        );

        GraphData result = executeProgram(sourceGraphData, "moving_percentile(graphData, 6h, 70.5);");

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1123),
            point("2017-03-01T01:00:00Z", 1123),
            point("2017-03-01T02:00:00Z", 930.75),
            point("2017-03-01T03:00:00Z", 615.69),
            point("2017-03-01T04:00:00Z", 300.64),
            point("2017-03-01T05:00:00Z", 54.285),
            point("2017-03-01T06:00:00Z", 51.04),
            point("2017-03-01T07:00:00Z", 32.47),
            point("2017-03-01T08:00:00Z", 39.51),
            point("2017-03-01T09:00:00Z", 52.31),
            point("2017-03-01T10:00:00Z", 109.55),
            point("2017-03-01T11:00:00Z", 109.55),
            point("2017-03-01T12:00:00Z", 109.55),
            point("2017-03-01T13:00:00Z", 109.55),
            point("2017-03-01T14:00:00Z", 109.55)
        );

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0.1);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }
}
