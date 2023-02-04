package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Oleg Baryshnikov
 */
public class SelFnDropBelowTest {

    private final static GraphData one = GraphData.of(
        point("2018-08-01T00:00:00Z", 1),
        point("2018-08-02T00:00:00Z", 2),
        point("2018-08-03T00:00:00Z", 3),
        point("2018-08-04T00:00:00Z", 4),
        point("2018-08-05T00:00:00Z", 5),
        point("2018-08-06T00:00:00Z", 6),
        point("2018-08-07T00:00:00Z", 7),
        point("2018-08-08T00:00:00Z", 8),
        point("2018-08-09T00:00:00Z", 9),
        point("2018-08-10T00:00:00Z", 10)
    );

    private final static GraphData two = GraphData.of(
        point("2018-08-01T00:00:00Z", 2),
        point("2018-08-02T00:00:00Z", 4),
        point("2018-08-03T00:00:00Z", Double.NaN),
        point("2018-08-04T00:00:00Z", 6),
        point("2018-08-05T00:00:00Z", 10),
        point("2018-08-06T00:00:00Z", 12),
        point("2018-08-07T00:00:00Z", Double.NaN),
        point("2018-08-08T00:00:00Z", 14),
        point("2018-08-09T00:00:00Z", Double.POSITIVE_INFINITY),
        point("2018-08-10T00:00:00Z", 16)
    );

    @Test
    public void dropBelowForSingleLine() {
        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_below(graphData, 5);", one);

        GraphData expected = GraphData.of(
            point("2018-08-05T00:00:00Z", 5),
            point("2018-08-06T00:00:00Z", 6),
            point("2018-08-07T00:00:00Z", 7),
            point("2018-08-08T00:00:00Z", 8),
            point("2018-08-09T00:00:00Z", 9),
            point("2018-08-10T00:00:00Z", 10)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void dropBelowForSeveralLines() {
        GraphData[] result = ProgramTestSupport.expression("drop_below(graphData, 5);")
            .onMultipleLines(one, two)
            .exec()
            .getAsMultipleLines();

        GraphData expectedOne = GraphData.of(
            point("2018-08-05T00:00:00Z", 5),
            point("2018-08-06T00:00:00Z", 6),
            point("2018-08-07T00:00:00Z", 7),
            point("2018-08-08T00:00:00Z", 8),
            point("2018-08-09T00:00:00Z", 9),
            point("2018-08-10T00:00:00Z", 10)
        );

        GraphData expectedTwo = GraphData.of(
            point("2018-08-04T00:00:00Z", 6),
            point("2018-08-05T00:00:00Z", 10),
            point("2018-08-06T00:00:00Z", 12),
            point("2018-08-08T00:00:00Z", 14),
            point("2018-08-10T00:00:00Z", 16)
        );

        Assert.assertThat(result[0], CoreMatchers.equalTo(expectedOne));
        Assert.assertThat(result[1], CoreMatchers.equalTo(expectedTwo));
    }

    @Test
    public void dropToEmpty() {
        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_below(graphData, 15);", one);
        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void dropAllPoints() {
        GraphData result = ProgramTestSupport.execExprOnSingleLine("drop_below(graphData, -1);", one);
        Assert.assertThat(result, CoreMatchers.equalTo(one));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
