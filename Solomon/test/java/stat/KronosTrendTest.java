package ru.yandex.solomon.math.stat;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.math.GraphDataMath;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.GraphDataArrayList;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

/**
 * @author Ivan Tsybulin
 */
public class KronosTrendTest {

    private GraphData trainData(int days, double bias, double mag, double noise) {
        Instant start = Instant.parse("2019-05-25T21:00:00Z");
        Instant finish = start.plus(Duration.ofDays(days));

        GraphDataArrayList gd = new GraphDataArrayList();
        int k = 1;
        for (Instant i = start; i.isBefore(finish); i = i.plusSeconds(60)) {
            long ts = i.toEpochMilli();
            double value = bias + mag * Math.sin(Duration.between(start, i).toMillis() / 86400e3 * 2 * Math.PI) + noise * k;
            k = -k;

            gd.add(ts, value);
        }
        return gd.buildGraphData();
    }

    @Test
    public void oneDayTest() {
        GraphData probeGraphData = GraphData.of(
                point("2018-06-14T23:00:00Z", 1), // Fri
                point("2018-06-15T07:00:00Z", 2), // Fri
                point("2018-06-16T09:00:00Z", 3), // Sat*
                point("2018-06-17T11:00:00Z", 4), // Sun*
                point("2018-06-18T19:00:00Z", 5)  // Mon
        );

        KronosTrend kt = new KronosTrend(24 * 60, Duration.ofMinutes(5), 6.0 / 11.0, Duration.ofHours(3), 10);

        kt.fit(trainData(1, 5, 0, 0.55));

        GraphData expectedMean = GraphData.of(
                point("2018-06-14T23:00:00Z", 4.89),
                point("2018-06-15T07:00:00Z", 4.89),
                point("2018-06-16T09:00:00Z", 4.95),
                point("2018-06-17T11:00:00Z", 4.95),
                point("2018-06-18T19:00:00Z", 4.89)
        );
        GraphData mean = kt.predictMean(probeGraphData.getTimestamps());

        GraphData expectedVariance = GraphData.of(
                point("2018-06-14T23:00:00Z", 0.363),
                point("2018-06-15T07:00:00Z", 0.363),
                point("2018-06-16T09:00:00Z", 0.33),
                point("2018-06-17T11:00:00Z", 0.33),
                point("2018-06-18T19:00:00Z", 0.363)
        );

        GraphData var = kt.predictVariance(probeGraphData.getTimestamps());

        assertApproxEquals(expectedMean, mean, 1e-14);
        assertApproxEquals(expectedVariance, var, 1e-14);
    }

    @Test
    public void sineTest() {
        GraphData probeGraphData = GraphData.of(
                point("2018-06-14T23:00:00Z", 1), // Fri
                point("2018-06-15T07:00:00Z", 2), // Fri
                point("2018-06-16T09:00:00Z", 3), // Sat*
                point("2018-06-17T11:00:00Z", 4), // Sun*
                point("2018-06-18T19:00:00Z", 5)  // Mon
        );

        KronosTrend kt = new KronosTrend(24 * 60, Duration.ofMinutes(5), 6.0 / 11.0, Duration.ofHours(3), 10);

        kt.fit(trainData(1, 0, 1, 0));

        double sin11avg = 4.999524048722921e-1;
        double sin5avg = 4.999904807569812e-1;
        double sin11var = 1.5705255879922112e-04;
        double sin11varpi = 2.0940082029266522e-04;
        double sin5var = 3.569666293325601e-05;

        GraphData expectedMean = GraphData.of(
                point("2018-06-14T23:00:00Z", sin5avg),
                point("2018-06-15T07:00:00Z", sin5avg),
                point("2018-06-16T09:00:00Z", 0),
                point("2018-06-17T11:00:00Z", -sin11avg),
                point("2018-06-18T19:00:00Z", -sin5avg)
        );
        GraphData mean = kt.predictMean(probeGraphData.getTimestamps());

        GraphData expectedVariance = GraphData.of(
                point("2018-06-14T23:00:00Z", sin5var),
                point("2018-06-15T07:00:00Z", sin5var),
                point("2018-06-16T09:00:00Z", sin11varpi),
                point("2018-06-17T11:00:00Z", sin11var),
                point("2018-06-18T19:00:00Z", sin5var)
        );

        GraphData var = kt.predictVariance(probeGraphData.getTimestamps());

        assertApproxEquals(expectedMean, mean, 1e-14);
        assertApproxEquals(expectedVariance, var, 1e-14);
    }

