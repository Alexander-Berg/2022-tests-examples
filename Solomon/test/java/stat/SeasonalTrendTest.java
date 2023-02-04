package ru.yandex.solomon.math.stat;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.math.GraphDataMath;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

/**
 * @author Ivan Tsybulin
 */
public class SeasonalTrendTest {

    @Test
    public void emptyTest() {
        GraphData sourceGraphData = GraphData.empty;

        CalendarSplitter cal = new CalendarSplitter(12, DailyProfile.DAILY);
        SeasonalTrend st = new SeasonalTrend(cal, 0);

        st.fit(sourceGraphData);
        GraphData mean = st.predictMean(sourceGraphData.getTimestamps());
        GraphData var = st.predictVariance(sourceGraphData.getTimestamps());

        Assert.assertEquals(GraphData.empty, mean);
        Assert.assertEquals(GraphData.empty, var);
    }

    @Test
    public void singlePointTest() {
        GraphData sourceGraphData = GraphData.of(
            point("2018-06-13T09:00:00Z", 1),
            point("2018-06-14T09:00:00Z", 2),
            point("2018-06-15T09:00:00Z", 3),
            point("2018-06-16T09:00:00Z", 4),
            point("2018-06-17T09:00:00Z", 5),
            point("2018-06-18T09:00:00Z", 6),
            point("2018-06-19T09:00:00Z", 7)
        );

        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.DAILY, Duration.ofHours(3));
        SeasonalTrend st = new SeasonalTrend(cal, 0);

        st.fit(sourceGraphData);
        GraphData mean = st.predictMean(sourceGraphData.getTimestamps());

        Assert.assertEquals(sourceGraphData, mean);

        GraphData expectedVariance = GraphData.of(
            point("2018-06-13T09:00:00Z", Double.NaN),
            point("2018-06-14T09:00:00Z", Double.NaN),
            point("2018-06-15T09:00:00Z", Double.NaN),
            point("2018-06-16T09:00:00Z", Double.NaN),
            point("2018-06-17T09:00:00Z", Double.NaN),
            point("2018-06-18T09:00:00Z", Double.NaN),
            point("2018-06-19T09:00:00Z", Double.NaN)
        );

