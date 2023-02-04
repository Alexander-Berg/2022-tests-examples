package ru.yandex.solomon.math.operation.map;

import java.util.EnumSet;
import java.util.Objects;

import org.junit.Test;

import ru.yandex.solomon.math.operation.Metric;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.math.protobuf.OperationAggregationSummary;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.aggregation.DoubleSummary;
import ru.yandex.solomon.model.timeseries.aggregation.HistogramSummary;
import ru.yandex.solomon.model.timeseries.aggregation.Int64Summary;
import ru.yandex.solomon.model.timeseries.aggregation.SummaryDoubleSummary;
import ru.yandex.solomon.model.timeseries.aggregation.SummaryInt64Summary;
import ru.yandex.solomon.model.timeseries.aggregation.TimeseriesSummary;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.model.point.AggrPoints.summaryDouble;
import static ru.yandex.solomon.model.point.AggrPoints.summaryInt64;

/**
 * @author Vladimir Gordiychuk
 */
public class OperationAggregationSummaryTest {
    @Test
    public void doubleEmptyAggregation() {
        AggrGraphDataArrayList empty = new AggrGraphDataArrayList(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask(), 10);
        DoubleSummary summary = (DoubleSummary) apply(empty, OperationAggregationSummary.newBuilder()
                .addAggregations(Aggregation.LAST)
                .addAggregations(Aggregation.COUNT)
                .build());

        assertThat(summary.getCount(), equalTo(0L));
        assertThat(summary.getLast(), equalTo(0d));
    }

    @Test
    public void doubles() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-06T15:40:01.007Z", 3),
                point("2018-07-06T15:40:02.007Z", 12),
                point("2018-07-06T15:40:03.007Z", 5),
                point("2018-07-06T15:40:03.007Z", 1));

        DoubleSummary summary = (DoubleSummary) apply(source, OperationAggregationSummary.newBuilder()
                .addAggregations(Aggregation.MAX)
                .addAggregations(Aggregation.SUM)
                .addAggregations(Aggregation.LAST)
                .addAggregations(Aggregation.COUNT)
                .build());

        assertThat(summary.getCount(), equalTo(4L));
        assertThat(summary.getMax(), equalTo(12d));
        assertThat(summary.getSum(), equalTo(3d + 12d + 5d + 1d));
        assertThat(summary.getLast(), equalTo(1d));
    }

    @Test
    public void longs() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            lpoint("2018-07-06T15:40:01.007Z", 3),
            lpoint("2018-07-06T15:40:02.007Z", 12),
            lpoint("2018-07-06T15:40:03.007Z", 5),
            lpoint("2018-07-06T15:40:03.007Z", 1)
        );

        Int64Summary summary = (Int64Summary) apply(source, OperationAggregationSummary.newBuilder()
            .addAggregations(Aggregation.MAX)
            .addAggregations(Aggregation.MIN)
            .addAggregations(Aggregation.SUM)
            .addAggregations(Aggregation.LAST)
            .addAggregations(Aggregation.COUNT)
            .addAggregations(Aggregation.AVG)
            .build());

        assertEquals(12, summary.getMax());
        assertEquals(1, summary.getMin());
        assertEquals(3 + 12 + 5 + 1, summary.getSum());
        assertEquals(1, summary.getLast());
        assertEquals(4, summary.getCount());
        assertEquals(21 / 4, summary.getAvg(), 0.0);
    }

    @Test
    public void histograms() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-06T15:40:01.007Z", dhistogram(new double[]{10, 20, 30}, new long[]{12, 0, 0})),
                point("2018-07-06T15:40:02.007Z", dhistogram(new double[]{10, 20, 30}, new long[]{1, 4, 5})),
                point("2018-07-06T15:40:03.007Z", dhistogram(new double[]{10, 20, 30}, new long[]{1, 2, 0})),
                point("2018-07-06T15:40:03.007Z", dhistogram(new double[]{10, 20, 30}, new long[]{0, 0, 2})));

        HistogramSummary summary = (HistogramSummary) apply(source, OperationAggregationSummary.newBuilder()
                .addAggregations(Aggregation.SUM)
                .addAggregations(Aggregation.LAST)
                .addAggregations(Aggregation.COUNT)
                .build());

        assertThat(summary.getCount(), equalTo(4L));
        assertThat(summary.getSum(), equalTo(dhistogram(new double[]{10, 20, 30}, new long[]{14, 6, 7})));
        assertThat(summary.getLast(), equalTo(dhistogram(new double[]{10, 20, 30}, new long[]{0, 0, 2})));
    }

    @Test
    public void summaryDoubles() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-06T15:40:01.007Z", summaryDouble(12, 0, 0)),
                point("2018-07-06T15:40:02.007Z", summaryDouble(1, 4, 5)),
                point("2018-07-06T15:40:03.007Z", summaryDouble(1, 2, 0)),
                point("2018-07-06T15:40:03.007Z", summaryDouble(0, 0, 2)));

        SummaryDoubleSummary summary = (SummaryDoubleSummary) apply(source, OperationAggregationSummary.newBuilder()
                .addAggregations(Aggregation.SUM)
                .addAggregations(Aggregation.LAST)
                .addAggregations(Aggregation.COUNT)
                .build());

        assertThat(summary.getCount(), equalTo(4L));
        assertThat(summary.getSum(), equalTo(summaryDouble(12, 0, 0, 1, 4, 5, 1, 2, 0, 0, 0, 2)));
        assertThat(summary.getLast(), equalTo(summaryDouble(0, 0, 2)));
    }

    @Test
    public void summaryInt64s() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-07-06T15:40:01.007Z", summaryInt64(12, 0, 0)),
                point("2018-07-06T15:40:02.007Z", summaryInt64(1, 4, 5)),
                point("2018-07-06T15:40:03.007Z", summaryInt64(1, 2, 0)),
                point("2018-07-06T15:40:03.007Z", summaryInt64(0, 0, 2)));

        SummaryInt64Summary summary = (SummaryInt64Summary) apply(source, OperationAggregationSummary.newBuilder()
                .addAggregations(Aggregation.SUM)
                .addAggregations(Aggregation.LAST)
                .addAggregations(Aggregation.COUNT)
                .build());

        assertThat(summary.getCount(), equalTo(4L));
        assertThat(summary.getSum(), equalTo(summaryInt64(12, 0, 0, 1, 4, 5, 1, 2, 0, 0, 0, 2)));
        assertThat(summary.getLast(), equalTo(summaryInt64(0, 0, 2)));
    }

    private TimeseriesSummary apply(AggrGraphDataArrayList source, OperationAggregationSummary opts) {
        var op = new ru.yandex.solomon.math.operation.map.OperationAggregationSummary<MetricId>(opts);
        Metric<MetricId>
            result = op.apply(new Metric<>(null, StockpileColumns.typeByMask(source.columnSetMask()), source));

        TimeseriesSummary summary = Objects.requireNonNull(result.getSummary());
        for (Aggregation aggregation : Aggregation.values()) {
            if (EnumSet.of(Aggregation.DEFAULT_AGGREGATION, Aggregation.UNRECOGNIZED).contains(aggregation)) {
                continue;
            }

            assertThat(aggregation.name(), summary.has(aggregation), equalTo(opts.getAggregationsList().contains(aggregation)));
        }

        return summary;
    }
}
