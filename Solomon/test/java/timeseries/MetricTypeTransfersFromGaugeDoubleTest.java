package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.protobuf.MetricType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class MetricTypeTransfersFromGaugeDoubleTest extends MetricTypeTransfersTest {

    public MetricTypeTransfersFromGaugeDoubleTest() {
        super(MetricType.DGAUGE);
    }

    @Test
    public void toGaugeDouble() {
        AggrGraphDataArrayList source = listOf(
                point("2018-08-10T15:29:59Z", 42d),
                point("2018-08-10T15:30:00Z", 0d),
                point("2018-08-10T15:31:00Z", 1.6d),
                point("2018-08-10T15:32:00Z", 1.2d));

        AggrGraphDataArrayList result = transferTo(MetricType.DGAUGE, source);
        assertEquals(source, result);
    }

    @Test
    public void toGaugeInt64() {
        expectRound(MetricType.IGAUGE);
    }

    @Test
    public void toCounter() {
        expectRound(MetricType.COUNTER);
    }

    @Test
    public void toRate() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-08-07T14:36:00Z", 0),
                point("2018-08-07T14:36:10Z", 70),
                point("2018-08-07T14:36:20Z", 20),
                point("2018-08-07T14:36:30Z", 50));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-08-07T14:36:00Z", 0),
                lpoint("2018-08-07T14:36:10Z", 700),
                lpoint("2018-08-07T14:36:20Z", 900),
                lpoint("2018-08-07T14:36:30Z", 1400));

        AggrGraphDataArrayList result = transferTo(MetricType.RATE, source);
        assertEquals(expected.columnSetMask(), result.columnSetMask());
        assertEquals(expected, result);
    }

    private void expectRound(MetricType target) {
        AggrGraphDataArrayList source = listOf(
                point("2018-08-10T15:29:59Z", 42d),
                point("2018-08-10T15:30:00Z", 0d),
                point("2018-08-10T15:31:00Z", 1.6d),
                point("2018-08-10T15:32:00Z", 1.2d),
                point("2018-08-10T15:33:00Z", (double) Long.MAX_VALUE),
                point("2018-08-10T15:34:00Z", (double) Long.MIN_VALUE),
                point("2018-08-10T15:30:00Z", Double.POSITIVE_INFINITY),
                point("2018-08-10T15:30:00Z", Double.NEGATIVE_INFINITY),
                point("2018-08-10T15:30:00Z", Double.MAX_VALUE),
                point("2018-08-10T15:30:00Z", Double.MIN_VALUE));

        AggrGraphDataArrayList expected = listOf(
                lpoint("2018-08-10T15:29:59Z", 42L),
                lpoint("2018-08-10T15:30:00Z", 0L),
                lpoint("2018-08-10T15:31:00Z", 2L),
                lpoint("2018-08-10T15:32:00Z", 1L),
                lpoint("2018-08-10T15:33:00Z", Long.MAX_VALUE),
                lpoint("2018-08-10T15:34:00Z", Long.MIN_VALUE),
                lpoint("2018-08-10T15:30:00Z", Math.round(Double.POSITIVE_INFINITY)),
                lpoint("2018-08-10T15:30:00Z", Math.round(Double.NEGATIVE_INFINITY)),
                lpoint("2018-08-10T15:30:00Z", Math.round(Double.MAX_VALUE)),
                lpoint("2018-08-10T15:30:00Z", Math.round(Double.MIN_VALUE))
        );

        AggrGraphDataArrayList result = transferTo(target, source);
        assertEquals(expected.columnSetMask(), result.columnSetMask());
        assertEquals(expected, result);
    }
}
