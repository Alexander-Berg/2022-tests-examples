package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.EnumSet;

import org.junit.Test;

import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class DoubleCollectorsTest {

    private static PointValueCollector of(Aggregation aggregation) {
        return PointValueCollectors.of(MetricType.DGAUGE, aggregation);
    }

    @Test
    public void none() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.MAX,
                Aggregation.MIN,
                Aggregation.SUM,
                Aggregation.AVG,
                Aggregation.LAST,
                Aggregation.COUNT);

        long time = System.currentTimeMillis();
        AggrPoint point = AggrPoint.builder()
                .time(time)
                .doubleValue(42d)
                .build();

        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            boolean result = collector.compute(point);
            assertThat(aggregation.name(), result, equalTo(false));
            assertThat(aggregation.name(), point.getValueDivided(), equalTo(Double.NaN));
            assertThat(aggregation.name(), point.getTsMillis(), equalTo(time));
        }
    }

    @Test
    public void onePoint() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.MAX,
                Aggregation.MIN,
                Aggregation.SUM,
                Aggregation.AVG,
                Aggregation.LAST);

        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            double result = compute(collector, 42d);
            assertThat(aggregation.name(), result, equalTo(42d));
        }
    }

    @Test
    public void nanIgnores() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.MAX,
                Aggregation.MIN,
                Aggregation.SUM,
                Aggregation.AVG,
                Aggregation.LAST);

        for (Aggregation aggregation : source) {
            PointValueCollector collector = of(aggregation);
            double result = compute(collector, Double.NaN, 42d, Double.NaN, Double.NaN, Double.NaN);
            assertThat(aggregation.name(), result, equalTo(42d));
        }
    }

    @Test
    public void last() {
        PointValueCollector last = of(Aggregation.LAST);
        assertThat(compute(last, 1d, 50d, Double.NaN), equalTo(50d));
        assertThat(compute(last, 1d, Double.NaN, 50d), equalTo(50d));
        assertThat(compute(last, Double.NaN, 1d, 50d), equalTo(50d));
        assertThat(compute(last, Double.NaN, 1d, 50d, 2d, Double.NaN, 5d, 3d, Double.NaN), equalTo(3d));
    }

    @Test
    public void sum() {
        PointValueCollector sum = of(Aggregation.SUM);
        assertThat(compute(sum, 1d, 5d, Double.NaN), equalTo(6d));
        assertThat(compute(sum, 1d, Double.NaN, 5d), equalTo(6d));
        assertThat(compute(sum, Double.NaN, 1d, 5d), equalTo(6d));
        assertThat(compute(sum, Double.NaN, 1d, 2d, Double.NaN, 4d, 5d, Double.NaN), equalTo(12d));
    }

    @Test
    public void max() {
        PointValueCollector max = of(Aggregation.MAX);
        assertThat(compute(max, 1d, 3d, Double.NaN), equalTo(3d));
        assertThat(compute(max, 2d, Double.NaN, 5d), equalTo(5d));
        assertThat(compute(max, Double.NaN, 5d, 1d), equalTo(5d));
        assertThat(compute(max, Double.NaN, 1d, 314d, Double.NaN, 4d, 2d, Double.NaN), equalTo(314d));
    }

    @Test
    public void min() {
        PointValueCollector min = of(Aggregation.MIN);
        assertThat(compute(min, 1d, 3d, Double.NaN), equalTo(1d));
        assertThat(compute(min, 2d, Double.NaN, 5d), equalTo(2d));
        assertThat(compute(min, Double.NaN, 5d, 1d), equalTo(1d));
        assertThat(compute(min, Double.NaN, 1d, 314d, Double.NaN, -42d, 2d, Double.NaN), equalTo(-42d));
    }

    @Test
    public void count() {
        PointValueCollector count = of(Aggregation.COUNT);
        assertThat(compute(count, 1d, 3d, Double.NaN), equalTo(2d));
        assertThat(compute(count, 2d, Double.NaN, 5d), equalTo(2d));
        assertThat(compute(count, Double.NaN, 5d, 1d), equalTo(2d));
        assertThat(compute(count, Double.NaN, 1d, 314d, Double.NaN, -42d, 2d, Double.NaN), equalTo(4d));
    }

    @Test
    public void avg() {
        PointValueCollector avg = of(Aggregation.AVG);
        assertThat(compute(avg, 1d, 1d, Double.NaN), equalTo(1d));
        assertThat(compute(avg, 1d, 4d, Double.NaN), equalTo(2.5d));
        assertThat(compute(avg, Double.NaN, 1d, -5d, 3d, 8d, 300d, 5d, Double.NaN), equalTo(52d));
    }

    @Test
    public void defaultAvg() {
        PointValueCollector max = of(Aggregation.DEFAULT_AGGREGATION);
        assertThat(compute(max, 1d, 3d, Double.NaN), equalTo(2d));
        assertThat(compute(max, 2d, Double.NaN, 5d), equalTo(3.5d));
        assertThat(compute(max, Double.NaN, 5d, 1d), equalTo(3d));
        assertThat(compute(max, Double.NaN, 1d, 314d, Double.NaN, 4d, 2d, Double.NaN), equalTo(80.25d));
    }

    private double compute(PointValueCollector collector, double... values) {
        for (double value : values) {
            collector.append(AggrPoint.builder()
                    .time(System.currentTimeMillis())
                    .doubleValue(value)
                    .build());
        }

        AggrPoint temp = new AggrPoint();
        assertThat(collector.compute(temp), equalTo(true));
        collector.reset();
        return temp.getValueDivided();
    }
}
