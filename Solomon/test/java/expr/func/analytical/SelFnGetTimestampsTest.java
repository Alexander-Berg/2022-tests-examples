package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueVector;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Ivan Tsybulin
 */
public class SelFnGetTimestampsTest {

    @Test
    public void getTimestampsOnSingleLine() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T10:50:00Z", 123),
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:50:50Z", 1)
        );

        SelValueVector expect = new SelValueVector(new double[]{
                1496919000,
                1496919050
        });


        SelValue result = ProgramTestSupport.expression("get_timestamps(graphData);")
                .onSingleLine(source)
                .exec()
                .getAsVector();

        Assert.assertThat(result, CoreMatchers.equalTo(expect));
    }

    @Test(expected = EvaluationException.class)
    public void throwOnMultipleLines() throws Exception {
        GraphData first = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:50:50Z", 123),
            point("2017-06-08T10:51:00Z", 321),
            point("2017-06-08T10:52:00Z", 543)
        );

        GraphData second = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:52:00Z", 321),
            point("2017-06-08T10:52:32Z", Double.NaN),
            point("2017-06-08T10:53:32Z", 123)
        );

        ProgramTestSupport.expression("get_timestamps(graphData);")
            .onMultipleLines(first, second)
            .exec()
            .getAsVector();
    }

    @Test
    public void getTimestampsOnSingletonVector() throws Exception {
        GraphData first = GraphData.of(
                point("2017-06-08T10:50:30Z", Double.NaN),
                point("2017-06-08T10:50:50Z", 123),
                point("2017-06-08T10:51:00Z", 321),
                point("2017-06-08T10:52:00Z", 543)
        );

        SelValueVector result = ProgramTestSupport.expression("get_timestamps(graphData);")
                .onMultipleLines(first)
                .exec()
                .getAsVector();

        SelValueVector expectFirst = new SelValueVector(new double [] {
                1496919050,
                1496919060,
                1496919120
        });

        Assert.assertThat(result.castToVector(), CoreMatchers.equalTo(expectFirst));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
