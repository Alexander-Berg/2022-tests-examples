package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.EnumSet;

import org.junit.Test;

import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.LogHistogramColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class LogHistogramCollectorsTest {
    private static PointValueCollector of(Aggregation aggregation) {
        return PointValueCollectors.of(MetricType.LOG_HISTOGRAM, aggregation);
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
                .logHistogram(LogHistogram.ofBuckets(10, 30, 50))
                .build();

        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            boolean result = collector.compute(point);
            assertThat(aggregation.name(), result, equalTo(false));
            assertThat(aggregation.name(), point.logHistogram, equalTo(LogHistogramColumn.DEFAULT_VALUE));
            assertThat(aggregation.name(), point.getTsMillis(), equalTo(time));
        }
    }

    @Test
    public void onePoint() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        LogHistogram expected = LogHistogram.ofBuckets(5, 1, 4, 10, 231, 0, 2, 0);
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            LogHistogram result = compute(collector, expected);
            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void noneIgnores() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        LogHistogram expected = LogHistogram.ofBuckets(5, 1, 4, 10, 231, 0, 2, 0);
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            LogHistogram result = compute(collector,
                    LogHistogramColumn.DEFAULT_VALUE,
                    expected,
                    LogHistogramColumn.DEFAULT_VALUE,
                    LogHistogramColumn.DEFAULT_VALUE);

            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void last() {
        PointValueCollector last = of(Aggregation.LAST);
        assertThat(compute(last,
                LogHistogram.ofBuckets(5, 1, 4, 10, 231, 0, 2, 0),
                LogHistogram.ofBuckets(1, 2, 3),
                LogHistogram.ofBuckets(5, 10, 2, 0, 0)),
                equalTo(LogHistogram.ofBuckets(5, 10, 2, 0, 0)));

        assertThat(compute(last,
                LogHistogram.ofBuckets(1, 2, 3),
                LogHistogram.ofBuckets(3, 2, 1),
                LogHistogram.ofBuckets(1, 1, 0, 1)),
                equalTo(LogHistogram.ofBuckets(1, 1, 0, 1)));
    }

    @Test
    public void sum() {
        PointValueCollector sum = of(Aggregation.SUM);
        assertThat(compute(sum,
                LogHistogram.ofBuckets(4, 1, 0),
                LogHistogram.ofBuckets(5, 2, 0),
                LogHistogram.ofBuckets(0, 3, 1)),
                equalTo(LogHistogram.ofBuckets(9, 6, 1)));

        assertThat(compute(sum,
                LogHistogram.ofBuckets(4, 1, 0),
                LogHistogram.ofBuckets(5, 2, 0),
                LogHistogram.ofBuckets(0, 3, 1, 2)),
                equalTo(LogHistogram.ofBuckets(9, 6, 1, 2)));
    }

    @Test
    public void defaultSum() {
        PointValueCollector sum = of(Aggregation.DEFAULT_AGGREGATION);
        assertThat(compute(sum,
                LogHistogram.ofBuckets(4, 1, 0),
                LogHistogram.ofBuckets(5, 2, 0),
                LogHistogram.ofBuckets(0, 3, 1)),
                equalTo(LogHistogram.ofBuckets(9, 6, 1)));

        assertThat(compute(sum,
                LogHistogram.ofBuckets(4, 1, 0),
                LogHistogram.ofBuckets(5, 2, 0),
                LogHistogram.ofBuckets(0, 3, 1, 2)),
                equalTo(LogHistogram.ofBuckets(9, 6, 1, 2)));
    }

    private LogHistogram compute(PointValueCollector collector, LogHistogram... values) {
        for (LogHistogram value : values) {
            collector.append(AggrPoint.builder()
                    .time(System.currentTimeMillis())
                    .logHistogram(value)
                    .build());
        }

        AggrPoint temp = new AggrPoint();
        assertThat(collector.compute(temp), equalTo(true));
        collector.reset();
        return temp.logHistogram;
    }

}
