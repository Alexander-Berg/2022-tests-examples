package ru.yandex.solomon.model.timeseries;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class MergingAggrGraphDataIteratorTest {

    @Test
    public void mergeSameTs() {
        AggrGraphDataArrayList left = AggrGraphDataArrayList.of(
                point("2018-04-16T10:59:54.617Z", 42));

        AggrGraphDataArrayList right = AggrGraphDataArrayList.of(
                point("2018-04-16T10:59:54.617Z", 42));

        AggrGraphDataArrayList result = mergeMax(left, right);
        assertThat(result, Matchers.equalTo(left));
    }

    @Test
    public void mergeFillGaps() {
        AggrGraphDataArrayList left = AggrGraphDataArrayList.of(
                point("2018-04-16T10:00:00Z", 1),
                point("2018-04-16T10:00:15Z", 2),
                point("2018-04-16T10:00:30Z", 3),
                // gap
                point("2018-04-16T10:01:00Z", 5),
                point("2018-04-16T10:01:15Z", 6),
                point("2018-04-16T10:01:30Z", 7)
                // gap
        );

        AggrGraphDataArrayList right = AggrGraphDataArrayList.of(
                // gap
                point("2018-04-16T10:00:30Z", 3),
                point("2018-04-16T10:00:45Z", 4),
                point("2018-04-16T10:01:00Z", 5),
                // gap
                point("2018-04-16T10:01:30Z", 7),
                point("2018-04-16T10:01:45Z", 8),
                point("2018-04-16T10:02:00Z", 9)
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-04-16T10:00:00Z", 1),
                point("2018-04-16T10:00:15Z", 2),
                point("2018-04-16T10:00:30Z", 3),
                point("2018-04-16T10:00:45Z", 4),
                point("2018-04-16T10:01:00Z", 5),
                point("2018-04-16T10:01:15Z", 6),
                point("2018-04-16T10:01:30Z", 7),
                point("2018-04-16T10:01:45Z", 8),
                point("2018-04-16T10:02:00Z", 9)
        );

        AggrGraphDataArrayList result = mergeMax(left, right);
        assertThat(result, Matchers.equalTo(expected));
    }

    @Test
    public void mergeAggregates() {
        AggrGraphDataArrayList left = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .doubleValue(3)
                        .merged()
                        .count(2)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .doubleValue(42)
                        .merged()
                        .count(5)
                        .build()
        );

        AggrGraphDataArrayList right = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .doubleValue(55)
                        .merged()
                        .count(5)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .doubleValue(30)
                        .merged()
                        .count(4)
                        .build()
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .doubleValue(55)
                        .merged()
                        .count(5)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .doubleValue(42)
                        .merged()
                        .count(5)
                        .build());

        AggrGraphDataArrayList result = mergeMax(left, right);
        assertThat(result, Matchers.equalTo(expected));
    }

    @Test
    public void mergeCombineLongAggregates() {
        AggrGraphDataArrayList left = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .longValue(3)
                        .merged()
                        .count(2)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .longValue(42)
                        .merged()
                        .count(5)
                        .build()
        );

        AggrGraphDataArrayList right = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .longValue(55)
                        .merged()
                        .count(5)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .longValue(30)
                        .merged()
                        .count(4)
                        .build());

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .longValue(55 + 3)
                        .merged()
                        .count(5 + 2)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .longValue(42 + 30)
                        .merged()
                        .count(5 + 4)
                        .build());

        AggrGraphDataArrayList result = mergeCombine(left, right);
        assertEquals(expected, result);
    }

    @Test
    public void mergeMaxLongAggregates() {
        AggrGraphDataArrayList left = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .longValue(3)
                        .merged()
                        .count(2)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .longValue(42)
                        .merged()
                        .count(5)
                        .build()
        );

        AggrGraphDataArrayList right = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .longValue(55)
                        .merged()
                        .count(5)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .longValue(30)
                        .merged()
                        .count(4)
                        .build());

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2018-04-16T10:00:00Z")
                        .longValue(55)
                        .merged()
                        .count(5)
                        .build(),

                AggrPoint.builder()
                        .time("2018-04-16T10:00:15Z")
                        .longValue(42)
                        .merged()
                        .count(5)
                        .build());

        AggrGraphDataArrayList result = mergeMax(left, right);
        assertEquals(expected, result);
    }

    @Test
    public void mergeDeleteAll() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-08-20T09:04:30Z", 1d),
                point("2018-08-20T09:04:34Z", 2d),
                point("2018-08-20T09:04:35Z", 3d));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-08-20T09:04:00Z", 4d));

        AggrGraphDataArrayList tree = AggrGraphDataArrayList.of(
                point("2018-08-20T09:05:00Z", 5d));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-08-20T09:04:00Z", 4d),
                point("2018-08-20T09:05:00Z", 5d));

        AggrGraphDataArrayList result = AggrGraphDataArrayList.of(
                MergingAggrGraphDataIterator.ofCombineAggregate(Arrays.asList(
                        FilteringBeforeAggrGraphDataIterator.of(Instant.parse("2018-08-20T09:05:00Z").toEpochMilli(), one.iterator()),
                        two.iterator(),
                        tree.iterator())));

        assertEquals(expected, result);
    }

    private AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    private AggrGraphDataArrayList mergeMax(AggrGraphDataArrayList... source) {
        AggrGraphDataListIterator it = Stream.of(source)
                .map(AggrGraphDataArrayListOrView::iterator)
                .collect(collectingAndThen(toList(), MergingAggrGraphDataIterator::ofMaxAggregate));


        AggrGraphDataArrayList result = new AggrGraphDataArrayList(it.columnSetMask(), 1);
        result.addAllFrom(it);
        return result;
    }

    private AggrGraphDataArrayList mergeCombine(AggrGraphDataArrayList... source) {
        AggrGraphDataListIterator it = Stream.of(source)
                .map(AggrGraphDataArrayListOrView::iterator)
                .collect(collectingAndThen(toList(), MergingAggrGraphDataIterator::ofCombineAggregate));


        AggrGraphDataArrayList result = new AggrGraphDataArrayList(it.columnSetMask(), 1);
        result.addAllFrom(it);
        return result;
    }
}
