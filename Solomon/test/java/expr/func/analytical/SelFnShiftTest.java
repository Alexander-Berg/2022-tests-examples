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
public class SelFnShiftTest {

    @Test
    public void shiftFromPastSingle() throws Exception {
        GraphData source = GraphData.of(
            point("2017-06-07T15:46:27Z", 4),
            point("2017-06-07T15:47:45Z", 3),
            point("2017-06-07T15:48:00Z", 2),
            point("2017-06-07T15:49:15Z", -1),

            point("2017-06-08T15:46:00Z", 10),
            point("2017-06-08T15:47:00Z", 21),
            point("2017-06-08T15:48:00Z", 22),
            point("2017-06-08T15:49:00Z", -25)
        );

        Interval interval = interval("2017-06-08T15:46:00Z", "2017-06-08T15:50:00Z");

        GraphData result = ProgramTestSupport.execExprOnSingleLine("shift(graphData, 1d);", interval, source);

        GraphData expected = GraphData.of(
            point("2017-06-08T15:46:27Z", 4),
            point("2017-06-08T15:47:45Z", 3),
            point("2017-06-08T15:48:00Z", 2),
            point("2017-06-08T15:49:15Z", -1)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void shiftFromPastMultiple() throws Exception {
        GraphData first = GraphData.of(
            point("2017-06-07T15:46:27Z", 0),
            point("2017-06-07T15:47:45Z", 4),
            point("2017-06-07T15:48:00Z", 1),
            point("2017-06-07T15:49:15Z", -6),

            point("2017-06-08T15:46:00Z", 10),
            point("2017-06-08T15:47:00Z", 21),
            point("2017-06-08T15:48:00Z", 4),
            point("2017-06-08T15:49:00Z", -25)
        );

        GraphData second = GraphData.of(
            point("2017-06-07T15:46:30Z", 5),
            point("2017-06-07T15:49:55Z", 8),

            point("2017-06-08T15:46:15Z", 9),
            point("2017-06-08T15:46:35Z", 10),
            point("2017-06-08T15:46:45Z", 22),
            point("2017-06-08T15:46:55Z", 41)
        );

        GraphData[] result = ProgramTestSupport.expression("shift(graphData, 1d);")
            .fromTime("2017-06-08T15:46:00Z")
            .toTime("2017-06-08T15:50:00Z")
            .onMultipleLines(first, second)
            .exec()
            .getAsMultipleLines();

        GraphData expectedFirst = GraphData.of(
            point("2017-06-08T15:46:27Z", 0),
            point("2017-06-08T15:47:45Z", 4),
            point("2017-06-08T15:48:00Z", 1),
            point("2017-06-08T15:49:15Z", -6)
        );

        GraphData expectedSecond = GraphData.of(
            point("2017-06-08T15:46:30Z", 5),
            point("2017-06-08T15:49:55Z", 8)
        );

        Assert.assertThat(result[0], CoreMatchers.equalTo(expectedFirst));
        Assert.assertThat(result[1], CoreMatchers.equalTo(expectedSecond));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    private static Interval interval(String from, String to) {
        return new Interval(Instant.parse(from), Instant.parse(to));
    }
}