    @Test
    public void manyDaysTest() {
        GraphData probeGraphData = GraphData.of(
                point("2018-06-14T23:00:00Z", 1), // Fri
                point("2018-06-15T07:00:00Z", 2), // Fri
                point("2018-06-16T09:00:00Z", 3), // Sat*
                point("2018-06-17T11:00:00Z", 4), // Sun*
                point("2018-06-18T19:00:00Z", 5)  // Mon
        );

        KronosTrend kt = new KronosTrend(24 * 60, Duration.ofMinutes(5), 2.0 / 11.0, Duration.ofHours(3), 10);

        kt.fit(trainData(1, 3, 1, 2));
        GraphData oneDayMean = kt.predictMean(probeGraphData.getTimestamps());
        GraphData oneDayVar = kt.predictVariance(probeGraphData.getTimestamps());

        kt = new KronosTrend(24 * 60, Duration.ofMinutes(5), 2.0 / 11.0, Duration.ofHours(3), 10);
        kt.fit(trainData(10, 3, 1, 2));
        GraphData mean = kt.predictMean(probeGraphData.getTimestamps());
        GraphData var = kt.predictVariance(probeGraphData.getTimestamps());

        double var9scale = (9 - 1) * 10.0 / (9 * 10 - 1);
        double var11scale = (11 - 1) * 10.0 / (11 * 10 - 1);

        DoubleArrayView oneDayVarValues = oneDayVar.getValues();

        GraphData expectedVar = GraphData.of(
                point("2018-06-14T23:00:00Z", oneDayVarValues.at(0) * var9scale), // Fri
                point("2018-06-15T07:00:00Z", oneDayVarValues.at(1) * var9scale), // Fri
                point("2018-06-16T09:00:00Z", oneDayVarValues.at(2) * var11scale), // Sat*
                point("2018-06-17T11:00:00Z", oneDayVarValues.at(3) * var11scale), // Sun*
                point("2018-06-18T19:00:00Z", oneDayVarValues.at(4) * var9scale)  // Mon
        );

        assertApproxEquals(oneDayMean, mean, 1e-14);
        assertApproxEquals(expectedVar, var, 1e-14);
    }

    @Test
    public void missingDataTest() {
        GraphData fitData = trainData(1, 5, 0, 0.55);
        GraphData probeGraphData = GraphData.of(
                point("2018-06-14T23:00:00Z", 1), // Fri
                point("2018-06-15T06:58:00Z", 2), // Fri
                point("2018-06-15T07:00:00Z", 2), // Fri
                point("2018-06-15T07:03:00Z", 2), // Fri
                point("2018-06-16T09:00:00Z", 3), // Sat*
                point("2018-06-17T11:00:00Z", 4), // Sun*
                point("2018-06-18T19:00:00Z", 5)  // Mon
        );

        KronosTrend kt = new KronosTrend(24 * 60, Duration.ofMinutes(5), 6.0 / 11.0, Duration.ofHours(3), 11);

        double[] values = fitData.getValues().copyToArray();
        values[10 * 60] = Double.NaN;
        fitData = new GraphData(fitData.getTimeline(), values);

        kt.fit(fitData);

        GraphData expectedMean = GraphData.of(
                point("2018-06-14T23:00:00Z", 4.89),
                point("2018-06-15T06:58:00Z", Double.NaN),
                point("2018-06-15T07:00:00Z", Double.NaN),
                point("2018-06-15T07:03:00Z", Double.NaN),
                point("2018-06-16T09:00:00Z", 4.95),
                point("2018-06-17T11:00:00Z", 4.95),
                point("2018-06-18T19:00:00Z", 4.89)
        );
        GraphData mean = kt.predictMean(probeGraphData.getTimestamps());

        GraphData expectedVariance = GraphData.of(
                point("2018-06-14T23:00:00Z", 0.363),
                point("2018-06-15T06:58:00Z", Double.NaN),
                point("2018-06-15T07:00:00Z", Double.NaN),
                point("2018-06-15T07:03:00Z", Double.NaN),
                point("2018-06-16T09:00:00Z", 0.33),
                point("2018-06-17T11:00:00Z", 0.33),
                point("2018-06-18T19:00:00Z", 0.363)
        );

        GraphData var = kt.predictVariance(probeGraphData.getTimestamps());

        assertApproxEquals(expectedMean, mean, 1e-14);
        assertApproxEquals(expectedVariance, var, 1e-14);
    }

    @Test
    public void adjustedTest() {
        GraphData fitData = trainData(20, 50, 30, 1);
        GraphData probeGraphData = GraphDataMath.tail(trainData(21, 50, 30, 2.5), Duration.ofDays(1).minus(Duration.ofSeconds(1)));

        KronosTrend kt = new KronosTrend(24 * 60, Duration.ofMinutes(5), 2.0 / 11.0, Duration.ofHours(3), 10);

        kt.fit(fitData);
        GraphData adjusted = kt.predictAdjusted(probeGraphData, 0, 0);
        DoubleArrayView adjustedValues = adjusted.getValues();

        for (int i = 0; i < adjustedValues.length(); i++) {
            assertInRange("At index i = " + i, Math.abs(adjustedValues.at(i)), 2, 3);
        }
    }

    private static void assertInRange(String messagePrefix, double value, double low, double high) {
        String message = messagePrefix + " value = " + value + " not in [" + low + ", " + high + "]";
        Assert.assertTrue(message, value >= low);
        Assert.assertTrue(message, value <= high);
    }

    private static void assertApproxEquals(GraphData expected, GraphData actual, final double fuzz) {
        Assert.assertEquals(expected.getTimestamps(), actual.getTimestamps());
        DoubleArrayView expectedVals = expected.getValues();
        DoubleArrayView actualVals = actual.getValues();
        for (int i = 0; i < expectedVals.length(); i++) {
            Assert.assertEquals("At index i = " + i, expectedVals.at(i), actualVals.at(i), fuzz);
        }
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
