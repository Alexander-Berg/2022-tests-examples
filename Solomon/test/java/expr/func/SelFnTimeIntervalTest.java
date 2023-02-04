package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.DataPoint.point;

/**
 * @author Ivan Tsybulin
 */
public class SelFnTimeIntervalTest {

    @Test
    public void checkIntervalBegin() {
        double ts = ProgramTestSupport.expression("time_interval_begin();")
                .forTimeInterval(new Interval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(2000)))
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertEquals(1000, ts, 0d);
    }

    @Test
    public void checkIntervalEnd() {
        double ts = ProgramTestSupport.expression("time_interval_end();")
                .forTimeInterval(new Interval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(2000)))
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertEquals(2000, ts, 0d);
    }

    @Test
    public void emptyIntervalOneTs() {
        Interval result = ProgramTestSupport.expression("time_interval(graphData);")
                .onSingleLine(GraphData.empty)
                .exec()
                .getAsInterval();

        assertEquals(Interval.EMPTY, result);
    }

    @Test
    public void emptyIntervalManyTs() {
        Interval result = ProgramTestSupport.expression("time_interval(graphData);")
                .onMultipleLines(GraphData.empty, GraphData.empty, GraphData.empty)
                .exec()
                .getAsInterval();

        assertEquals(Interval.EMPTY, result);
    }

    @Test
    public void intervalOneTs() {
        long begin = Instant.parse("2018-04-09T15:24:00Z").toEpochMilli();
        long end = begin + TimeUnit.MINUTES.toMillis(10);
        long ts0 = begin + TimeUnit.SECONDS.toMillis(30);

        GraphData graphData = GraphData.of(
                point(ts0, 1),
                point(ts0 + 10_000, 2),
                point(ts0 + 20_000, 3),
                point(ts0 + 30_000, 4));

        Interval result = ProgramTestSupport.expression("time_interval(graphData);")
                .onSingleLine(graphData)
                .forTimeInterval(Interval.millis(begin, end))
                .exec()
                .getAsInterval();

        assertEquals(graphData.getTimeline().interval(), result);
    }

    @Test
    public void intervalManyTs() {
        long begin = Instant.parse("2018-04-09T15:24:00Z").toEpochMilli();
        long end = begin + TimeUnit.MINUTES.toMillis(10);
        long ts0 = begin + TimeUnit.SECONDS.toMillis(30);

        GraphData gdOne = GraphData.of(
                point(ts0, 1),
                point(ts0 + 10_000, 2),
                point(ts0 + 20_000, 3),
                point(ts0 + 30_000, 4));

        GraphData gdTwo = GraphData.of(
                point(ts0 + 5_000, 1),
                point(ts0 + 10_000, 2),
                point(ts0 + 20_000, 3),
                point(ts0 + 50_000, 5));

        Interval result = ProgramTestSupport.expression("time_interval(graphData);")
                .onMultipleLines(gdOne, gdTwo, GraphData.empty)
                .forTimeInterval(Interval.millis(begin, end))
                .exec()
                .getAsInterval();

        Interval expected = Interval.millis(ts0, ts0 + 50_000);
        assertEquals(expected, result);
    }
}
