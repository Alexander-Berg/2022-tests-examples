package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricTypeTransfersFromRateTest extends MetricTypeTransfersTest {
    public MetricTypeTransfersFromRateTest() {
        super(MetricType.RATE);
    }

    @Test
    public void toGaugeDouble() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 100),
                lpoint("2018-08-07T14:36:10Z", 200),
                lpoint("2018-08-07T14:36:20Z", 300),
                // gap here
                lpoint("2018-08-07T14:36:40Z", 10),
                lpoint("2018-08-07T14:36:50Z", 80),
                lpoint("2018-08-07T14:37:00Z", 300));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:36:10Z", 10),
                point("2018-08-07T14:36:20Z", 10),
                // absent
                point("2018-08-07T14:36:40Z", 10),
                point("2018-08-07T14:36:50Z", 7),
                point("2018-08-07T14:37:00Z", 22));

        AggrGraphDataArrayList result = transferTo(MetricType.DGAUGE, source);
        result.foldDenomIntoOne();
        assertEquals(expected.columnSetMask(), result.columnSetMask());
        assertEquals(expected, result);
    }

    @Test
    public void toGaugeDoubleUnsigned() {
        long maxSigned = Long.MAX_VALUE;
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", maxSigned - 100),
                lpoint("2018-08-07T14:36:10Z", maxSigned),
                lpoint("2018-08-07T14:36:20Z", maxSigned + 300),
                lpoint("2018-08-07T14:36:40Z", maxSigned + 600),
                lpoint("2018-08-07T14:36:50Z", maxSigned + 1000),
                lpoint("2018-08-07T14:37:00Z", maxSigned + 15000));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-08-07T14:36:10Z", 10d),
                point("2018-08-07T14:36:20Z", 30d),
                point("2018-08-07T14:36:40Z", 15d),
                point("2018-08-07T14:36:50Z", 40d),
                point("2018-08-07T14:37:00Z", 1400d));

        AggrGraphDataArrayList result = transferTo(MetricType.DGAUGE, source);
        result.foldDenomIntoOne();
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