        GraphData var = st.predictVariance(sourceGraphData.getTimestamps());
        Assert.assertEquals(expectedVariance, var);
    }

    @Test
    public void partialFitTest() {
        GraphData sourceGraphData = GraphData.of(
            point("2018-06-13T09:00:00Z", 1),
            point("2018-06-14T09:00:00Z", 2),
            point("2018-06-15T09:00:00Z", 3),
            point("2018-06-16T09:00:00Z", 4),
            point("2018-06-17T09:00:00Z", 5),
            point("2018-06-18T09:00:00Z", 6),
            point("2018-06-19T09:00:00Z", 7)
        );

        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.DAILY, Duration.ofHours(3));
        SeasonalTrend st = new SeasonalTrend(cal, 0);

        GraphData fitData = GraphDataMath.dropTail(sourceGraphData, 3);

        st.fit(fitData);

        GraphData expectedMean = GraphData.of(
            point("2018-06-13T09:00:00Z", 1),
            point("2018-06-14T09:00:00Z", 2),
            point("2018-06-15T09:00:00Z", 3),
            point("2018-06-16T09:00:00Z", 4),
            point("2018-06-17T09:00:00Z", Double.NaN),
            point("2018-06-18T09:00:00Z", Double.NaN),
            point("2018-06-19T09:00:00Z", Double.NaN)
        );

        GraphData mean = st.predictMean(sourceGraphData.getTimestamps());
        Assert.assertEquals(expectedMean, mean);

        GraphData expectedVariance = GraphData.of(
            point("2018-06-13T09:00:00Z", Double.NaN),
            point("2018-06-14T09:00:00Z", Double.NaN),
            point("2018-06-15T09:00:00Z", Double.NaN),
            point("2018-06-16T09:00:00Z", Double.NaN),
            point("2018-06-17T09:00:00Z", Double.NaN),
            point("2018-06-18T09:00:00Z", Double.NaN),
            point("2018-06-19T09:00:00Z", Double.NaN)
        );

        GraphData var = st.predictVariance(sourceGraphData.getTimestamps());
        Assert.assertEquals(expectedVariance, var);
    }

    @Test
    public void adjustedTest() {
        GraphData sourceGraphData = GraphData.of(
            point("2018-06-13T12:00:00Z", 1), // Wed
            point("2018-06-14T12:00:00Z", 2), // Thu
            point("2018-06-15T12:00:00Z", 3), // Fri
            point("2018-06-16T12:00:00Z", 4), // Sat*
            point("2018-06-17T12:00:00Z", 5), // Sun*
            point("2018-06-18T12:00:00Z", 6), // Mon
            point("2018-06-19T12:00:00Z", 7), // Tue
            point("2018-06-20T12:00:00Z", 100500),
            point("2018-06-21T12:00:00Z", 100500),
            point("2018-06-22T12:00:00Z", 100500),
            point("2018-06-22T18:00:00Z", 100500),
            point("2018-06-23T00:00:00Z", 100500),
            point("2018-06-23T06:00:00Z", 100500),
            point("2018-06-23T12:00:00Z", 100500),
            point("2018-06-23T18:00:00Z", 100500),
            point("2018-06-24T00:00:00Z", 100500),
            point("2018-06-24T06:00:00Z", 100500),
            point("2018-06-24T12:00:00Z", 100500)
        );

        double workMean = (2 + 3 + 6) / 3.;
        double wendMean = (4 + 5) / 2.;
        double workVar = 13 / 3.;
        double wendVar = 1 / 2.;
        double workStd = Math.sqrt(workVar);
        double wendStd = Math.sqrt(wendVar);

        GraphData expectedAdjusted = GraphData.of(
            point("2018-06-13T12:00:00Z", (1 - workMean) / workStd),
            point("2018-06-14T12:00:00Z", (2 - workMean) / workStd),
            point("2018-06-15T12:00:00Z", (3 - workMean) / workStd),
            point("2018-06-16T12:00:00Z", (4 - wendMean) / wendStd),
            point("2018-06-17T12:00:00Z", (5 - wendMean) / wendStd),
            point("2018-06-18T12:00:00Z", (6 - workMean) / workStd),
            point("2018-06-19T12:00:00Z", (7 - workMean) / workStd),
            point("2018-06-20T12:00:00Z", (100500 - workMean) / workStd),
            point("2018-06-21T12:00:00Z", (100500 - workMean) / workStd),
            point("2018-06-22T12:00:00Z", (100500 - workMean) / workStd),
            point("2018-06-22T18:00:00Z", (100500 - 0.75 * workMean - 0.25 * wendMean) / Math.sqrt(0.75 * workVar + 0.25 * wendVar)),
            point("2018-06-23T00:00:00Z", (100500 - 0.5  * workMean - 0.5  * wendMean) / Math.sqrt(0.5  * workVar + 0.5  * wendVar)),
            point("2018-06-23T06:00:00Z", (100500 - 0.25 * workMean - 0.75 * wendMean) / Math.sqrt(0.25 * workVar + 0.75 * wendVar)),
            point("2018-06-23T12:00:00Z", (100500 - wendMean) / wendStd),
            point("2018-06-23T18:00:00Z", (100500 - wendMean) / wendStd),
            point("2018-06-24T00:00:00Z", (100500 - wendMean) / wendStd),
            point("2018-06-24T06:00:00Z", (100500 - wendMean) / wendStd),
            point("2018-06-24T12:00:00Z", (100500 - wendMean) / wendStd)
        );

        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.WORK);
        SeasonalTrend st = new SeasonalTrend(cal, 0.41);

        GraphData fitData = GraphDataMath.dropTail(sourceGraphData, Duration.ofDays(4));
        st.fit(fitData);
        GraphData adjusted = st.predictAdjusted(sourceGraphData);

        assertApproxEquals(expectedAdjusted, adjusted, 1e-15);
    }

    @Test
    public void adjustedWithVarianceCorrectionTest() {
        GraphData sourceGraphData = GraphData.of(
            point("2018-06-13T12:00:00Z", 1), // Wed
            point("2018-06-14T12:00:00Z", 2), // Thu
            point("2018-06-15T12:00:00Z", 3), // Fri
            point("2018-06-16T12:00:00Z", 4), // Sat*
            point("2018-06-17T12:00:00Z", 5), // Sun*
            point("2018-06-18T12:00:00Z", 6), // Mon
            point("2018-06-19T12:00:00Z", 7), // Tue
            point("2018-06-20T12:00:00Z", 100500),
            point("2018-06-21T12:00:00Z", 100500),
            point("2018-06-22T12:00:00Z", 100500),
            point("2018-06-22T18:00:00Z", 100500),
            point("2018-06-23T00:00:00Z", 100500),
            point("2018-06-23T06:00:00Z", 100500),
            point("2018-06-23T12:00:00Z", 100500),
            point("2018-06-23T18:00:00Z", 100500),
            point("2018-06-24T00:00:00Z", 100500),
            point("2018-06-24T06:00:00Z", 100500),
            point("2018-06-24T12:00:00Z", 100500)
        );

        double workMean = (2 + 3 + 6) / 3.;
        double wendMean = (4 + 5) / 2.;
        double std = 1e3;

        GraphData expectedAdjusted = GraphData.of(
            point("2018-06-13T12:00:00Z", (1 - workMean) / std),
            point("2018-06-14T12:00:00Z", (2 - workMean) / std),
            point("2018-06-15T12:00:00Z", (3 - workMean) / std),
            point("2018-06-16T12:00:00Z", (4 - wendMean) / std),
            point("2018-06-17T12:00:00Z", (5 - wendMean) / std),
            point("2018-06-18T12:00:00Z", (6 - workMean) / std),
            point("2018-06-19T12:00:00Z", (7 - workMean) / std),
            point("2018-06-20T12:00:00Z", (100500 - workMean) / std),
            point("2018-06-21T12:00:00Z", (100500 - workMean) / std),
            point("2018-06-22T12:00:00Z", (100500 - workMean) / std),
            point("2018-06-22T18:00:00Z", (100500 - 0.75 * workMean - 0.25 * wendMean) / std),
            point("2018-06-23T00:00:00Z", (100500 - 0.5  * workMean - 0.5  * wendMean) / std),
            point("2018-06-23T06:00:00Z", (100500 - 0.25 * workMean - 0.75 * wendMean) / std),
            point("2018-06-23T12:00:00Z", (100500 - wendMean) / std),
            point("2018-06-23T18:00:00Z", (100500 - wendMean) / std),
            point("2018-06-24T00:00:00Z", (100500 - wendMean) / std),
            point("2018-06-24T06:00:00Z", (100500 - wendMean) / std),
            point("2018-06-24T12:00:00Z", (100500 - wendMean) / std)
        );

        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.WORK);
        SeasonalTrend st = new SeasonalTrend(cal, 0.41);

        GraphData fitData = GraphDataMath.dropTail(sourceGraphData, Duration.ofDays(4));
        st.fit(fitData);
        GraphData adjusted = st.predictAdjusted(sourceGraphData, 0, std * std);

        assertApproxEquals(expectedAdjusted, adjusted, 1e-15);
    }

    @Test
    public void sourceWithNans() {
        GraphData sourceGraphData = GraphData.of(
            point("2018-06-13T12:00:00Z", 1), // Wed
            point("2018-06-14T12:00:00Z", 2), // Thu
            point("2018-06-15T12:00:00Z", 3), // Fri
            point("2018-06-16T12:00:00Z", 4), // Sat*
            point("2018-06-17T12:00:00Z", 5), // Sun*
            point("2018-06-18T12:00:00Z", 6), // Mon
            point("2018-06-19T12:00:00Z", 7), // Tue
            point("2018-06-20T12:00:00Z", 11), // Wed
            point("2018-06-21T12:00:00Z", 12), // Thu
            point("2018-06-22T12:00:00Z", Double.NaN), // Fri
            point("2018-06-23T12:00:00Z", 14), // Sat*
            point("2018-06-24T12:00:00Z", 15), // Sun*
            point("2018-06-25T12:00:00Z", 16), // Mon
            point("2018-06-26T12:00:00Z", 17), // Tue
            point("2018-06-27T12:00:00Z", 21), // Wed
            point("2018-06-28T12:00:00Z", 22), // Thu
            point("2018-06-29T12:00:00Z", 23), // Fri
            point("2018-06-30T12:00:00Z", 24), // Sat*
            point("2018-07-01T12:00:00Z", 25), // Sun*
            point("2018-07-02T12:00:00Z", 26), // Mon
            point("2018-07-03T12:00:00Z", 27)  // Tue
        );

        CalendarSplitter cal = new CalendarSplitter(1, DailyProfile.DAILY);
        SeasonalTrend st = new SeasonalTrend(cal, 0);

        st.fit(sourceGraphData);

        GraphData mean = st.predictMean(sourceGraphData.getTimestamps());
        GraphData var = st.predictVariance(sourceGraphData.getTimestamps());

        GraphData expectedMean = GraphData.of(
            point("2018-06-13T12:00:00Z", 11), // Wed
            point("2018-06-14T12:00:00Z", 12), // Thu
            point("2018-06-15T12:00:00Z", 13), // Fri
            point("2018-06-16T12:00:00Z", 14), // Sat*
            point("2018-06-17T12:00:00Z", 15), // Sun*
            point("2018-06-18T12:00:00Z", 16), // Mon
            point("2018-06-19T12:00:00Z", 17), // Tue
            point("2018-06-20T12:00:00Z", 11), // Wed
            point("2018-06-21T12:00:00Z", 12), // Thu
            point("2018-06-22T12:00:00Z", 13), // Fri
            point("2018-06-23T12:00:00Z", 14), // Sat*
            point("2018-06-24T12:00:00Z", 15), // Sun*
            point("2018-06-25T12:00:00Z", 16), // Mon
            point("2018-06-26T12:00:00Z", 17), // Tue
            point("2018-06-27T12:00:00Z", 11), // Wed
            point("2018-06-28T12:00:00Z", 12), // Thu
            point("2018-06-29T12:00:00Z", 13), // Fri
            point("2018-06-30T12:00:00Z", 14), // Sat*
            point("2018-07-01T12:00:00Z", 15), // Sun*
            point("2018-07-02T12:00:00Z", 16), // Mon
            point("2018-07-03T12:00:00Z", 17)  // Tue
        );

        GraphData expectedVar = GraphData.of(
            point("2018-06-13T12:00:00Z", 100), // Wed
            point("2018-06-14T12:00:00Z", 100), // Thu
            point("2018-06-15T12:00:00Z", 200), // Fri
            point("2018-06-16T12:00:00Z", 100), // Sat*
            point("2018-06-17T12:00:00Z", 100), // Sun*
            point("2018-06-18T12:00:00Z", 100), // Mon
            point("2018-06-19T12:00:00Z", 100), // Tue
            point("2018-06-20T12:00:00Z", 100), // Wed
            point("2018-06-21T12:00:00Z", 100), // Thu
            point("2018-06-22T12:00:00Z", 200), // Fri
            point("2018-06-23T12:00:00Z", 100), // Sat*
            point("2018-06-24T12:00:00Z", 100), // Sun*
            point("2018-06-25T12:00:00Z", 100), // Mon
            point("2018-06-26T12:00:00Z", 100), // Tue
            point("2018-06-27T12:00:00Z", 100), // Wed
            point("2018-06-28T12:00:00Z", 100), // Thu
            point("2018-06-29T12:00:00Z", 200), // Fri
            point("2018-06-30T12:00:00Z", 100), // Sat*
            point("2018-07-01T12:00:00Z", 100), // Sun*
            point("2018-07-02T12:00:00Z", 100), // Mon
            point("2018-07-03T12:00:00Z", 100)  // Tue
        );

        assertApproxEquals(expectedMean, mean, 0);
        assertApproxEquals(expectedVar, var, 0);
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
