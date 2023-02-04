package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.EnumSet;
import java.util.LongSummaryStatistics;
import java.util.stream.LongStream;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.SummaryInt64Column;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryInt64CollectorsTest {
    private static PointValueCollector of(Aggregation aggregation) {
        return PointValueCollectors.of(MetricType.ISUMMARY, aggregation);
    }

    @Test
    public void none() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        long time = System.currentTimeMillis();
        AggrPoint point = AggrPoint.builder()
                .time(time)
                .summary(summary(1, 2, 3, 10))
                .build();

        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            boolean result = collector.compute(point);
            assertThat(aggregation.name(), result, equalTo(false));
            assertThat(aggregation.name(), point.summaryInt64, equalTo(SummaryInt64Column.DEFAULT_VALUE));
            assertThat(aggregation.name(), point.getTsMillis(), equalTo(time));
        }
    }

    @Test
    public void onePoint() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        SummaryInt64Snapshot expected = summary(5, 1, 4, 10, 231, -3, 2, 0);
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            SummaryInt64Snapshot result = compute(collector, expected);
            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void noneIgnores() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        SummaryInt64Snapshot expected = summary(5, 1, 4, 10, 231, -3, 2, 0);
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            SummaryInt64Snapshot result = compute(collector,
                    SummaryInt64Column.DEFAULT_VALUE,
                    expected,
                    SummaryInt64Column.DEFAULT_VALUE,
                    SummaryInt64Column.DEFAULT_VALUE);

            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void last() {
        PointValueCollector last = of(Aggregation.LAST);
        assertThat(compute(last,
                summary(5, 1, 4, 10, 231, 0, 2, 0),
                summary(1, 2, 3),
                summary(5, 10, 2, 0, 0)),
                equalTo(summary(5, 10, 2, 0, 0)));

        assertThat(compute(last,
                summary(1, 2, 3),
                summary(3, 2, 1),
                summary(1, 1, 0, 1)),
                equalTo(summary(1, 1, 0, 1)));
    }

    @Test
    public void sum() {
        PointValueCollector sum = of(Aggregation.SUM);
        assertThat(compute(sum,
                summary(4, 1, 0),
                summary(5, 2, 0),
                summary(0, 3, 1)),
                equalTo(summary(4, 1, 0, 5, 2, 0, 0, 3, 1)));

        assertThat(compute(sum,
                summary(4, 1, 0),
                summary(5, 2, 0),
                summary(0, 3, 1, 2)),
                equalTo(summary(4, 1, 0, 5, 2, 0, 0, 3, 1, 2)));
    }

    @Test
    public void defaultSum() {
        PointValueCollector last = of(Aggregation.DEFAULT_AGGREGATION);
        assertThat(compute(last,
                summary(4, 1, 0),
                summary(5, 2, 0),
                summary(0, 3, 1)),
                equalTo(summary(4, 1, 0, 5, 2, 0, 0, 3, 1)));

        assertThat(compute(last,
                summary(4, 1, 0),
                summary(5, 2, 0),
                summary(0, 3, 1, 2)),
                equalTo(summary(4, 1, 0, 5, 2, 0, 0, 3, 1, 2)));
    }

    private SummaryInt64Snapshot compute(PointValueCollector collector, SummaryInt64Snapshot... values) {
        for (SummaryInt64Snapshot value : values) {
            collector.append(AggrPoint.builder()
                    .time(System.currentTimeMillis())
                    .summary(value)
                    .build());
        }

        AggrPoint temp = new AggrPoint();
        assertThat(collector.compute(temp), equalTo(true));
        collector.reset();
        return temp.summaryInt64;
    }

    private SummaryInt64Snapshot summary(long... values) {
        LongSummaryStatistics summary = LongStream.of(values).summaryStatistics();
        return new ImmutableSummaryInt64Snapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax(), values[values.length - 1]);
    }
}
