package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnDropNanTest {

    @Test
    public void dropOnSingleLine() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T12:44:25Z", Double.NaN),
            point("2017-06-08T12:44:30Z", 2),
            point("2017-06-08T12:44:40Z", Double.NaN),
            point("2017-06-08T12:44:50Z", 3)
        );

        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_nan(graphData);", source);

        GraphData expected = GraphData.of(
            point("2017-06-08T12:44:30Z", 2),
            point("2017-06-08T12:44:50Z", 3)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void dropOnMultipleLines() throws Exception {
        GraphData one = GraphData.of(
            point("2017-06-08T12:44:25Z", Double.NaN),
            point("2017-06-08T12:44:30Z", 2),
            point("2017-06-08T12:44:40Z", Double.NaN),
            point("2017-06-08T12:44:50Z", 3)
        );

        GraphData two = GraphData.of(
            point("2017-06-08T12:44:15Z", 2),
            point("2017-06-08T12:44:40Z", Double.NaN),
            point("2017-06-08T12:44:50Z", Double.NaN)
        );

        GraphData[] result = ProgramTestSupport.expression("drop_nan(graphData);")
            .onMultipleLines(one, two)
            .exec()
            .getAsMultipleLines();

        GraphData expectedOne = GraphData.of(
            point("2017-06-08T12:44:30Z", 2),
            point("2017-06-08T12:44:50Z", 3)
        );

        GraphData expectedTwo = GraphData.of(
            point("2017-06-08T12:44:15Z", 2)
        );

        Assert.assertThat(result[0], CoreMatchers.equalTo(expectedOne));
        Assert.assertThat(result[1], CoreMatchers.equalTo(expectedTwo));
    }

    @Test
    public void dropToEmpty() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T12:44:25Z", Double.NaN),
            point("2017-06-08T12:44:40Z", Double.NaN)
        );

        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_nan(graphData);", source);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void dropNanForEmpty() throws Exception {
        Interval interval = interval("2017-06-08T12:44:25Z", "2017-06-08T12:44:40Z");
        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_nan(graphData);", interval, GraphData.empty);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void dropNanOnLineWithoutNan() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T12:44:25Z", 2),
            point("2017-06-08T12:44:40Z", 1)
        );

        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_nan(graphData);", source);
        Assert.assertThat(result, CoreMatchers.equalTo(source));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    private static Interval interval(String from, String to) {
        return new Interval(Instant.parse(from), Instant.parse(to));
    }
}
