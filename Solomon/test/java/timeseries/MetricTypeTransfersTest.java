package ru.yandex.solomon.model.timeseries;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;

/**
 * @author Vladimir Gordiychuk
 */
abstract class MetricTypeTransfersTest {
    private final MetricType sourceType;

    MetricTypeTransfersTest(MetricType source) {
        this.sourceType = source;
    }

    AggrGraphDataArrayList transferTo(MetricType to, AggrGraphDataArrayList source) {
        AggrGraphDataListIterator it = MetricTypeTransfers.of(sourceType, to, source.iterator());

        AggrGraphDataArrayList result = new AggrGraphDataArrayList(it.columnSetMask(), source.length());
        AggrPoint temp = new AggrPoint(it.columnSetMask());
        while (it.next(temp)) {
            result.addRecord(temp);
        }

        return result;
    }

    AggrGraphDataArrayList listOf(AggrPoint... points) {
        return AggrGraphDataArrayList.of(points);
    }
}
