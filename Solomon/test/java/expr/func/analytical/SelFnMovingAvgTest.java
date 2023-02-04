package ru.yandex.solomon.expression.expr.func.analytical;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnMovingAvgTest extends WindowTestBase {
    @Test
    public void movingAverageOnSameData() {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 8),
            point("2017-03-01T00:15:00Z", 8),
            point("2017-03-01T00:30:00Z", 8),
            point("2017-03-01T00:45:00Z", 8),
            point("2017-03-01T01:00:00Z", 8),
            point("2017-03-01T01:15:00Z", 8),
            point("2017-03-01T01:30:00Z", 8),
            point("2017-03-01T01:45:00Z", 8)
        );

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 8),
            point("2017-03-01T00:15:00Z", 8),
            point("2017-03-01T00:30:00Z", 8),
            point("2017-03-01T00:45:00Z", 8),
            point("2017-03-01T01:00:00Z", 8),
            point("2017-03-01T01:15:00Z", 8),
            point("2017-03-01T01:30:00Z", 8),
            point("2017-03-01T01:45:00Z", 8)
        );

        GraphData result = executeProgram(sourceGraphData, "moving_avg(graphData, 1h);");

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }

    @Test
    public void movingAverage() {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:15:00Z", 4),
            point("2017-03-01T00:30:00Z", -6),
            point("2017-03-01T00:45:00Z", 5),
            point("2017-03-01T01:00:00Z", 15),
            point("2017-03-01T01:15:00Z", 77),
            point("2017-03-01T01:30:00Z", 123),
            point("2017-03-01T01:45:00Z", -24)
        );

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:15:00Z", 4),    // 4 / 1
            point("2017-03-01T00:30:00Z", -1),   // (4 + -6) / 2
            point("2017-03-01T00:45:00Z", 1),    // (4 + -6 + 5) / 3
            point("2017-03-01T01:00:00Z", 4.5),  // (4 + -6 + 5 + 15) / 4
            point("2017-03-01T01:15:00Z", 19),   // (4 + -6 + 5 + 15 + 77) / 5
            point("2017-03-01T01:30:00Z", 42.8), // (-6 + 5 + 15 + 77 + 123) / 5
            point("2017-03-01T01:45:00Z", 39.2)  // (5 + 15 + 77 + 123 + -24) / 5
        );

        GraphData result = executeProgram(sourceGraphData, "moving_avg(graphData, 1h);");

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }
}
