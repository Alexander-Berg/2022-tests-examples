package ru.yandex.solomon.math.operation.reduce;

import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataIterable;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.solomon.model.timeseries.aggregation.collectors.PointValueCollector;
import ru.yandex.solomon.model.timeseries.aggregation.collectors.PointValueCollectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class CombineIteratorTest {

    private static AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    @Test
    public void sumEmpty() {
        AggrGraphDataArrayList result = combine(Aggregation.SUM,
                AggrGraphDataArrayList.empty(),
                AggrGraphDataArrayList.empty(),
                AggrGraphDataArrayList.empty());

        assertThat(result.length(), equalTo(0));
    }

    @Test
    public void sumTimeByTime() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 3));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 4),
                point("2018-07-04T15:11:00Z", 6),
                point("2018-07-04T15:12:00Z", 5));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 4 + 1),
                point("2018-07-04T15:11:00Z", 6 + 2),
                point("2018-07-04T15:12:00Z", 5 + 3));

        AggrGraphDataArrayList result = combine(Aggregation.SUM, AggrGraphDataArrayList.empty(), one, two, AggrGraphDataArrayList.empty());
        assertThat(result, equalTo(expected));
    }

    @Test
    public void sumWithGaps() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                // gap here
                point("2018-07-04T15:12:00Z", 3),
                point("2018-07-04T15:13:00Z", 4));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 4),
                point("2018-07-04T15:11:00Z", 6),
                // gap here
                point("2018-07-04T15:13:00Z", 5));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 4 + 1),
                point("2018-07-04T15:11:00Z", 6),
                point("2018-07-04T15:12:00Z", 3),
                point("2018-07-04T15:13:00Z", 5 + 4));

        AggrGraphDataArrayList result = combine(Aggregation.SUM, AggrGraphDataArrayList.empty(), one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void sumWithoutSameTs() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 3));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:13:44Z", 4),
                point("2018-07-04T15:14:00Z", 6),
                point("2018-07-04T15:15:00Z", 5));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 3),
                point("2018-07-04T15:13:44Z", 4),
                point("2018-07-04T15:14:00Z", 6),
                point("2018-07-04T15:15:00Z", 5));

        {
            AggrGraphDataArrayList result = combine(Aggregation.SUM, one, two);
            assertThat(result, equalTo(expected));
        }

        {
            AggrGraphDataArrayList result = combine(Aggregation.SUM, two, one);
            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void sumOne() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 3));

        AggrGraphDataArrayList result = combine(Aggregation.SUM, one);
        assertThat(result, equalTo(one));
    }

    @Test
    public void max() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 0),
                point("2018-07-04T15:11:00Z", 4),
                point("2018-07-04T15:12:00Z", 12));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 6),
                point("2018-07-04T15:11:00Z", 1),
                point("2018-07-04T15:12:00Z", 2));

        AggrGraphDataArrayList tree = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", -2),
                point("2018-07-04T15:11:00Z", 8),
                point("2018-07-04T15:12:00Z", 2));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 6),
                point("2018-07-04T15:11:00Z", 8),
                point("2018-07-04T15:12:00Z", 12));

        AggrGraphDataArrayList result = combine(Aggregation.MAX, one, two, tree);
        assertThat(result, equalTo(expected));
    }

    private AggrGraphDataArrayList combine(Aggregation aggregation, AggrGraphDataArrayList... sources) {
        int mask = StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask();
        PointValueCollector collector = PointValueCollectors.of(MetricType.DGAUGE, aggregation);

        AggrGraphDataListIterator it = Stream.of(sources)
                .map(AggrGraphDataIterable::iterator)
                .collect(collectingAndThen(toList(), list -> CombineIterator.of(mask, list, collector)));
        return AggrGraphDataArrayList.of(it);
    }
}
