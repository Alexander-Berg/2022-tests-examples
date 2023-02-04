package ru.yandex.solomon.model.timeseries;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.column.StockpileColumn;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;

/**
 * @author Vladimir Gordiychuk
 */
public class IntegrateAggrGraphDataIteratorTest {
    @Test
    public void empty() {
        AggrGraphDataArrayList result = integrate(AggrGraphDataArrayList.empty());
        assertEquals(AggrGraphDataArrayList.empty(), result);
    }

    @Test
    public void integrateSameSource() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-08-07T14:36:00Z", 0),
                point("2018-08-07T14:36:10Z", 10),
                point("2018-08-07T14:36:20Z", 10),
                point("2018-08-07T14:36:30Z", 10));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 0),
                lpoint("2018-08-07T14:36:10Z", 100),
                lpoint("2018-08-07T14:36:20Z", 200),
                lpoint("2018-08-07T14:36:30Z", 300));

        assertEquals(expected, result);
    }

    @Test
    public void integrateSameResult() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-08-07T14:36:00Z", 10),
                point("2018-08-07T14:36:10Z", 0),
                point("2018-08-07T14:36:20Z", 0),
                point("2018-08-07T14:36:30Z", 0));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 100),
                lpoint("2018-08-07T14:36:10Z", 100),
                lpoint("2018-08-07T14:36:20Z", 100),
                lpoint("2018-08-07T14:36:30Z", 100));

        assertEquals(expected, result);
    }

    @Test
    public void integrateNotStableGrow() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-08-07T14:36:00Z", 0),
                point("2018-08-07T14:36:10Z", 70),
                point("2018-08-07T14:36:20Z", 20),
                point("2018-08-07T14:36:30Z", 50));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 0),
                lpoint("2018-08-07T14:36:10Z", 700),
                lpoint("2018-08-07T14:36:20Z", 900),
                lpoint("2018-08-07T14:36:30Z", 1400));

        assertEquals(expected, result);
    }

    @Test
    public void integrateOnReset() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:36:10Z", 0),
                point("2018-08-07T14:36:20Z", 10),
                // absent
                point("2018-08-07T14:36:50Z", 7),
                point("2018-08-07T14:37:00Z", 22));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:10Z", 0),
                lpoint("2018-08-07T14:36:20Z", 100),
                // gap here
                lpoint("2018-08-07T14:36:50Z", 310),
                lpoint("2018-08-07T14:37:00Z", 530));

        assertEquals(expected, result);
    }

    @Test
    public void integrateConsiderTime() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-08-07T14:30:00Z", 0),
                point("2018-08-07T14:40:00Z", 10_000),
                point("2018-08-07T14:50:00Z", 20_000));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:30:00Z", 0),
                lpoint("2018-08-07T14:40:00Z", 6000000),
                lpoint("2018-08-07T14:50:00Z", 18000000));

        assertEquals(expected, result);
    }

    @Test
    public void integrateConsiderDenoms() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-08-07T14:36:00Z", 10, 10_000),
                point("2018-08-07T14:36:10Z", 55, 10_000),
                point("2018-08-07T14:36:20Z", 25, 10_000),
                point("2018-08-07T14:36:30Z", 15, 10_000));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 10),
                lpoint("2018-08-07T14:36:10Z", 65),
                lpoint("2018-08-07T14:36:20Z", 90),
                lpoint("2018-08-07T14:36:30Z", 105));

        assertEquals(expected, result);
    }

    @Test
    public void integrateResetIntoGaps() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder().time("2018-08-07T14:36:10Z").doubleValue(55).stepMillis(10_000).build(),
                AggrPoint.builder().time("2018-08-07T14:36:20Z").doubleValue(45).stepMillis(10_000).build(),
                AggrPoint.builder().time("2018-08-07T14:37:20Z").doubleValue(15).stepMillis(10_000).build(),
                AggrPoint.builder().time("2018-08-07T14:37:30Z").doubleValue(12).stepMillis(10_000).build());

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:10Z", 550, 10_000),
                lpoint("2018-08-07T14:36:20Z", 1000, 10_000),
                // gap here
                lpoint("2018-08-07T14:37:20Z", 150, 10_000),
                lpoint("2018-08-07T14:37:30Z", 270, 10_000));

        AggrGraphDataArrayList result = integrate(source);
        assertEquals(expected, result);
    }

    @Test
    public void integrateAsUnsignedLong() {
        final long startTime = Instant.parse("2018-08-07T14:36:00Z").toEpochMilli();
        final long stepMillis = 10_000;

        AggrGraphDataArrayList source = new AggrGraphDataArrayList();
        AggrPoint temp = new AggrPoint(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask());
        for (int index = 0; index < 100; index++) {
            temp.tsMillis = startTime + (stepMillis * index);
            temp.valueNum = (double) Long.MAX_VALUE / 100;
            source.addRecord(temp);
        }

        AggrGraphDataArrayList result = integrate(source);

        AggrPoint prev = new AggrPoint();
        AggrPoint point = new AggrPoint();
        AggrGraphDataArrayListViewIterator it = result.iterator();
        it.next(prev);
        long expectedDelta = prev.longValue;
        while (it.next(point)) {
            long delta = point.longValue - prev.longValue;
            assertEquals("prev: " + prev + ", actual:" + point, expectedDelta, delta);
            point.copyTo(prev);
        }
    }

    @Test
    public void safeFirstPoint() {
        final long ts0 = Instant.parse("2018-08-07T14:36:00Z").toEpochMilli();
        final long step = 10_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoints.point(ts0, 10),
                AggrPoints.point(ts0 + step, 10),
                AggrPoints.point(ts0 + step * 2, 10),
                AggrPoints.point(ts0 + step * 3, 10));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint(ts0, 100),
                lpoint(ts0 + step, 200),
                lpoint(ts0 + step * 2, 300),
                lpoint(ts0 + step * 3, 400));

        assertEquals(expected, result);
    }

    @Test
    public void saveFirstPointByDenom() {
        final long ts0 = Instant.parse("2018-08-07T14:36:00Z").toEpochMilli();
        final long step = 10_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts0, 100, step),
                point(ts0 + step, 100, step),
                point(ts0 + step * 2, 100, step),
                point(ts0 + step * 3, 100, step));

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint(ts0, 100),
                lpoint(ts0 + step, 200),
                lpoint(ts0 + step * 2, 300),
                lpoint(ts0 + step * 3, 400));

        assertEquals(expected, result);
    }

    @Test
    public void saveFirstPointByStep() {
        final long ts0 = Instant.parse("2018-08-07T14:36:00Z").toEpochMilli();
        final long step = 10_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder().time(ts0).doubleValue(10).stepMillis(step).build(),
                AggrPoint.builder().time(ts0 + step).doubleValue(10).stepMillis(step).build(),
                AggrPoint.builder().time(ts0 + step * 2).doubleValue(10).stepMillis(step).build(),
                AggrPoint.builder().time(ts0 + step * 3).doubleValue(10).stepMillis(step).build());

        AggrGraphDataArrayList result = integrate(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint(ts0, 100, step),
                lpoint(ts0 + step, 200, step),
                lpoint(ts0 + step * 2, 300, step),
                lpoint(ts0 + step * 3, 400, step));

        assertEquals(expected, result);
    }

    private AggrPoint point(String time, double num) {
        return AggrPoints.point(time, num);
    }

    private AggrPoint point(String time, double num, long denom) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(num, denom)
                .build();
    }

    private AggrPoint point(long time, double num, long denom) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(num, denom)
                .build();
    }

    private AggrGraphDataArrayList integrate(AggrGraphDataArrayList source) {
        return AggrGraphDataArrayList.of(IntegrateAggrGraphDataIterator.of(source));
    }
}
