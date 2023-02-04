package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelType;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

/**
 * @author Ivan Tsybulin
 */
public class SelFnKronosAdjustedTest {

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    @Test
    public void emptyTest() {
        GraphData result = ProgramTestSupport
                .expression("kronos_adjusted(drop_tail(graphData, 10m), graphData, 24 * 60, 5m, +3h, 0.1, 10, 0, 0);")
                .onSingleLine(GraphData.empty)
                .fromTime("2018-06-02T06:50:00Z")
                .toTime("2018-06-02T06:59:00Z")
                .exec()
                .getAsSingleLine();

        Assert.assertEquals(GraphData.empty, result);
    }

    @Test
    public void singleTest() {
        GraphData source = GraphData.of(
                point("2018-06-13T12:00:00Z", 1), // Wed
                point("2018-06-14T12:00:00Z", 2), // Thu
                point("2018-06-15T12:00:00Z", 3), // Fri
                point("2018-06-16T12:00:00Z", 4), // Sat*
                point("2018-06-17T12:00:00Z", 5), // Sun*
                point("2018-06-18T12:00:00Z", 6), // Mon
                point("2018-06-19T12:00:00Z", 14), // Tue
                point("2018-06-20T12:00:00Z", 1), // Wed
                point("2018-06-21T12:00:00Z", 2), // Thu
                point("2018-06-22T12:00:00Z", 3), // Fri
                point("2018-06-23T12:00:00Z", 4), // Sat*
                point("2018-06-24T12:00:00Z", 5), // Sun*
                point("2018-06-25T12:00:00Z", 6), // Mon
                point("2018-06-26T12:00:00Z", 14)  // Tue
        );

        double meanWork = 4;
        double meanWend = 5;
        double stdWork = Math.sqrt(Math.max(0.1 * meanWork * meanWork + 1, 20d / 9));
        double stdWend = Math.sqrt(Math.max(0.1 * meanWend * meanWend + 1, 224d / 13));
        GraphData expectedAdj = GraphData.of(
                point("2018-06-13T12:00:00Z", (1 - meanWork) / stdWork), // Wed
                point("2018-06-14T12:00:00Z", (2 - meanWork) / stdWork), // Thu
                point("2018-06-15T12:00:00Z", (3 - meanWork) / stdWork), // Fri
                point("2018-06-16T12:00:00Z", (4 - meanWend) / stdWend), // Sat*
                point("2018-06-17T12:00:00Z", (5 - meanWend) / stdWend), // Sun*
                point("2018-06-18T12:00:00Z", (6 - meanWork) / stdWork), // Mon
                point("2018-06-19T12:00:00Z", (14 - meanWork) / stdWork), // Tue
                point("2018-06-20T12:00:00Z", (1 - meanWork) / stdWork), // Wed
                point("2018-06-21T12:00:00Z", (2 - meanWork) / stdWork), // Thu
                point("2018-06-22T12:00:00Z", (3 - meanWork) / stdWork), // Fri
                point("2018-06-23T12:00:00Z", (4 - meanWend) / stdWend), // Sat*
                point("2018-06-24T12:00:00Z", (5 - meanWend) / stdWend), // Sun*
                point("2018-06-25T12:00:00Z", (6 - meanWork) / stdWork), // Mon
                point("2018-06-26T12:00:00Z", (14 - meanWork) / stdWork)  // Tue
        );

        GraphData result = ProgramTestSupport
                .expression("kronos_adjusted(graphData, graphData, 12, 1h, +0h, 0.3, 5, 0.1, 1);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertApproxEquals(expectedAdj, result,1e-15);
    }

    private SelType getResultTypeFor(String train, String test) {
        return ProgramTestSupport.expression("kronos_adjusted(" + train + ", " + test + ", 12, 1h, +0h, 0.3, 5, 0, 0);")
                .onMultipleLines(GraphData.empty)
                .exec()
                .getAsSelValue()
                .type();
    }

    @Test
    public void mixedTest() {
        String single = "single(graphData)";
        String multi = "graphData";

        Assert.assertTrue(getResultTypeFor(single, single).isGraphData());
        Assert.assertTrue(getResultTypeFor(single, multi).isGraphData());
        Assert.assertTrue(getResultTypeFor(multi, single).isGraphData());
        Assert.assertTrue(getResultTypeFor(multi, multi).isGraphDataVector());
    }

    @Test
    public void multipleTest() {
        GraphData one = GraphData.of(
                point("2018-06-13T12:00:00Z", 1), // Wed
                point("2018-06-14T12:00:00Z", 2), // Thu
                point("2018-06-15T12:00:00Z", 3), // Fri
                point("2018-06-16T12:00:00Z", 4), // Sat*
                point("2018-06-17T12:00:00Z", 5), // Sun*
                point("2018-06-18T12:00:00Z", 6), // Mon
                point("2018-06-19T12:00:00Z", 14), // Tue
                point("2018-06-20T12:00:00Z", 1), // Wed
                point("2018-06-21T12:00:00Z", 2), // Thu
                point("2018-06-22T12:00:00Z", 3), // Fri
                point("2018-06-23T12:00:00Z", 4), // Sat*
                point("2018-06-24T12:00:00Z", 5), // Sun*
                point("2018-06-25T12:00:00Z", 6), // Mon
                point("2018-06-26T12:00:00Z", 14)  // Tue
        );

        GraphData two = GraphData.of(
                point("2018-06-13T12:00:00Z", 42), // Wed
                point("2018-06-14T12:00:00Z", 42), // Thu
                point("2018-06-15T12:00:00Z", 42), // Fri
                point("2018-06-16T12:00:00Z", 42), // Sat*
                point("2018-06-17T12:00:00Z", 42), // Sun*
                point("2018-06-18T12:00:00Z", 42), // Mon
                point("2018-06-19T12:00:00Z", 42)  // Tue
        );

        double meanWork = 4;
        double meanWend = 5;
        double stdWork = Math.sqrt(Math.max(0.1 * meanWork * meanWork + 1, 20d / 9));
        double stdWend = Math.sqrt(Math.max(0.1 * meanWend * meanWend + 1, 224d / 13));
        GraphData expectedAdjOne = GraphData.of(
                point("2018-06-13T12:00:00Z", (1 - meanWork) / stdWork), // Wed
                point("2018-06-14T12:00:00Z", (2 - meanWork) / stdWork), // Thu
                point("2018-06-15T12:00:00Z", (3 - meanWork) / stdWork), // Fri
                point("2018-06-16T12:00:00Z", (4 - meanWend) / stdWend), // Sat*
                point("2018-06-17T12:00:00Z", (5 - meanWend) / stdWend), // Sun*
                point("2018-06-18T12:00:00Z", (6 - meanWork) / stdWork), // Mon
                point("2018-06-19T12:00:00Z", (14 - meanWork) / stdWork), // Tue
                point("2018-06-20T12:00:00Z", (1 - meanWork) / stdWork), // Wed
                point("2018-06-21T12:00:00Z", (2 - meanWork) / stdWork), // Thu
                point("2018-06-22T12:00:00Z", (3 - meanWork) / stdWork), // Fri
                point("2018-06-23T12:00:00Z", (4 - meanWend) / stdWend), // Sat*
                point("2018-06-24T12:00:00Z", (5 - meanWend) / stdWend), // Sun*
                point("2018-06-25T12:00:00Z", (6 - meanWork) / stdWork), // Mon
                point("2018-06-26T12:00:00Z", (14 - meanWork) / stdWork)  // Tue
        );

        GraphData expectedAdjTwo = GraphData.of(
                point("2018-06-13T12:00:00Z", 0), // Wed
                point("2018-06-14T12:00:00Z", 0), // Thu
                point("2018-06-15T12:00:00Z", 0), // Fri
                point("2018-06-16T12:00:00Z", 0), // Sat*
                point("2018-06-17T12:00:00Z", 0), // Sun*
                point("2018-06-18T12:00:00Z", 0), // Mon
                point("2018-06-19T12:00:00Z", 0)  // Tue
        );

        GraphData[] result = ProgramTestSupport
                .expression("kronos_adjusted(graphData, graphData, 12, 1h, +0h, 0.3, 5, 0.1, 1);")
                .onMultipleLines(one, two)
                .exec()
                .getAsMultipleLines();

        assertApproxEquals(expectedAdjOne, result[0], 1e-15);
        assertApproxEquals(expectedAdjTwo, result[1], 1e-15);
    }

    private static void assertApproxEquals(GraphData expected, GraphData actual, final double fuzz) {
        Assert.assertEquals(expected.getTimestamps(), actual.getTimestamps());
        DoubleArrayView expectedVals = expected.getValues();
        DoubleArrayView actualVals = actual.getValues();
        for (int i = 0; i < expectedVals.length(); i++) {
            Assert.assertEquals("At i = " + i, expectedVals.at(i), actualVals.at(i), fuzz);
        }
    }
}
