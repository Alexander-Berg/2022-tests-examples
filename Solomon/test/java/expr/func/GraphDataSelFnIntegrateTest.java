package ru.yandex.solomon.expression.expr.func;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.DataPoint.point;

/**
 * @author Vladimir Gordiychuk
 */
public class GraphDataSelFnIntegrateTest {

    @Test
    public void empty() {
        var result = ProgramTestSupport.expression("integrate_fn(graphData);")
            .onSingleLine(GraphData.empty)
            .exec()
            .getAsSingleLine();

        assertEquals(GraphData.empty, result);
    }

    @Test
    public void one() {
        var source = GraphData.of(
            point("2017-06-08T12:44:25Z", 5),
            point("2017-06-08T12:44:30Z", 5),
            point("2017-06-08T12:44:40Z", 10),
            point("2017-06-08T12:44:50Z", 15)
        );

        var result = ProgramTestSupport.expression("integrate_fn(graphData);")
            .onSingleLine(source)
            .exec()
            .getAsSingleLine();

        var expected = GraphData.of(
            point("2017-06-08T12:44:25Z", 0),
            point("2017-06-08T12:44:30Z", 25),
            point("2017-06-08T12:44:40Z", 125),
            point("2017-06-08T12:44:50Z", 275));

        assertEquals(expected, result);
    }

    @Test
    public void two() {
        var one = GraphData.of(
            point("2017-06-08T12:44:25Z", 5),
            point("2017-06-08T12:44:30Z", 5),
            point("2017-06-08T12:44:40Z", 10),
            point("2017-06-08T12:44:50Z", 15)
        );

        var two = GraphData.of(
            point("2017-06-08T12:44:25Z", 15),
            point("2017-06-08T12:44:30Z", 10),
            point("2017-06-08T12:44:40Z", 5),
            point("2017-06-08T12:44:50Z", 5)
        );

        var result = ProgramTestSupport.expression("integrate_fn(graphData);")
            .onMultipleLines(one, two)
            .exec()
            .getAsMultipleLines();

        var expectedOne = GraphData.of(
            point("2017-06-08T12:44:25Z", 0),
            point("2017-06-08T12:44:30Z", 25),
            point("2017-06-08T12:44:40Z", 125),
            point("2017-06-08T12:44:50Z", 275));

        var expectedTwo = GraphData.of(
            point("2017-06-08T12:44:25Z", 0),
            point("2017-06-08T12:44:30Z", 50),
            point("2017-06-08T12:44:40Z", 100),
            point("2017-06-08T12:44:50Z", 150));

        assertEquals(expectedOne, result[0]);
        assertEquals(expectedTwo, result[1]);
    }

    @Test
    public void alias() {
        var one = GraphData.of(
                point("2017-06-08T12:44:25Z", 5),
                point("2017-06-08T12:44:30Z", 5),
                point("2017-06-08T12:44:40Z", 10),
                point("2017-06-08T12:44:50Z", 15)
        );

        var two = GraphData.of(
                point("2017-06-08T12:44:25Z", 15),
                point("2017-06-08T12:44:30Z", 10),
                point("2017-06-08T12:44:40Z", 5),
                point("2017-06-08T12:44:50Z", 5)
        );

        var result = ProgramTestSupport.expression("integral(graphData);")
                .onMultipleLines(one, two)
                .exec()
                .getAsMultipleLines();

        var expectedOne = GraphData.of(
                point("2017-06-08T12:44:25Z", 0),
                point("2017-06-08T12:44:30Z", 25),
                point("2017-06-08T12:44:40Z", 125),
                point("2017-06-08T12:44:50Z", 275));

        var expectedTwo = GraphData.of(
                point("2017-06-08T12:44:25Z", 0),
                point("2017-06-08T12:44:30Z", 50),
                point("2017-06-08T12:44:40Z", 100),
                point("2017-06-08T12:44:50Z", 150));

        assertEquals(expectedOne, result[0]);
        assertEquals(expectedTwo, result[1]);
    }
}
