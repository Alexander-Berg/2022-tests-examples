package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricTypeTransfersFromCounterTest extends MetricTypeTransfersTest {
    public MetricTypeTransfersFromCounterTest() {
        super(MetricType.COUNTER);
    }

    @Test
    public void toGaugeDouble() {
        AggrGraphDataArrayList source = listOf(
                lpoint("2018-08-10T15:29:58Z", 42L),
                lpoint("2018-08-10T15:30:00Z", 0L),
                lpoint("2018-08-10T15:31:00Z", 2L),
                lpoint("2018-08-10T15:32:00Z", 1000500L),
                lpoint("2018-08-10T15:33:00Z", Long.MAX_VALUE),
                lpoint("2018-08-10T15:34:00Z", Long.MIN_VALUE));

        AggrGraphDataArrayList expected = listOf(
                point("2018-08-10T15:29:58Z", 42d),
                point("2018-08-10T15:30:00Z", 0d),
                point("2018-08-10T15:31:00Z", 2d),
                point("2018-08-10T15:32:00Z", 1000500d),
                point("2018-08-10T15:33:00Z", (double) Long.MAX_VALUE),
                point("2018-08-10T15:34:00Z", (double) Long.MIN_VALUE));

        AggrGraphDataArrayList result = transferTo(MetricType.DGAUGE, source);
        assertEquals(expected.columnSetMask(), result.columnSetMask());
        assertEquals(expected, result);
    }

    @Test
    public void toGaugeInt64() {
        expectedSame(MetricType.IGAUGE);
    }

    @Test
    public void toCounter() {
        expectedSame(MetricType.COUNTER);
    }

    @Test
    public void toRate() {
        expectedSame(MetricType.RATE);
    }

    private void expectedSame(MetricType type) {
        AggrGraphDataArrayList source = listOf(
                lpoint("2018-08-10T15:29:59Z", 42L),
                lpoint("2018-08-10T15:30:00Z", 0L),
                lpoint("2018-08-10T15:31:00Z", 2L),
                lpoint("2018-08-10T15:32:00Z", 1000500L),
                lpoint("2018-08-10T15:33:00Z", Long.MAX_VALUE),
                lpoint("2018-08-10T15:34:00Z", Long.MIN_VALUE));

        AggrGraphDataArrayList result = transferTo(type, source);
        assertEquals(source, result);
    }
}
