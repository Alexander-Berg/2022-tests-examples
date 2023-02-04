package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.stream.DoubleStream;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.SummaryDoubleColumn;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SummaryDoubleCollectorsTest {
    private static PointValueCollector of(Aggregation aggregation) {
        return PointValueCollectors.of(MetricType.DSUMMARY, aggregation);
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
            assertThat(aggregation.name(), point.summaryDouble, equalTo(SummaryDoubleColumn.DEFAULT_VALUE));
            assertThat(aggregation.name(), point.getTsMillis(), equalTo(time));
        }
    }

    @Test
    public void onePoint() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        SummaryDoubleSnapshot expected = summary(5, 1, 4, 10, 231, -3, 2, 0);
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            SummaryDoubleSnapshot result = compute(collector, expected);
            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void noneIgnores() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        SummaryDoubleSnapshot expected = summary(5, 1, 4, 10, 231, -3, 2, 0);
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            SummaryDoubleSnapshot result = compute(collector,
                    SummaryDoubleColumn.DEFAULT_VALUE,
                    expected,
                    SummaryDoubleColumn.DEFAULT_VALUE,
                    SummaryDoubleColumn.DEFAULT_VALUE);

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
        PointValueCollector sum = of(Aggregation.DEFAULT_AGGREGATION);
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

    private SummaryDoubleSnapshot compute(PointValueCollector collector, SummaryDoubleSnapshot... values) {
        for (SummaryDoubleSnapshot value : values) {
            collector.append(AggrPoint.builder()
                    .time(System.currentTimeMillis())
                    .summary(value)
                    .build());
        }

        AggrPoint temp = new AggrPoint();
        assertThat(collector.compute(temp), equalTo(true));
        collector.reset();
        return temp.summaryDouble;
    }

    private SummaryDoubleSnapshot summary(double... values) {
        DoubleSummaryStatistics summary = DoubleStream.of(values).summaryStatistics();
        return new ImmutableSummaryDoubleSnapshot(summary.getCount(), summary.getSum(), summary.getMin(), summary.getMax(), values[values.length - 1]);
    }
}
