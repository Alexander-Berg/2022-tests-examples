package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Ivan Tsybulin
 */
public class SelFnDropTailTest {

    @Test
    public void dropTailOnShortLine() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T10:50:14Z", 123),
            point("2017-06-08T10:50:30Z", 321),
            point("2017-06-08T10:50:50Z", 1)
        );

        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_tail(graphData, 10);", source);

        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void dropTailOnEmptyLine() throws Exception {
        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_tail(graphData, 10);", GraphData.empty);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void dropTailOnSingleLine() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-08T10:50:14Z", 123),
            point("2017-06-08T10:50:30Z", Double.NaN),
            point("2017-06-08T10:50:50Z", 1),
            point("2017-06-08T10:51:00Z", Double.NaN),
            point("2017-06-08T10:51:15Z", Double.NaN)
        );

        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_tail(graphData, 3);", source);

        GraphData expected = GraphData.of(
            point("2017-06-08T10:50:14Z", 123),
            point("2017-06-08T10:50:30Z", Double.NaN)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void dropTailForMultipleLines() throws Exception {
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

        GraphData[] result = ProgramTestSupport.expression("drop_tail(graphData, 3);")
            .onMultipleLines(first, second)
            .exec()
            .getAsMultipleLines();

        GraphData expectFirst = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN)
        );

        GraphData expectSecond = GraphData.of(
            point("2017-06-08T10:50:30Z", Double.NaN)
        );

        Assert.assertThat(result[0], CoreMatchers.equalTo(expectFirst));
        Assert.assertThat(result[1], CoreMatchers.equalTo(expectSecond));
    }

    @Test
    public void dropTailByDurationShort() {
        GraphData source = GraphData.of(
                point("2017-06-08T10:50:14Z", 123),
                point("2017-06-08T10:50:30Z", 321),
                point("2017-06-08T10:50:50Z", 1)
        );

        GraphData result = ProgramTestSupport.expression("drop_tail(graphData, 1d);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void dropTailByDuration() {
        GraphData source = GraphData.of(
                point("2017-06-08T10:50:00Z", 1),
                point("2017-06-08T10:51:00Z", 2),
                point("2017-06-08T10:52:00Z", 3),
                point("2017-06-08T10:53:00Z", 4),
                point("2017-06-08T10:54:00Z", 5)
        );

        GraphData expected = GraphData.of(
                point("2017-06-08T10:50:00Z", 1),
                point("2017-06-08T10:51:00Z", 2)
        );

        GraphData result = ProgramTestSupport.expression("drop_tail(graphData, 2m);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void dropTailVectorByDuration() {
        GraphData one = GraphData.of(
                point("2017-06-08T10:50:00Z", 1),
                point("2017-06-08T10:51:00Z", 2),
                point("2017-06-08T10:52:00Z", 3),
                point("2017-06-08T10:53:00Z", 4),
                point("2017-06-08T10:54:00Z", 5)
        );

        GraphData expectedOne = GraphData.of(
                point("2017-06-08T10:50:00Z", 1),
                point("2017-06-08T10:51:00Z", 2)
        );

        GraphData two = GraphData.of(
                point("2017-06-08T10:50:00Z", 1),
                point("2017-06-08T10:51:00Z", 2),
                point("2017-06-08T10:52:00Z", 3),
                point("2017-06-08T10:53:00Z", 4),
                point("2017-06-08T11:00:00Z", 5)
        );

        GraphData expectedTwo = GraphData.of(
                point("2017-06-08T10:50:00Z", 1),
                point("2017-06-08T10:51:00Z", 2),
                point("2017-06-08T10:52:00Z", 3),
                point("2017-06-08T10:53:00Z", 4)
        );

        GraphData[] result = ProgramTestSupport.expression("drop_tail(graphData, 2m);")
                .onMultipleLines(one, two)
                .exec()
                .getAsMultipleLines();

        assertThat(result[0], equalTo(expectedOne));
        assertThat(result[1], equalTo(expectedTwo));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
