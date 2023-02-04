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
public class SelFnReplaceNanTest {

    @Test
    public void replaceNanOnTsWithoutNan() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T10:50:14Z", 123),
            point("2017-06-08T10:50:30Z", 321),
            point("2017-06-08T10:50:50Z", 1)
        );

        Interval interval = interval("2017-06-08T10:45:00Z", "2017-06-08T10:55:00Z");

        GraphData result = ProgramTestSupport.execExprOnSingleLine("replace_nan(graphData, 2);", interval, source);

        Assert.assertThat(result, CoreMatchers.equalTo(source));
    }

    @Test
    public void replaceNanOnEmptyLine() throws Exception {
        Interval interval = interval("2017-06-08T10:45:00Z", "2017-06-08T10:55:00Z");
        GraphData result = ProgramTestSupport.execExprOnSingleLine("replace_nan(graphData, 2);", interval, GraphData.empty);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void replaceNanSingleLine() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T10:50:14Z", 123),
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:50:50Z", 1),
            point("2017-06-08T10:51:00Z", Double.NaN),
            point("2017-06-08T10:51:15Z", Double.NaN)
        );

        Interval interval = interval("2017-06-08T10:45:00Z", "2017-06-08T10:55:00Z");
        GraphData result = ProgramTestSupport.execExprOnSingleLine("replace_nan(graphData, 5);", interval, source);

        GraphData expected = GraphData.of(
            point("2017-06-08T10:50:14Z", 123),
            point("2017-06-08T10:50:30Z", 5),
            point("2017-06-08T10:50:50Z", 1),
            point("2017-06-08T10:51:00Z", 5),
            point("2017-06-08T10:51:15Z", 5)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void replaceWithDifferentValue() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:50:50Z", 123),
            point("2017-06-08T10:51:00Z", Double.NaN),
            point("2017-06-08T10:51:15Z", Double.NaN)
        );

        Interval interval = interval("2017-06-08T10:45:00Z", "2017-06-08T10:55:00Z");
        GraphData resultWithOne = ProgramTestSupport.execExprOnSingleLine("replace_nan(graphData, 1);", interval, source);
        GraphData resultWithZero = ProgramTestSupport.execExprOnSingleLine("replace_nan(graphData, 0);", interval, source);

        Assert.assertThat(resultWithOne, CoreMatchers.not(CoreMatchers.equalTo(resultWithZero)));
    }

    @Test
    public void replaceNanForMultipleLines() throws Exception {
        GraphData first = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:50:50Z", 123)
        );

        GraphData second = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:52:00Z", 321),
            point("2017-06-08T10:52:32Z", Double.NaN)
        );

        GraphData[] result = ProgramTestSupport.expression("replace_nan(graphData, 1);")
        .fromTime("2017-06-08T10:45:00Z")
            .toTime("2017-06-08T10:55:00Z")
            .onMultipleLines(first, second)
            .exec()
            .getAsMultipleLines();

        GraphData expectFirst = GraphData.of(
            point("2017-06-08T10:50:30Z", 1),
            point("2017-06-08T10:50:50Z", 123)
        );

        GraphData expectSecond = GraphData.of(
            point("2017-06-08T10:50:30Z", 1),
            point("2017-06-08T10:52:00Z", 321),
            point("2017-06-08T10:52:32Z", 1)
        );

        Assert.assertThat(result[0], CoreMatchers.equalTo(expectFirst));
        Assert.assertThat(result[1], CoreMatchers.equalTo(expectSecond));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    private static Interval interval(String from, String to) {
        return new Interval(Instant.parse(from), Instant.parse(to));
    }
}
