package ru.yandex.solomon.math.stat;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public class MovingSumTest {
    @Test
    public void simpleMovingSumOnSameData() {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 2),
            point("2017-03-01T00:15:00Z", 2),
            point("2017-03-01T00:30:00Z", 2),
            point("2017-03-01T00:45:00Z", 2),
            point("2017-03-01T01:00:00Z", 2),
            point("2017-03-01T01:15:00Z", 2),
            point("2017-03-01T01:30:00Z", 2),
            point("2017-03-01T01:45:00Z", 2)
        );
        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 2),
            point("2017-03-01T00:15:00Z", 4),
            point("2017-03-01T00:30:00Z", 6),
            point("2017-03-01T00:45:00Z", 8),
            point("2017-03-01T01:00:00Z", 10),
            point("2017-03-01T01:15:00Z", 10),
            point("2017-03-01T01:30:00Z", 10),
            point("2017-03-01T01:45:00Z", 10)
        );

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofHours(1));

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void simpleMovingSumWithSameCountPointInWindow() {
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
            point("2017-03-01T00:15:00Z", 16),     // (5 + 11)
            point("2017-03-01T00:30:00Z", 71),  // (5 + 11 + 55)
            point("2017-03-01T00:45:00Z", 73), // (5 + 11 + 55 + 2)
            point("2017-03-01T01:00:00Z", 82),  // (5 + 11 + 55 + 2 + 9)
            point("2017-03-01T01:15:00Z", 87),  // (11 + 55 + 2 + 9 + 10)
            point("2017-03-01T01:30:00Z", 109),  // (55 + 2 + 9 + 10 + 33)
            point("2017-03-01T01:45:00Z", 55)     // (2 + 9 + 10 + 33 + 1)
        );

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofHours(1));

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0.1);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }

    @Test
    public void simpleMovingSumWithDifferentCountPointInWindow() throws Exception {
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

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofHours(1));

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),     // 1 / 1
            point("2017-03-01T00:01:00Z", 100),    // (1 + 99)
            point("2017-03-01T00:02:00Z", 140),  // (1 + 99 + 40)
            point("2017-03-01T00:03:00Z", 162),  // (1 + 99 + 40 + 22)
            point("2017-03-01T00:10:00Z", 163),  // (1 + 99 + 40 + 22 + 1)
            point("2017-03-01T00:15:00Z", 166),  // (1 + 99 + 40 + 22 + 1 + 3)
            point("2017-03-01T00:30:00Z", 170),  // (1 + 99 + 40 + 22 + 1 + 3 + 4)
            point("2017-03-01T00:45:00Z", 182), // (1 + 99 + 40 + 22 + 1 + 3 + 4 + 12)
            point("2017-03-01T01:00:00Z", 190),  // (1 + 99 + 40 + 22 + 1 + 3 + 4 + 12 + 8)
            point("2017-03-01T01:15:00Z", 68),  // (3 + 4 + 12 + 8 + 41)
            point("2017-03-01T01:30:00Z", 76),  // (4 + 12 + 8 + 41 + 11)
            point("2017-03-01T01:45:00Z", 84)   // (12 + 8 + 41 + 11 + 12)
        );

        Assert.assertArrayEquals(expectedGraphData.getValues().toArray(), result.getValues().toArray(), 0.1);
        Assert.assertArrayEquals(expectedGraphData.getTimestamps().toArray(), result.getTimestamps().toArray());
    }

    @Test
    public void simpleMovingSumWithNegativeItems() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-02T00:00:00Z", -10),
            point("2017-03-03T00:00:00Z", 6),
            point("2017-03-04T00:00:00Z", 1)
        );

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofDays(2));

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-02T00:00:00Z", -6), // (4 + -10)
            point("2017-03-03T00:00:00Z", 0), // (4 + -10 + 6)
            point("2017-03-04T00:00:00Z", -3) // (-10 + 6 + 1)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void simpleMovingSumIgnoreNaNs() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-01T12:00:00Z", Double.NaN),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 6),
            point("2017-03-04T00:00:00Z", 1)
        );

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofDays(2));

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4), // 4
            point("2017-03-02T00:00:00Z", 12), // (4 + 8)
            point("2017-03-03T00:00:00Z", 18), // (4 + 8 + 6)
            point("2017-03-04T00:00:00Z", 15)  // (8 + 6 + 1)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void simpleMovingSumWindowAverageWithSmallWindow() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 9),
            point("2017-03-04T00:00:00Z", 3)
        );

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofHours(6));

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
    public void movingSumPreservesSign() {
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

        GraphData result = MovingSum.simple(sourceGraphData, Duration.ofMinutes(30));

        var negative = result.getValues().stream().filter(value -> value < 0).toArray();
        System.out.println(Arrays.toString(negative));
        Assert.assertEquals(0, negative.length);
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}

