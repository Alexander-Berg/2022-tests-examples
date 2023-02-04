package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.EnumSet;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.LongValueRandomData;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class LongCollectorsTest {
    @Test
    public void one() {
        EnumSet<Aggregation> source = EnumSet.of(
                Aggregation.DEFAULT_AGGREGATION,
                Aggregation.MAX,
                Aggregation.MIN,
                Aggregation.SUM,
                Aggregation.AVG,
                Aggregation.LAST);

        for (Aggregation aggregation : source) {
            for (MetricType type : ImmutableList.of(MetricType.COUNTER, MetricType.RATE)) {
                PointValueCollector collector = PointValueCollectors.of(type, aggregation);
                long expected = LongValueRandomData.randomLongValue(ThreadLocalRandom.current());
                long result = compute(collector, expected);
                assertEquals(type.name() + ":" + aggregation.name(), expected, result);
            }
        }
    }

    @Test
    public void max() {
        long[] dataset = dataSet();
        LongSummaryStatistics summary = summary(dataset);
        long result = compute(MetricType.COUNTER, Aggregation.MAX, dataset);
        assertEquals(summary.getMax(), result);
    }

    @Test
    public void min() {
        long[] dataset = dataSet();
        LongSummaryStatistics summary = summary(dataset);
        long result = compute(MetricType.COUNTER, Aggregation.MIN, dataset);
        assertEquals(summary.getMin(), result);
    }

    @Test
    public void sum() {
        long[] dataset = dataSet();
        LongSummaryStatistics summary = summary(dataset);
        long result = compute(MetricType.COUNTER, Aggregation.SUM, dataset);
        assertEquals(summary.getSum(), result);
    }

    @Test
    public void count() {
        long[] dataset = dataSet();
        LongSummaryStatistics summary = summary(dataset);
        long result = compute(MetricType.COUNTER, Aggregation.COUNT, dataset);
        assertEquals(summary.getCount(), result);
    }

    @Test
    public void last() {
        long[] dataset = dataSet();
        long result = compute(MetricType.COUNTER, Aggregation.LAST, dataset);
        assertEquals(dataset[dataset.length - 1], result);
    }

    @Test
    public void defaultCounterLast() {
        long[] dataset = dataSet();
        long result = compute(MetricType.COUNTER, Aggregation.DEFAULT_AGGREGATION, dataset);
        assertEquals(dataset[dataset.length - 1], result);
    }

    @Test
    public void defaultRateLast() {
        long[] dataset = dataSet();
        long result = compute(MetricType.RATE, Aggregation.DEFAULT_AGGREGATION, dataset);
        assertEquals(dataset[dataset.length - 1], result);
    }

    @Test
    public void defaultIGaugeAvg() {
        long[] dataset = {10, 20, 30, 10, 100};
        long result = compute(MetricType.IGAUGE, Aggregation.DEFAULT_AGGREGATION, dataset);
        assertEquals(34, result);
    }

    private long[] dataSet() {
        return LongStream.generate(ThreadLocalRandom.current()::nextLong)
                .limit(100)
                .toArray();
    }

    private long compute(MetricType type, Aggregation aggregation, long[] dataset) {
        PointValueCollector collector = PointValueCollectors.of(type, aggregation);
        return compute(collector, dataset);
    }

    private LongSummaryStatistics summary(long[] dataSet) {
        return LongStream.of(dataSet).summaryStatistics();
    }

    private long compute(PointValueCollector collector, long... values) {
        for (long value : values) {
            collector.append(AggrPoint.builder()
                    .time(System.currentTimeMillis())
                    .longValue(value)
                    .build());
        }

        AggrPoint temp = new AggrPoint();
        assertThat(collector.compute(temp), equalTo(true));
        collector.reset();
        return temp.longValue;
    }
}
