package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

/**
 * @author Ivan Tsybulin
 */
public class SelFnSeasonalMeanTest {

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    @Test
    public void emptyTest() {
        GraphData result = ProgramTestSupport
                .expression("seasonal_mean(drop_tail(graphData, 10m), graphData, 1, 'daily', +3h, 0.1);")
                .onSingleLine(GraphData.empty)
                .fromTime("2018-06-02T06:50:00Z")
                .toTime("2018-06-02T06:59:00Z")
                .exec()
                .getAsSingleLine();

        Assert.assertEquals(result, GraphData.empty);
    }

    @Test
    public void seasonalSingleTest() {
        GraphData source = GraphData.of(
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
        GraphData expectedMean = GraphData.of(
            point("2018-06-13T12:00:00Z", workMean),
            point("2018-06-14T12:00:00Z", workMean),
            point("2018-06-15T12:00:00Z", workMean),
            point("2018-06-16T12:00:00Z", wendMean),
            point("2018-06-17T12:00:00Z", wendMean),
            point("2018-06-18T12:00:00Z", workMean),
            point("2018-06-19T12:00:00Z", workMean),
            point("2018-06-20T12:00:00Z", workMean),
            point("2018-06-21T12:00:00Z", workMean),
            point("2018-06-22T12:00:00Z", workMean),
            point("2018-06-22T18:00:00Z", 0.75 * workMean + 0.25 * wendMean),
            point("2018-06-23T00:00:00Z", 0.5  * workMean + 0.5  * wendMean),
            point("2018-06-23T06:00:00Z", 0.25 * workMean + 0.75 * wendMean),
            point("2018-06-23T12:00:00Z", wendMean),
            point("2018-06-23T18:00:00Z", wendMean),
            point("2018-06-24T00:00:00Z", wendMean),
            point("2018-06-24T06:00:00Z", wendMean),
            point("2018-06-24T12:00:00Z", wendMean)
        );

        GraphData result = ProgramTestSupport
                .expression("seasonal_mean(drop_tail(graphData, 4d), graphData, 1, 'work', +0h, 0.41);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertApproxEquals(result, expectedMean, 1e-15);
    }

    @Test
    public void seasonalMultipleTest() {
        GraphData one = GraphData.of(
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

        GraphData two = GraphData.of(
            point("2018-06-13T12:00:00Z", 1), // Wed
            point("2018-06-14T12:00:00Z", 2), // Thu
            point("2018-06-15T12:00:00Z", 3), // Fri
            point("2018-06-16T12:00:00Z", 4), // Sat*
            point("2018-06-17T12:00:00Z", 5), // Sun*
            point("2018-06-18T12:00:00Z", 6), // Mon
            point("2018-06-19T12:00:00Z", 7)  // Tue
        );

        double workMean = (2 + 3 + 6) / 3.;
        double wendMean = (4 + 5) / 2.;
        GraphData expectedMeanOne = GraphData.of(
            point("2018-06-13T12:00:00Z", workMean),
            point("2018-06-14T12:00:00Z", workMean),
            point("2018-06-15T12:00:00Z", workMean),
            point("2018-06-16T12:00:00Z", wendMean),
            point("2018-06-17T12:00:00Z", wendMean),
            point("2018-06-18T12:00:00Z", workMean),
            point("2018-06-19T12:00:00Z", workMean),
            point("2018-06-20T12:00:00Z", workMean),
            point("2018-06-21T12:00:00Z", workMean),
            point("2018-06-22T12:00:00Z", workMean),
            point("2018-06-22T18:00:00Z", 0.75 * workMean + 0.25 * wendMean),
            point("2018-06-23T00:00:00Z", 0.5  * workMean + 0.5  * wendMean),
            point("2018-06-23T06:00:00Z", 0.25 * workMean + 0.75 * wendMean),
            point("2018-06-23T12:00:00Z", wendMean),
            point("2018-06-23T18:00:00Z", wendMean),
            point("2018-06-24T00:00:00Z", wendMean),
            point("2018-06-24T06:00:00Z", wendMean),
            point("2018-06-24T12:00:00Z", wendMean)
        );

        GraphData expectedMeanTwo = GraphData.of(
            point("2018-06-13T12:00:00Z", 1.5),
            point("2018-06-14T12:00:00Z", 1.5),
            point("2018-06-15T12:00:00Z", 1.5),
            point("2018-06-16T12:00:00Z", Double.NaN),
            point("2018-06-17T12:00:00Z", Double.NaN),
            point("2018-06-18T12:00:00Z", 1.5),
            point("2018-06-19T12:00:00Z", 1.5)
        );

        GraphData[] result = ProgramTestSupport
                .expression("seasonal_mean(drop_tail(graphData, 4d), graphData, 1, 'work', +0h, 0.41);")
                .onMultipleLines(one, two)
                .exec()
                .getAsMultipleLines();

        assertApproxEquals(result[0], expectedMeanOne, 1e-15);
        assertApproxEquals(result[1], expectedMeanTwo, 1e-15);
    }

    private static void assertApproxEquals(GraphData expected, GraphData actual, final double fuzz) {
        Assert.assertEquals(expected.getTimestamps(), actual.getTimestamps());
        DoubleArrayView expectedVals = expected.getValues();
        DoubleArrayView actualVals = actual.getValues();
        for (int i = 0; i < expectedVals.length(); i++) {
            Assert.assertEquals(expectedVals.at(i), actualVals.at(i), fuzz);
        }
    }
}
