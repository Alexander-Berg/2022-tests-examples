package ru.yandex.solomon.model.timeseries.aggregation;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.solomon.model.type.Histogram;

import static ru.yandex.solomon.model.timeseries.aggregation.AggregateConverters.fromProto;
import static ru.yandex.solomon.model.timeseries.aggregation.AggregateConverters.toProto;

/**
 * @author Vladimir Gordiychuk
 */
public class AggregateConvertersTest {

    @Test
    public void summaryDouble() {
        DoubleSummary summary = new DoubleSummary();
        summary.setCount(10);
        summary.setLast(42);
        summary.setMax(50);

        DoubleSummary result = (DoubleSummary) fromProto(toProto(summary));
        Assert.assertEquals(summary, result);
    }

    @Test
    public void summaryIn64() {
        Int64Summary summary = new Int64Summary();
        summary.setCount(10);
        summary.setLast(42);
        summary.setMax(50);

        Int64Summary result = (Int64Summary) fromProto(toProto(summary));
        Assert.assertEquals(summary, result);
    }

    @Test
    public void summaryHistogram() {
        HistogramSummary summary = new HistogramSummary();
        summary.setCount(10);
        summary.setLast(Histogram.newInstance(new double[]{10, 20, 30}, new long[]{1, 0, 5}));

        HistogramSummary result = (HistogramSummary) fromProto(toProto(summary));
        Assert.assertEquals(summary, result);
    }

    @Test
    public void summarySummaryDouble() {
        SummaryDoubleSummary summary = new SummaryDoubleSummary();
        summary.setCount(10);
        summary.setSum(new ImmutableSummaryDoubleSnapshot(4, 42, 2, 10));

        SummaryDoubleSummary result = (SummaryDoubleSummary) fromProto(toProto(summary));
        Assert.assertEquals(summary, result);
    }

    @Test
    public void summarySummaryInt64() {
        SummaryInt64Summary summary = new SummaryInt64Summary();
        summary.setCount(10);
        summary.setLast(new ImmutableSummaryInt64Snapshot(4, 42, 2, 10));

        SummaryInt64Summary result = (SummaryInt64Summary) fromProto(toProto(summary));
        Assert.assertEquals(summary, result);
    }
}
