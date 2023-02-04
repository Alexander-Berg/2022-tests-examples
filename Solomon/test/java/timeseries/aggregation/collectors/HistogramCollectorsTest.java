package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.EnumSet;

import org.junit.Test;

import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.HistogramColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class HistogramCollectorsTest {

    private static PointValueCollector of(Aggregation aggregation) {
        return PointValueCollectors.of(MetricType.HIST, aggregation);
    }

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void none() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        long time = System.currentTimeMillis();
        var hist = histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5});
        AggrPoint point = AggrPoint.builder()
                .time(time)
                .histogram(hist)
                .build();

        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            boolean result = collector.compute(point);
            assertThat(aggregation.name(), result, equalTo(false));
            assertThat(aggregation.name(), point.histogram, equalTo(hist));
            assertThat(aggregation.name(), point.getTsMillis(), equalTo(time));
        }
    }

    @Test
    public void onePoint() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        var expected = histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5});
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            var result = compute(collector, expected);
            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void noneIgnores() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.SUM,
                Aggregation.LAST);

        var expected = histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5});
        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            var result = compute(collector,
                    HistogramColumn.DEFAULT_VALUE,
                    expected,
                    HistogramColumn.DEFAULT_VALUE,
                    HistogramColumn.DEFAULT_VALUE);

            assertThat(aggregation.name(), result, equalTo(expected));
        }
    }

    @Test
    public void last() {
        PointValueCollector last = of(Aggregation.LAST);
        assertThat(compute(last,
                histogram(new double[]{10, 30, 50}, new long[]{2, 4, 3}),
                histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3}),
                histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5})),
                equalTo(histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5})));

        assertThat(compute(last,
                histogram(new double[]{10, 30, 50, 100}, new long[]{4, 3, 2, 1}),
                histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3}),
                histogram(new double[]{10, 30}, new long[]{1, 0})),
                equalTo(histogram(new double[]{10, 30}, new long[]{1, 0})));
    }

    @Test
    public void sum() {
        PointValueCollector sum = of(Aggregation.SUM);
        assertThat(compute(sum,
                histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5}),
                histogram(new double[]{10, 30, 50}, new long[]{2, 2, 3}),
                histogram(new double[]{10, 30, 50}, new long[]{0, 0, 4})),
                equalTo(histogram(new double[]{10, 30, 50}, new long[]{3, 2, 12})));

        assertThat(compute(sum,
                histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5}),
                histogram(new double[]{10, 30, 50}, new long[]{2, 2, 3}),
                histogram(new double[]{10, 30, 40, 50}, new long[]{0, 0, 3, 0})),
                equalTo(histogram(new double[]{10, 30, 40, 50}, new long[]{3, 2, 7, 4})));

        assertThat(compute(sum,
                histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5}),
                histogram(new double[]{10, 30, 50}, new long[]{2, 2, 3}),
                histogram(new double[]{10, 30, 50, 100}, new long[]{0, 0, 2, 1})),
                equalTo(histogram(new double[]{10, 30, 50, 100}, new long[]{3, 2, 10, 1})));

        assertThat(compute(sum,
                histogram(new double[]{10, 30, 50, 100}, new long[]{1, 0, 5, 0}),
                histogram(new double[]{10, 30, 50, 100}, new long[]{2, 2, 3, 8}),
                histogram(new double[]{10, 30, 50}, new long[]{0, 0, 2})),
                equalTo(histogram(new double[]{10, 30, 50, 100}, new long[]{3, 2, 10, 8})));
    }

    private Histogram compute(PointValueCollector collector, Histogram... values) {
        for (Histogram value : values) {
            collector.append(AggrPoint.builder()
                    .time(System.currentTimeMillis())
                    .histogram(value)
                    .build());
        }

        AggrPoint temp = new AggrPoint();
        assertThat(collector.compute(temp), equalTo(true));
        collector.reset();
        return temp.histogram;
    }
}
