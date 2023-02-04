package ru.yandex.solomon.math.operation.reduce;

import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.math.operation.Metric;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class OperationCombineTest {
    private static AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    private static AggrPoint point(String time, double value, int count) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .count(count)
                .build();
    }

    private static AggrPoint point(String time, Histogram value) {
        return AggrPoint.builder()
                .time(time)
                .histogram(value)
                .build();
    }

    private static AggrPoint point(String time, Histogram value, int count) {
        return AggrPoint.builder()
                .time(time)
                .histogram(value)
                .count(count)
                .build();
    }

    private static AggrPoint point(String time, LogHistogram value) {
        return AggrPoint.builder()
                .time(time)
                .logHistogram(value)
                .build();
    }

    private static AggrPoint point(String time, LogHistogram value, int count) {
        return AggrPoint.builder()
                .time(time)
                .logHistogram(value)
                .count(count)
                .build();
    }

    private static AggrPoint point(String time, SummaryInt64Snapshot value) {
        return AggrPoint.builder()
                .time(time)
                .summary(value)
                .build();
    }

    private static AggrPoint point(String time, SummaryInt64Snapshot value, int count) {
        return AggrPoint.builder()
                .time(time)
                .summary(value)
                .count(count)
                .build();
    }

    private static AggrPoint point(String time, SummaryDoubleSnapshot value) {
        return AggrPoint.builder()
                .time(time)
                .summary(value)
                .build();
    }

    private static AggrPoint point(String time, SummaryDoubleSnapshot value, int count) {
        return AggrPoint.builder()
                .time(time)
                .summary(value)
                .count(count)
                .build();
    }

    private static Histogram dhistogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    private static SummaryDoubleSnapshot summaryDouble(double... values) {
        DoubleSummaryStatistics summary = DoubleStream.of(values).summaryStatistics();
        return new ImmutableSummaryDoubleSnapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax());
    }

    private static SummaryInt64Snapshot summaryInt64(long... values) {
        LongSummaryStatistics summary = LongStream.of(values).summaryStatistics();
        return new ImmutableSummaryInt64Snapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax());
    }

    @Test
    public void combineNoneTimeseries() {
        OperationCombine action = new OperationCombine(combine(Aggregation.DEFAULT_AGGREGATION));

        List<Metric> result = action.apply(Collections.emptyList());
        assertThat(result, Matchers.emptyIterable());
    }

    @Test
    public void combineSingleTimeSeries() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 3));

        AggrGraphDataArrayList result = apply(combine(Aggregation.SUM), source);
        assertThat(result, equalTo(source));
    }

    @Test
    public void combineDoubles() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 4),
                point("2018-07-04T15:11:00Z", Double.NaN),
                point("2018-07-04T15:12:00Z", 3));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 2),
                point("2018-07-04T15:11:00Z", 5)
                // gap here
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 6, 2),
                point("2018-07-04T15:11:00Z", 5, 1),
                point("2018-07-04T15:12:00Z", 3, 1));

        AggrGraphDataArrayList result = apply(combine(Aggregation.SUM), one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void combineHistogram() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", dhistogram(new double[]{10, 20, 30}, new long[]{1, 2, 3})),
                point("2018-07-04T15:11:00Z", dhistogram(new double[]{10, 20, 30}, new long[]{0, 4, 0})),
                point("2018-07-04T15:12:00Z", dhistogram(new double[]{10, 20, 30}, new long[]{5, 0, 1})));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", dhistogram(new double[]{10, 20, 30}, new long[]{1, 1, 1})),
                point("2018-07-04T15:11:00Z", dhistogram(new double[]{10, 20, 30}, new long[]{20, 3, 4}))
                // gap here
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", dhistogram(new double[]{10, 20, 30}, new long[]{2, 3, 4}), 2),
                point("2018-07-04T15:11:00Z", dhistogram(new double[]{10, 20, 30}, new long[]{20, 7, 4}), 2),
                point("2018-07-04T15:12:00Z", dhistogram(new double[]{10, 20, 30}, new long[]{5, 0, 1}), 1));

        AggrGraphDataArrayList result = apply(combine(Aggregation.SUM), one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void combineLogHistogram() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", LogHistogram.ofBuckets(1, 2, 3)),
                point("2018-07-04T15:11:00Z", LogHistogram.ofBuckets(0, 4, 0)),
                point("2018-07-04T15:12:00Z", LogHistogram.ofBuckets(5, 0, 1)));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", LogHistogram.ofBuckets(1, 1, 1)),
                point("2018-07-04T15:11:00Z", LogHistogram.ofBuckets(20, 3, 4))
                // gap here
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", LogHistogram.ofBuckets(2, 3, 4), 2),
                point("2018-07-04T15:11:00Z", LogHistogram.ofBuckets(20, 7, 4), 2),
                point("2018-07-04T15:12:00Z", LogHistogram.ofBuckets(5, 0, 1), 1));

        AggrGraphDataArrayList result = apply(combine(Aggregation.SUM), one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void combineSummaryDouble() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryDouble(1, 2, 3)),
                point("2018-07-04T15:11:00Z", summaryDouble(0, 4, 0)),
                point("2018-07-04T15:12:00Z", summaryDouble(5, 0, 1)));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryDouble(1, 1, 1)),
                point("2018-07-04T15:11:00Z", summaryDouble(20, 3, 4))
                // gap here
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryDouble(1, 2, 3, 1, 1, 1), 2),
                point("2018-07-04T15:11:00Z", summaryDouble(0, 4, 0, 20, 3, 4), 2),
                point("2018-07-04T15:12:00Z", summaryDouble(5, 0, 1), 1));

        AggrGraphDataArrayList result = apply(combine(Aggregation.SUM), one, two);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void combineSummaryInt64() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryInt64(1, 2, 3)),
                point("2018-07-04T15:11:00Z", summaryInt64(0, 4, 0)),
                point("2018-07-04T15:12:00Z", summaryInt64(5, 0, 1)));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryInt64(1, 1, 1)),
                point("2018-07-04T15:11:00Z", summaryInt64(20, 3, 4))
                // gap here
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryInt64(1, 2, 3, 1, 1, 1), 2),
                point("2018-07-04T15:11:00Z", summaryInt64(0, 4, 0, 20, 3, 4), 2),
                point("2018-07-04T15:12:00Z", summaryInt64(5, 0, 1), 1));

        AggrGraphDataArrayList result = apply(combine(Aggregation.SUM), one, two);
        assertThat(result, equalTo(expected));
    }

    @Test(expected = IllegalArgumentException.class)
    public void combineDifferentTypeNotAvailable() {
        AggrGraphDataArrayList one = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 1),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 3));

        AggrGraphDataArrayList two = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", 3),
                point("2018-07-04T15:11:00Z", 2),
                point("2018-07-04T15:12:00Z", 1));

        AggrGraphDataArrayList tree = AggrGraphDataArrayList.of(
                point("2018-07-04T15:10:44Z", summaryInt64(1, 2, 3)),
                point("2018-07-04T15:11:00Z", summaryInt64(0, 4, 0)),
                point("2018-07-04T15:12:00Z", summaryInt64(5, 0, 1)));

        apply(combine(Aggregation.SUM), one, two, tree);
    }

    private AggrGraphDataArrayList apply(ru.yandex.solomon.math.protobuf.OperationCombine opts, AggrGraphDataArrayList... source) {
        OperationCombine<MetricId> action = new OperationCombine<>(opts);
        Metric<MetricId> result = Iterables.getOnlyElement(action.apply(Stream.of(source)
                .map(l -> new Metric<MetricId>(null, StockpileColumns.typeByMask(l.columnSetMask()), l))
                .collect(toList())));

        if (result.getTimeseries() == null) {
            return null;
        }

        return AggrGraphDataArrayList.of(result.getTimeseries().iterator());
    }

    private static ru.yandex.solomon.math.protobuf.OperationCombine combine(Aggregation sum) {
        return ru.yandex.solomon.math.protobuf.OperationCombine.newBuilder()
            .setAggregation(sum)
            .build();
    }
}
