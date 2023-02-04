package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Ivan Tsybulin
 */
public class SelFnFilterByTimeTest {

    final static GraphData one = GraphData.of(
            point("2018-06-19T10:30:00Z", 1),
            point("2018-06-19T12:30:00Z", 2),
            point("2018-06-19T14:30:00Z", 3),
            point("2018-06-19T16:30:00Z", 4),
            point("2018-06-19T18:30:00Z", 5),
            point("2018-06-19T20:30:00Z", 6),
            point("2018-06-19T22:30:00Z", 7),
            point("2018-06-20T00:30:00Z", 8),
            point("2018-06-20T02:30:00Z", 9),
            point("2018-06-20T04:30:00Z", 10),
            point("2018-06-20T06:30:00Z", 11),
            point("2018-06-20T08:30:00Z", 12),
            point("2018-06-20T10:30:00Z", 13),
            point("2018-06-20T12:00:00Z", 14),
            point("2018-06-20T14:00:00Z", 15),
            point("2018-06-20T16:00:00Z", 16),
            point("2018-06-20T18:00:00Z", 17)
        );

    final static GraphData two = GraphData.of(
            point("2018-07-19T10:00:00Z", 101),
            point("2018-07-19T12:00:00Z", 102),
            point("2018-07-19T14:00:00Z", Double.NaN),
            point("2018-07-19T16:00:00Z", 104),
            point("2018-07-19T18:00:00Z", Double.NaN),
            point("2018-07-19T20:00:00Z", 106),
            point("2018-07-19T22:00:00Z", 107),
            point("2018-07-20T00:00:00Z", 108),
            point("2018-07-20T02:00:00Z", 109),
            point("2018-07-20T04:00:00Z", Double.NaN),
            point("2018-07-20T06:00:00Z", 1011),
            point("2018-07-20T08:00:00Z", 1012),
            point("2018-07-20T10:00:00Z", 1013),
            point("2018-07-20T12:00:00Z", 1014),
            point("2018-07-20T14:00:00Z", 1015),
            point("2018-07-20T16:00:00Z", 1016),
            point("2018-07-20T18:00:00Z", Double.NaN)
        );

    @Test
    public void filterOnSingleLine() {

        GraphData result = ProgramTestSupport.execExprOnSingleLine("filter_by_time(graphData, '[11h - 12h30m] + [15h-24h]');", one);

        GraphData expected = GraphData.of(
            point("2018-06-19T12:30:00Z", 2),
            point("2018-06-19T16:30:00Z", 4),
            point("2018-06-19T18:30:00Z", 5),
            point("2018-06-19T20:30:00Z", 6),
            point("2018-06-19T22:30:00Z", 7),
            point("2018-06-20T12:00:00Z", 14),
            point("2018-06-20T16:00:00Z", 16),
            point("2018-06-20T18:00:00Z", 17)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void filterOnMultipleLines() {
        GraphData[] result = ProgramTestSupport.expression("filter_by_time(graphData, '[11h - 12h30m] + [15h-24h]');")
            .onMultipleLines(one, two)
            .exec()
            .getAsMultipleLines();

        GraphData expectedOne = GraphData.of(
            point("2018-06-19T12:30:00Z", 2),
            point("2018-06-19T16:30:00Z", 4),
            point("2018-06-19T18:30:00Z", 5),
            point("2018-06-19T20:30:00Z", 6),
            point("2018-06-19T22:30:00Z", 7),
            point("2018-06-20T12:00:00Z", 14),
            point("2018-06-20T16:00:00Z", 16),
            point("2018-06-20T18:00:00Z", 17)
        );

        GraphData expectedTwo = GraphData.of(
            point("2018-07-19T12:00:00Z", 102),
            point("2018-07-19T16:00:00Z", 104),
            point("2018-07-19T18:00:00Z", Double.NaN),
            point("2018-07-19T20:00:00Z", 106),
            point("2018-07-19T22:00:00Z", 107),
            point("2018-07-20T12:00:00Z", 1014),
            point("2018-07-20T16:00:00Z", 1016),
            point("2018-07-20T18:00:00Z", Double.NaN)
        );

        Assert.assertThat(result[0], CoreMatchers.equalTo(expectedOne));
        Assert.assertThat(result[1], CoreMatchers.equalTo(expectedTwo));
    }

    @Test
    public void filterToEmpty() {
        GraphData result = ProgramTestSupport.execExprOnSingleLine("filter_by_time(graphData, '[0m-1m]');", one);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void filterToAll() {
        GraphData result = ProgramTestSupport.execExprOnSingleLine("filter_by_time(graphData, '[0h-24h]');", one);
        Assert.assertThat(result, CoreMatchers.equalTo(one));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
