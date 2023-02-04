package ru.yandex.solomon.math.operation.map;

import org.junit.Test;

import ru.yandex.solomon.math.operation.Metric;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class OperationCastTest {
    @Test
    public void sameKind() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-03T09:00:00Z", 1.0),
                point("2018-07-03T09:00:15Z", 2.0),
                point("2018-07-03T09:00:30Z", 3.3),
                point("2018-07-03T09:00:35Z", 4.6));

        AggrGraphDataArrayList result = apply(source, MetricType.DGAUGE, MetricType.DGAUGE);
        assertEquals(source, result);
    }

    @Test
    public void fromDGaugeToIGauge() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-03T09:00:00Z", 1.0),
                point("2018-07-03T09:00:15Z", 2.0),
                point("2018-07-03T09:00:30Z", 3.3),
                point("2018-07-03T09:00:35Z", 4.6));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-07-03T09:00:00Z", 1),
                lpoint("2018-07-03T09:00:15Z", 2),
                lpoint("2018-07-03T09:00:30Z", 3),
                lpoint("2018-07-03T09:00:35Z", 5));

        AggrGraphDataArrayList result = apply(source, MetricType.DGAUGE, MetricType.IGAUGE);
        assertEquals(expected, result);
    }

    private AggrGraphDataArrayList apply(AggrGraphDataArrayList source, MetricType from, MetricType to) {
        OperationCast action = new OperationCast(ru.yandex.solomon.math.protobuf.OperationCast.newBuilder().setType(to).build());
        Metric result = action.apply(new Metric(null, from, source));
        assertEquals(to, result.getType());
        if (result.getTimeseries() == null) {
            return null;
        }

        return AggrGraphDataArrayList.of(result.getTimeseries().iterator());
    }
}
