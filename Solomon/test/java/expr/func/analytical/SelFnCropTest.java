package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.MultilineProgramTestSupport;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertArrayEquals;
import static ru.yandex.solomon.model.point.DataPoint.point;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnCropTest {

    @Test
    public void cropByAnotherTsInterval() {
        long now = Instant.parse("2018-04-09T15:00:00Z").toEpochMilli();
        long dayShift = TimeUnit.DAYS.toMillis(1);
        GraphData gd = GraphData.of(
                // data into past doesn't have gaps
                point(now - dayShift - 50_000, 1),
                point(now - dayShift - 40_000, 2),
                point(now - dayShift - 30_000, 3),
                point(now - dayShift - 20_000, 4),
                point(now - dayShift - 10_000, 5),
                point(now - dayShift, 6),
                // gap
                point(now - 40_000, 8),
                point(now - 30_000, 9),
                point(now - 20_000, 10),
                point(now - 10_000, 11)
                // gap
        );

        // absent 10 seconds at begin and 10 seconds at end
        Interval interval = Interval.millis(now - 50_000, now);

        GraphData result = MultilineProgramTestSupport.create()
                .addLine("let now = {sensor=mySensor};")
                .addLine("let past = shift({sensor=mySensor}, 1d);")
                .addLine("let interval = time_interval(now);")
                .addLine("let cropped = crop(past, interval);")
                .forTimeInterval(interval)
                .onData("sensor=mySensor", gd)
                .exec()
                .get("cropped")
                .getAsSingleLine();

        double[] expected = new double[] {
                // gap, because cropped
                2,
                3,
                4,
                5,
                // gap, because cropped
        };

        assertArrayEquals(expected, result.getValues().copyOrArray(), 0.0);
    }
}
