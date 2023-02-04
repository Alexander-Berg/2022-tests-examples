package ru.yandex.solomon.math.stat;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public class MovingAverageTest {
    @Test
    public void simpleMovingAverageOnSameData() {
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

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofHours(1));

        Assert.assertEquals(sourceGraphData, result);
    }

    @Test
    public void simpleMovingAverageWithSameCountPointInWindow() {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 5),
            point("2017-03-01T00:15:00Z", 11),
            point("2017-03-01T00:30:00Z", 55),
            point("2017-03-01T00:45:00Z", 2),
            point("2017-03-01T01:00:00Z", 9),
            point("2017-03-01T01:15:00Z", 10),
            point("2017-03-01T01:30:00Z", 33),
            point("2017-03-01T01:45:00Z", 1)
        );

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 5),     // 5 / 1
            point("2017-03-01T00:15:00Z", 8),     // (5 + 11) / 2
            point("2017-03-01T00:30:00Z", 23.6),  // (5 + 11 + 55) / 3
            point("2017-03-01T00:45:00Z", 18.25), // (5 + 11 + 55 + 2) / 4
            point("2017-03-01T01:00:00Z", 16.4),  // (5 + 11 + 55 + 2 + 9) / 5
            point("2017-03-01T01:15:00Z", 17.4),  // (11 + 55 + 2 + 9 + 10) / 5
            point("2017-03-01T01:30:00Z", 21.8),  // (55 + 2 + 9 + 10 + 33) / 5
            point("2017-03-01T01:45:00Z", 11)     // (2 + 9 + 10 + 33 + 1) / 5
        );

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofHours(1));

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0.1);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }

    @Test
    public void simpleMovingAverageWithDifferentCountPointInWindow() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),
            point("2017-03-01T00:01:00Z", 99),
            point("2017-03-01T00:02:00Z", 40),
            point("2017-03-01T00:03:00Z", 22),
            point("2017-03-01T00:10:00Z", 1),
            point("2017-03-01T00:15:00Z", 3),
            point("2017-03-01T00:30:00Z", 4),
            point("2017-03-01T00:45:00Z", 12),
            point("2017-03-01T01:00:00Z", 8),
            point("2017-03-01T01:15:00Z", 41),
            point("2017-03-01T01:30:00Z", 11),
            point("2017-03-01T01:45:00Z", 12)
        );

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofHours(1));

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),     // 1 / 1
            point("2017-03-01T00:01:00Z", 50),    // (1 + 50) / 2
            point("2017-03-01T00:02:00Z", 46.6),  // (1 + 50 + 40) / 3
            point("2017-03-01T00:03:00Z", 40.5),  // (1 + 50 + 40 + 22) / 4
            point("2017-03-01T00:10:00Z", 32.6),  // (1 + 50 + 40 + 22 + 1) / 5
            point("2017-03-01T00:15:00Z", 27.6),  // (1 + 50 + 40 + 22 + 1 + 3) / 6
            point("2017-03-01T00:30:00Z", 24.3),  // (1 + 50 + 40 + 22 + 1 + 3 + 4) / 7
            point("2017-03-01T00:45:00Z", 22.75), // (1 + 50 + 40 + 22 + 1 + 3 + 4 + 12) / 8
            point("2017-03-01T01:00:00Z", 21.1),  // (1 + 50 + 40 + 22 + 1 + 3 + 4 + 12 + 8) / 9
            point("2017-03-01T01:15:00Z", 13.6),  // (3 + 4 + 12 + 8 + 41) / 5
            point("2017-03-01T01:30:00Z", 15.2),  // (4 + 12 + 8 + 41 + 11) / 5
            point("2017-03-01T01:45:00Z", 16.8)   // (12 + 8 + 41 + 11 + 12) / 5
        );

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0.1);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }

    @Test
    public void simpleMovingAverageWithNegativeItems() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-02T00:00:00Z", -10),
            point("2017-03-03T00:00:00Z", 6),
            point("2017-03-04T00:00:00Z", 1)
        );

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofDays(2));

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-02T00:00:00Z", -3), // (4 + -10) / 2
            point("2017-03-03T00:00:00Z", 0), // (4 + -10 + 6) / 3
            point("2017-03-04T00:00:00Z", -1) // (-10 + 6 + 1) / 3
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void simpleMovingAverageIgnoreNaNs() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-01T12:00:00Z", Double.NaN),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 6),
            point("2017-03-04T00:00:00Z", 1)
        );

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofDays(2));

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4), // 4 / 1
            point("2017-03-02T00:00:00Z", 6), // (4 + 8) / 2
            point("2017-03-03T00:00:00Z", 6), // (4 + 8 + 6) / 3
            point("2017-03-04T00:00:00Z", 5)  // (8 + 6 + 1) / 3
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void simpleMovingAverageWindowAverageWithSmallWindow() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 9),
            point("2017-03-04T00:00:00Z", 3)
        );

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofHours(6));

        // TODO: add NaNs when window not contain points?
        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 9),
            point("2017-03-04T00:00:00Z", 3)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void movingAvgPreservesSign() {
        GraphData sourceGraphData = GraphData.of(
                point("2017-03-01T00:00:00Z", 0),
                point("2017-03-01T00:15:00Z", 0),
                point("2017-03-01T00:30:00Z", 0.7),
                point("2017-03-01T00:45:00Z", 0.2),
                point("2017-03-01T01:00:00Z", 0),
                point("2017-03-01T01:15:00Z", 0),
                point("2017-03-01T01:30:00Z", 0),
                point("2017-03-01T01:45:00Z", 0)
        );

        // 0.7 + 0.2 - 0.7 - 0.2 is negative in naive floating point

        GraphData result = MovingAverage.simple(sourceGraphData, Duration.ofMinutes(30));

        Assert.assertTrue(result.getValues().stream().allMatch(value -> value >= 0));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
