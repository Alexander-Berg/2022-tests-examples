package ru.yandex.solomon.math.operation.reduce;

import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.solomon.math.operation.Metric;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class OperationTopTest {

    private static AggrGraphDataArrayList empty() {
        return new AggrGraphDataArrayList(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), 10);
    }

    private static AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    @Test
    public void topOnSmall() {
        AggrGraphDataArrayList[] source = {
                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", 1),
                        point("2018-07-04T15:11:00Z", 2),
                        point("2018-07-04T15:12:00Z", 3)),

                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", 3),
                        point("2018-07-04T15:11:00Z", 5),
                        point("2018-07-04T15:12:00Z", 1)),
        };

        AggrGraphDataArrayList[] result = apply(top(Aggregation.DEFAULT_AGGREGATION, 10), source);
        assertArrayEquals(source, result);
    }

    @Test
    public void topByMax() {
        AggrGraphDataArrayList[] source = {
                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", 123),
                        point("2018-07-04T15:11:00Z", 4),
                        point("2018-07-04T15:12:00Z", 3)),

                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", 1),
                        point("2018-07-04T15:11:00Z", 444),
                        point("2018-07-04T15:12:00Z", 4)),

                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", -12),
                        point("2018-07-04T15:11:00Z", 151),
                        point("2018-07-04T15:12:00Z", 6)),

                empty(),
                empty()
        };

        AggrGraphDataArrayList[] expected = {
                source[1],
                source[2],
        };

        AggrGraphDataArrayList[] result = apply(top(Aggregation.MAX, 2), source);
        assertArrayEquals(expected, result);
    }

    @Test
    public void reversedTopByMax() {
        AggrGraphDataArrayList[] source = {
                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", 123),
                        point("2018-07-04T15:11:00Z", 4),
                        point("2018-07-04T15:12:00Z", 3)),

                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", 1),
                        point("2018-07-04T15:11:00Z", 444),
                        point("2018-07-04T15:12:00Z", 4)),

                AggrGraphDataArrayList.of(
                        point("2018-07-04T15:10:44Z", -12),
                        point("2018-07-04T15:11:00Z", 151),
                        point("2018-07-04T15:12:00Z", 6)),

                empty(),
                empty()
        };

        AggrGraphDataArrayList[] expected = {
                source[0],
                source[2],
        };

        AggrGraphDataArrayList[] result = apply(bottom(Aggregation.MAX, 2), source);
        assertArrayEquals(expected, result);
    }

    private AggrGraphDataArrayList[] apply(ru.yandex.solomon.math.protobuf.OperationTop opts, AggrGraphDataArrayList... source) {
        OperationTop<MetricId> action = new OperationTop<>(opts);
        return action.apply(Stream.of(source)
                .map(l -> new Metric<MetricId>(null, StockpileColumns.typeByMask(l.columnSetMask()), l))
                .collect(toList()))
                .stream()
                .map(Metric::getTimeseries)
                .map(ts -> {
                    if (ts == null) {
                        return null;
                    }

                    return AggrGraphDataArrayList.of(ts);
                })
                .toArray(AggrGraphDataArrayList[]::new);
    }

    private static ru.yandex.solomon.math.protobuf.OperationTop top(Aggregation timeAggr, int limit) {
        return ru.yandex.solomon.math.protobuf.OperationTop.newBuilder()
            .setAsc(false)
            .setTimeAggregation(timeAggr)
            .setLimit(limit)
            .build();
    }

    private static ru.yandex.solomon.math.protobuf.OperationTop bottom(Aggregation timeAggr, int limit) {
        return ru.yandex.solomon.math.protobuf.OperationTop.newBuilder()
            .setAsc(true)
            .setTimeAggregation(timeAggr)
            .setLimit(limit)
            .build();
    }
}
