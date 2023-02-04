package ru.yandex.solomon.math.operation.comparation;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class PointValueComparatorsTest {

    @Test
    public void compareNumbers() {
        AggrPoint a = doublePoint(5);
        AggrPoint b = doublePoint(10);
        AggrPoint c = doublePoint(15);

        assertThat(compare(MetricType.DGAUGE, a, b), equalTo(-1));
        assertThat(compare(MetricType.DGAUGE, b, c), equalTo(-1));
        assertThat(compare(MetricType.DGAUGE, c, a), equalTo(1));
    }

    @Test
    public void compareHistograms() {
        AggrPoint a = histogramPoint(new double[]{10, 20}, new long[]{2, 0});
        AggrPoint b = histogramPoint(new double[]{10, 20}, new long[]{0, 3});
        AggrPoint c = histogramPoint(new double[]{10, 20}, new long[]{5, 4});

        assertThat(compare(MetricType.HIST, a, b), equalTo(-1));
        assertThat(compare(MetricType.HIST, b, c), equalTo(-1));
        assertThat(compare(MetricType.HIST, c, a), equalTo(1));
    }

    @Test
    public void compareSummaryDouble() {
        AggrPoint a = summaryDoublePoint(1, 2, 3);
        AggrPoint b = summaryDoublePoint(5, 2, 1);
        AggrPoint c = summaryDoublePoint(42);

        assertThat(compare(MetricType.DSUMMARY, a, b), equalTo(-1));
        assertThat(compare(MetricType.DSUMMARY, b, c), equalTo(-1));
        assertThat(compare(MetricType.DSUMMARY, c, a), equalTo(1));
    }

    @Test
    public void compareSummaryInt64() {
        AggrPoint a = summaryInt64Point(1, 2, 3);
        AggrPoint b = summaryInt64Point(5, 2, 1);
        AggrPoint c = summaryInt64Point(42);

        assertThat(compare(MetricType.ISUMMARY, a, b), equalTo(-1));
        assertThat(compare(MetricType.ISUMMARY, b, c), equalTo(-1));
        assertThat(compare(MetricType.ISUMMARY, c, a), equalTo(1));
    }

    private int compare(MetricType type, AggrPoint left, AggrPoint rigt) {
        return PointValueComparators.comparator(type).compare(left, rigt);
    }

    private AggrPoint doublePoint(double value) {
        return AggrPoint.builder()
                .doubleValue(value)
                .build();
    }

    private AggrPoint histogramPoint(double[] bounds, long[] buckets) {
        return AggrPoint.builder()
                .histogram(Histogram.newInstance(bounds, buckets))
                .build();
    }

    private AggrPoint summaryDoublePoint(double... values) {
        DoubleSummaryStatistics summary = DoubleStream.of(values).summaryStatistics();
        return AggrPoint.builder()
                .summary(new ImmutableSummaryDoubleSnapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax()))
                .build();
    }

    private AggrPoint summaryInt64Point(long... values) {
        LongSummaryStatistics summary = LongStream.of(values).summaryStatistics();
        return AggrPoint.builder()
                .summary(new ImmutableSummaryInt64Snapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax()))
                .build();
    }
}
