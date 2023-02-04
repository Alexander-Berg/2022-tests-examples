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
public class MovingPercentileTest {

    @Test
    public void movingPercentileWithSmallWindow() {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 9),
            point("2017-03-04T00:00:00Z", 3)
        );

        GraphData result = MovingPercentile.simple(sourceGraphData, Duration.ofHours(6), 90);

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
    public void percentile25() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 20),
            point("2017-03-01T01:00:00Z", 10),
            point("2017-03-01T02:00:00Z", 8),
            point("2017-03-01T03:00:00Z", 13),
            point("2017-03-01T04:00:00Z", 3),
            point("2017-03-01T05:00:00Z", 16),
            point("2017-03-01T06:00:00Z", 15),
            point("2017-03-01T07:00:00Z", 6),
            point("2017-03-01T08:00:00Z", 8),
            point("2017-03-01T09:00:00Z", 9),
            point("2017-03-01T10:00:00Z", 7)
        );

        GraphData result = MovingPercentile.simple(sourceGraphData, Duration.ofDays(1), 25);

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 20),
            point("2017-03-01T01:00:00Z", 10),
            point("2017-03-01T02:00:00Z", 8),
            point("2017-03-01T03:00:00Z", 8.5),
            point("2017-03-01T04:00:00Z", 5.5),
            point("2017-03-01T05:00:00Z", 6.75),
            point("2017-03-01T06:00:00Z", 8),
            point("2017-03-01T07:00:00Z", 6.5),
            point("2017-03-01T08:00:00Z", 7),
            point("2017-03-01T09:00:00Z", 7.5),
            point("2017-03-01T10:00:00Z", 7)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void percentile50() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 40),
            point("2017-03-01T01:00:00Z", 50),
            point("2017-03-01T02:00:00Z", 15),
            point("2017-03-01T03:00:00Z", 35),
            point("2017-03-01T04:00:00Z", 20)
        );

        GraphData result = MovingPercentile.simple(sourceGraphData, Duration.ofDays(1), 50);

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 40),
            point("2017-03-01T01:00:00Z", 45),
            point("2017-03-01T02:00:00Z", 40),
            point("2017-03-01T03:00:00Z", 37.5),
            point("2017-03-01T04:00:00Z", 35)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void percentile99Point9() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 150.5),
            point("2017-03-01T01:00:00Z", 124.12),
            point("2017-03-01T02:00:00Z", -123),
            point("2017-03-01T03:00:00Z", 51),
            point("2017-03-01T04:00:00Z", 0),
            point("2017-03-01T05:00:00Z", 9),
            point("2017-03-01T06:00:00Z", 0),
            point("2017-03-01T07:00:00Z", 193.3),
            point("2017-03-01T08:00:00Z", -55),
            point("2017-03-01T09:00:00Z", -19),
            point("2017-03-01T10:00:00Z", 15),
            point("2017-03-01T11:00:00Z", 75),
            point("2017-03-01T12:00:00Z", 81),
            point("2017-03-01T13:00:00Z", -400),
            point("2017-03-01T14:00:00Z", 0),
            point("2017-03-01T15:00:00Z", -1),
            point("2017-03-01T16:00:00Z", 2),
            point("2017-03-01T17:00:00Z", 34),
            point("2017-03-01T18:00:00Z", 8),
            point("2017-03-01T19:00:00Z", 9),
            point("2017-03-01T20:00:00Z", 0)
        );

        GraphData result = MovingPercentile.simple(sourceGraphData, Duration.ofDays(1), 99.9);

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 150.5),
            point("2017-03-01T01:00:00Z", 150.5),
            point("2017-03-01T02:00:00Z", 150.5),
            point("2017-03-01T03:00:00Z", 150.5),
            point("2017-03-01T04:00:00Z", 150.5),
            point("2017-03-01T05:00:00Z", 150.5),
            point("2017-03-01T06:00:00Z", 150.5),
            point("2017-03-01T07:00:00Z", 193.3),
            point("2017-03-01T08:00:00Z", 193.3),
            point("2017-03-01T09:00:00Z", 193.3),
            point("2017-03-01T10:00:00Z", 193.3),
            point("2017-03-01T11:00:00Z", 193.3),
            point("2017-03-01T12:00:00Z", 193.3),
            point("2017-03-01T13:00:00Z", 193.3),
            point("2017-03-01T14:00:00Z", 193.3),
            point("2017-03-01T15:00:00Z", 193.3),
            point("2017-03-01T16:00:00Z", 193.3),
            point("2017-03-01T17:00:00Z", 193.3),
            point("2017-03-01T18:00:00Z", 193.3),
            point("2017-03-01T19:00:00Z", 193.3),
            point("2017-03-01T20:00:00Z", 193.3)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void percentile25And85Different() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 68),
            point("2017-03-01T01:00:00Z", 96),
            point("2017-03-01T02:00:00Z", 79),
            point("2017-03-01T03:00:00Z", 14),
            point("2017-03-01T04:00:00Z", 16),
            point("2017-03-01T05:00:00Z", 6),
            point("2017-03-01T06:00:00Z", -97),
            point("2017-03-01T07:00:00Z", -63),
            point("2017-03-01T08:00:00Z", 59),
            point("2017-03-01T09:00:00Z", 58),
            point("2017-03-01T10:00:00Z", 15),
            point("2017-03-01T11:00:00Z", -76),
            point("2017-03-01T12:00:00Z", 15),
            point("2017-03-01T13:00:00Z", 5),
            point("2017-03-01T14:00:00Z", -36),
            point("2017-03-01T15:00:00Z", 82),
            point("2017-03-01T16:00:00Z", 82),
            point("2017-03-01T17:00:00Z", 39),
            point("2017-03-01T18:00:00Z", -21),
            point("2017-03-01T19:00:00Z", 56)
        );

        GraphData percentile25 = MovingPercentile.simple(sourceGraphData, Duration.ofDays(1), 25);
        GraphData percentile85 = MovingPercentile.simple(sourceGraphData, Duration.ofDays(1), 85);

        Assert.assertNotEquals(percentile25, percentile85);
    }

    @Test
    public void movingPercentileDifferentCountPointInWindow() throws Exception {
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

        GraphData result = MovingPercentile.simple(sourceGraphData, Duration.ofHours(1), 75);

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 1.0),
            point("2017-03-01T00:01:00Z", 99),
            point("2017-03-01T00:02:00Z", 99),
            point("2017-03-01T00:03:00Z", 84.25),
            point("2017-03-01T00:10:00Z", 69.5),
            point("2017-03-01T00:15:00Z", 54.75),
            point("2017-03-01T00:30:00Z", 40),
            point("2017-03-01T00:45:00Z", 35.5),
            point("2017-03-01T01:00:00Z", 31),
            point("2017-03-01T01:15:00Z", 26.5),
            point("2017-03-01T01:30:00Z", 26.5),
            point("2017-03-01T01:45:00Z", 26.5)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void ignoreNaNs() throws Exception {
        GraphData sourceGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4),
            point("2017-03-01T12:00:00Z", Double.NaN),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 6),
            point("2017-03-04T00:00:00Z", 1)
        );

        GraphData result = MovingPercentile.simple(sourceGraphData, Duration.ofDays(2), 65);

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-01T00:00:00Z", 4.0), // [4]
            point("2017-03-02T00:00:00Z", 7.8), // [4, 8]
            point("2017-03-03T00:00:00Z", 7.2), // [4, 8, 6]
            point("2017-03-04T00:00:00Z", 7.2)  // [8, 6, 1]
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

}
