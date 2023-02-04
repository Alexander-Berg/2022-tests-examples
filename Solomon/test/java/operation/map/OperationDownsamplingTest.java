package ru.yandex.solomon.math.operation.map;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.solomon.math.operation.Metric;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.math.protobuf.OperationDownsampling;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.HistogramColumn;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataIterable;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;
import ru.yandex.solomon.util.collection.array.LongArrayView;
import ru.yandex.solomon.util.time.Interval;
import ru.yandex.stockpile.api.EProjectId;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;
import static ru.yandex.solomon.model.point.AggrPoints.dpoint;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.model.point.AggrPoints.summaryDouble;
import static ru.yandex.solomon.model.point.AggrPoints.summaryInt64;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.bucket;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.ugram;

/**
 * @author Vladimir Gordiychuk
 */
public class OperationDownsamplingTest {

    private static Interval interval(String begin, String end) {
        return Interval.millis(ts(begin), ts(end));
    }

    @Test
    public void skipDownsamplingWithoutGrid() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), 1),
                point(ts("09:00:15"), 2),
                point(ts("09:00:30"), 3),
                point(ts("09:00:35"), 4));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.getDefaultInstance());
        assertThat(result, equalTo(source));
    }

    @Test
    public void skipEmptyDownsampling() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.empty();

        Interval interval = interval("09:00:00", "10:00:00");
        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setGridMillis(TimeUnit.MINUTES.toMillis(5))
                .build());
        assertThat(result, equalTo(source));
    }

    @Test
    public void tooManyGaps() {
        long lastPointMillis = Instant.parse("2019-06-21T00:00:00Z").toEpochMilli();
        long gridMillis = TimeUnit.SECONDS.toMillis(30);

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(AggrPoint.shortPoint(lastPointMillis, 1));

        Interval interval = Interval.millis(
            Instant.parse("2019-06-20T15:40:00Z").toEpochMilli(),
            Instant.parse("2019-06-20T19:29:59Z").toEpochMilli()
        );

        List<AggrPoint> points = new ArrayList<>();
        // Note that lastPointMillis is specially aligned to gridMillis for ease
        for (long tsMillis = interval.getBeginMillis(); tsMillis <= lastPointMillis; tsMillis += gridMillis) {
            AggrPoint point;
            if (tsMillis == lastPointMillis) {
                point = AggrPoint.builder()
                    .time(tsMillis)
                    .doubleValue(1)
                    .count(1)
                    .build();
            } else {
                point = AggrPoint.shortPoint(tsMillis, Double.NaN);
            }
            points.add(point);
        }

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(points.toArray(AggrPoint[]::new));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setGridMillis(gridMillis)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void downsamplingByDoubleLast() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), 1),
                point(ts("09:00:15"), 2),
                point(ts("09:01:44"), 3),
                point(ts("09:01:50"), 2),
                point(ts("09:02:32"), 3),
                point(ts("09:02:44"), 6),
                point(ts("09:02:50"), 5));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), 2, 2),
                point(ts("09:01:00"), 2 ,2),
                point(ts("09:02:00"), 5, 3));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .build());
        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsDefault() {
        Interval interval = interval("08:59:00", "09:03:00");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // gap here
                point(ts("09:00:00"), 1),
                point(ts("09:00:15"), 2),
                // gap here
                point(ts("09:03:32"), 5),
                point(ts("09:03:44"), 6));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("08:59:00"), Double.NaN, 0),
                point(ts("09:00:00"), 2, 2),
                point(ts("09:01:00"), Double.NaN, 0),
                point(ts("09:02:00"), Double.NaN, 0),
                point(ts("09:03:00"), 6, 2));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsNull() {
        Interval interval = interval("08:59:00", "09:03:00");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // gap here
                point(ts("09:00:00"), 1),
                point(ts("09:00:15"), 2),
                // gap here
                point(ts("09:03:32"), 5),
                point(ts("09:03:44"), 6));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("08:59:00"), Double.NaN, 0),
                point(ts("09:00:00"), 2, 2),
                point(ts("09:01:00"), Double.NaN, 0),
                point(ts("09:02:00"), Double.NaN, 0),
                point(ts("09:03:00"), 6, 2));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.NULL)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsNone() {
        Interval interval = interval("08:59:00", "09:03:00");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // gap here
                point(ts("09:00:00"), 1),
                point(ts("09:00:15"), 2),
                // gap here
                point(ts("09:03:32"), 5),
                point(ts("09:03:44"), 6));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), 2, 2),
                point(ts("09:03:00"), 6, 2));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.NONE)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsLast() {
        Interval interval = interval("08:59:00", "09:03:00");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // gap here
                point(ts("09:00:00"), 1),
                point(ts("09:00:15"), 2),
                // gap here
                point(ts("09:03:32"), 5),
                point(ts("09:03:44"), 6));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("08:59:00"), Double.NaN, 0),
                point(ts("09:00:00"), 2, 2),
                point(ts("09:01:00"), 2, 0),
                point(ts("09:02:00"), 2, 0),
                point(ts("09:03:00"), 6, 2));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.PREVIOUS)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsConsiderStepMillis() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                AggrPoint.builder().time(ts("09:00:00")).doubleValue(1).stepMillis(30_000).build(),
                AggrPoint.builder().time(ts("09:00:30")).doubleValue(2).stepMillis(30_000).build(),
                AggrPoint.builder().time(ts("09:01:00")).doubleValue(3).stepMillis(30_000).build(),
                AggrPoint.builder().time(ts("09:03:00")).doubleValue(7).stepMillis(30_000).build()
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), 1, 1),
                point(ts("09:00:30"), 2, 1),
                point(ts("09:01:00"), 3, 1),
                point(ts("09:01:30"), Double.NaN, 0),
                point(ts("09:02:00"), Double.NaN, 0),
                point(ts("09:02:30"), Double.NaN, 0),
                point(ts("09:03:00"), 7, 1));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.SECONDS.toMillis(15)) // grid millis less than step millis
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsWithoutStepMillisLogic() {
        long gridMillis = 15_000;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            AggrPoint.builder().time(ts("09:00:00")).doubleValue(1).stepMillis(30_000).build(),
            AggrPoint.builder().time(ts("09:00:30")).doubleValue(2).stepMillis(30_000).build(),
            AggrPoint.builder().time(ts("09:01:00")).doubleValue(3).stepMillis(30_000).build(),
            AggrPoint.builder().time(ts("09:03:00")).doubleValue(7).stepMillis(30_000).build()
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point(ts("09:00:00"), 1, 1),
            point(ts("09:00:15"), Double.NaN, 0),
            point(ts("09:00:30"), 2, 1),
            point(ts("09:00:45"), Double.NaN, 0),
            point(ts("09:01:00"), 3, 1),
            point(ts("09:01:15"), Double.NaN, 0),
            point(ts("09:01:30"), Double.NaN, 0),
            point(ts("09:01:45"), Double.NaN, 0),
            point(ts("09:02:00"), Double.NaN, 0),
            point(ts("09:02:15"), Double.NaN, 0),
            point(ts("09:02:30"), Double.NaN, 0),
            point(ts("09:02:45"), Double.NaN, 0),
            point(ts("09:03:00"), 7, 1));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.LAST)
            .setGridMillis(gridMillis)
            .setFillOption(OperationDownsampling.FillOption.NULL)
            .setIgnoreMinStepMillis(true)
            .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void fillGapsFartherThanStepMillisDistance() {
        int stepMillis = 15000;
        long gridMillis = 3600;

        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            AggrPoint.builder().time(ts("00:00:00")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:00:15")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:00:30")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:00:45")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:01:00")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:01:15")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:01:30")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:01:45")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:02:00")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:02:30")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:02:45")).doubleValue(10).stepMillis(stepMillis).build(),
            AggrPoint.builder().time(ts("00:03:00")).doubleValue(10).stepMillis(stepMillis).build()
        );

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point(ts("00:00:00.000"), 10, 1),
            point(ts("00:00:14.400"), 10, 1),
            point(ts("00:00:28.800"), 10, 1),
            point(ts("00:00:43.200"), 10, 1),
            point(ts("00:00:57.600"), 10, 1),
            point(ts("00:01:12.000"), 10, 1),
            point(ts("00:01:30.000"), 10, 1),
            point(ts("00:01:44.400"), 10, 1),
            point(ts("00:01:58.800"), 10, 1),
            point(ts("00:02:13.200"), Double.NaN, 0),
            point(ts("00:02:27.600"), 10, 1),
            point(ts("00:02:42.000"), 10, 1),
            point(ts("00:03:00.000"), 10, 1)
        );

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.LAST)
            .setGridMillis(gridMillis)
            .setFillOption(OperationDownsampling.FillOption.NULL)
            .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void lastHistogram() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), dhistogram(new double[]{10, 20, 30}, new long[]{1, 0, 5})),
                point(ts("09:00:15"), dhistogram(new double[]{10, 20, 30}, new long[]{2, 1, 3})),
                // gap here
                point(ts("09:03:32"), dhistogram(new double[]{10, 20, 30}, new long[]{1, 2, 3})),
                point(ts("09:03:44"), dhistogram(new double[]{10, 20, 30}, new long[]{5, 2, 1})));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), dhistogram(new double[]{10, 20, 30}, new long[]{2, 1, 3}), 2),
                point(ts("09:03:00"), dhistogram(new double[]{10, 20, 30}, new long[]{5, 2, 1}), 2));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.NONE)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void lastLogHistogram() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), LogHistogram.ofBuckets(1, 2, 3)),
                point(ts("09:00:15"), LogHistogram.ofBuckets(3, 2, 1)),
                // gap here
                point(ts("09:03:32"), LogHistogram.ofBuckets(3, 2, 1, 4)),
                point(ts("09:03:44"), LogHistogram.ofBuckets(1, 1, 2, 3)));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), LogHistogram.ofBuckets(3, 2, 1), 2),
                point(ts("09:03:00"), LogHistogram.ofBuckets(1, 1, 2, 3), 2));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.NONE)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void lastSummaryDouble() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), summaryDouble(1, 2, 3)),
                point(ts("09:00:15"), summaryDouble(3, 2, 1)),
                // gap here
                point(ts("09:03:32"), summaryDouble(3, 2, 1, 4)),
                point(ts("09:03:44"), summaryDouble(1, 1, 2, 3)));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), summaryDouble(3, 2, 1), 2),
                point(ts("09:03:00"), summaryDouble(1, 1, 2, 3), 2));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.NONE)
                .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void lastSummaryInt64() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), summaryInt64(1, 2, 3)),
                point(ts("09:00:15"), summaryInt64(3, 2, 1)),
                // gap here
                point(ts("09:03:32"), summaryInt64(3, 2, 1, 4)),
                point(ts("09:03:44"), summaryInt64(1, 1, 2, 3)));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("09:00:00"), summaryInt64(3, 2, 1), 2),
                point(ts("09:03:00"), summaryInt64(1, 1, 2, 3), 2));

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.LAST)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.NONE)
                .build());

        assertEquals(expected, result);
    }

    @Test
    public void downsamplingAggregateCountOfCount() {
        Interval interval = interval("08:59:00", "09:03:00");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                // gap here
                point(ts("09:00:00"), 1, 5),
                point(ts("09:00:15"), 2, 3),
                // gap here
                point(ts("09:03:32"), 5, 1),
                point(ts("09:03:44"), 6, 2));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(ts("08:59:00"), Double.NaN, 0),
                point(ts("09:00:00"), 1 + 2, 5 + 3),
                point(ts("09:01:00"), 1 + 2, 0),
                point(ts("09:02:00"), 1 + 2, 0),
                point(ts("09:03:00"), 5 + 6, 1 + 2));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.SUM)
                .setGridMillis(TimeUnit.MINUTES.toMillis(1))
                .setFillOption(OperationDownsampling.FillOption.PREVIOUS)
                .build());

        assertEquals(expected, result);
    }

    @Test
    public void downsamplingConsiderStepMillis() {
        Interval interval = interval("00:00:00", "00:10:00");

        long step1m = TimeUnit.MINUTES.toMillis(1);
        long step30s = TimeUnit.SECONDS.toMillis(30);
        long step15s = TimeUnit.SECONDS.toMillis(15);
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                dpoint(ts("00:00:00"), 1, step1m),
                dpoint(ts("00:01:00"), 2, step1m),
                dpoint(ts("00:02:00"), 3, step1m),
                dpoint(ts("00:03:00"), 4, step30s),
                dpoint(ts("00:03:30"), 5, step30s),
                dpoint(ts("00:04:00"), 6, step30s),
                dpoint(ts("00:04:30"), 7, step15s),
                dpoint(ts("00:04:45"), 8, step15s),
                // gap
                dpoint(ts("00:05:15"), 10, step15s),
                dpoint(ts("00:05:30"), 11, step15s));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                dpoint(ts("00:00:00"), 1),
                dpoint(ts("00:01:00"), 2),
                dpoint(ts("00:02:00"), 3),
                dpoint(ts("00:03:00"), 4),
                dpoint(ts("00:03:30"), 5),
                dpoint(ts("00:04:00"), 6),
                dpoint(ts("00:04:30"), 7 + 8),
                dpoint(ts("00:05:00"), 10),
                dpoint(ts("00:05:30"), 11));

        AggrGraphDataArrayList result = apply(source, interval, OperationDownsampling.newBuilder()
                .setAggregation(Aggregation.SUM)
                .setGridMillis(step30s)
                .setFillOption(OperationDownsampling.FillOption.NULL)
                .build());

        assertNotNull(result);
        result = result.cloneWithMask(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask());
        result = AggrGraphDataArrayList.of(result.slice(0, expected.length()));
        assertEquals(expected, result);
    }

    @Test
    public void downsamplingAggregateCountOverflow() {
        int points = 10;
        long count = Integer.MAX_VALUE / points + 100;

        AggrGraphDataArrayList source = new AggrGraphDataArrayList();
        long expectedSum = 0;
        long ts0 = ts("00:00:00");
        for(int index = 0; index < points; index++) {
            expectedSum += index;
            source.addRecord(dpoint(ts0 + (index * 10_000), index, true, count));
        }

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.SUM)
            .setGridMillis(TimeUnit.HOURS.toMillis(1))
            .setFillOption(OperationDownsampling.FillOption.NULL)
            .build());

        long expectedCount = count * points;
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(dpoint(ts0, expectedSum, false, expectedCount));

        assertNotNull(result);
        result.ensureCapacity(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.MERGE.mask() | StockpileColumn.COUNT.mask(), 100);
        assertEquals(expected, result);
    }

    @Test
    public void sumDownsamplingIGauge() {
        int points = 10;

        AggrGraphDataArrayList source = new AggrGraphDataArrayList();
        long expectedSum = 0;
        long ts0 = ts("00:00:00");
        for (int index = 0; index < points; index++) {
            expectedSum += index;
            source.addRecord(lpoint(ts0 + (index * 10_000), index, true, 64));
        }

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.SUM)
            .setGridMillis(TimeUnit.HOURS.toMillis(1))
            .setFillOption(OperationDownsampling.FillOption.NULL)
            .build());
        result.ensureCapacity(
            StockpileColumn.TS.mask()
                | StockpileColumn.LONG_VALUE.mask()
                | StockpileColumn.MERGE.mask()
                | StockpileColumn.COUNT.mask(),
            1);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(lpoint(ts0, expectedSum, false, 64 * points));
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    public void avgDownsamplingIGauge() {
        int points = 10;

        AggrGraphDataArrayList source = new AggrGraphDataArrayList();
        long sum = 0;
        long ts0 = ts("00:00:00");
        for (int index = 0; index < points; index++) {
            sum += index;
            source.addRecord(lpoint(ts0 + (index * 10_000), index, true, 64));
        }

        AggrGraphDataArrayList result = apply(source, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.AVG)
            .setGridMillis(TimeUnit.HOURS.toMillis(1))
            .setFillOption(OperationDownsampling.FillOption.NULL)
            .build());
        result.ensureCapacity(
            StockpileColumn.TS.mask()
                | StockpileColumn.LONG_VALUE.mask()
                | StockpileColumn.MERGE.mask()
                | StockpileColumn.COUNT.mask(),
            1);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(lpoint(ts0, sum / points, false, 64 * points));
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    public void applyDownsamplingAsUgram() {
        Interval interval = interval("08:59:00", "09:03:00");
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            // gap here
            point(ts("09:00:00"),
                ugram(
                    bucket(10, 20, 2),
                    bucket(20, 50, 6),
                    bucket(50, 60, 1))),
            point(ts("09:00:15"),
                ugram(
                    bucket(5, 10, 1),
                    bucket(10, 15, 3),
                    bucket(15, 30, 3),
                    bucket(30, 60, 3)
                )),
            // gap here
            point(ts("09:03:32"),
                ugram(
                    bucket(10.0, 20.0, 2.0),
                    bucket(20.0, 30.0, 6.0),
                    bucket(30.0, 50.0, 12.0),
                    bucket(50.0, 50.0, 1.0))),
            point(ts("09:03:44"),
                ugram(
                    bucket(10.0, 30.0, 8.0),
                    bucket(30.0, 40.0, 3.0),
                    bucket(40.0, 45.0, 5.0),
                    bucket(45.0, 50.0, 10.0))));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point(ts("08:59:00"), HistogramColumn.DEFAULT_VALUE, 0),
            point(ts("09:00:00"),
                ugram(
                    bucket(5, 10, 0.0 + 1.0),
                    bucket(10, 15, 1.0 + 3.0),
                    bucket(15, 20, 1.0 + 1.0),
                    bucket(20, 30, 2.0 + 2.0),
                    bucket(30, 50, 4.0 + 2.0),
                    bucket(50, 60, 1.0 + 1.0)),
                2),
            point(ts("09:01:00"), Histogram.newInstance(), 0),
            point(ts("09:02:00"), Histogram.newInstance(), 0),
            point(ts("09:03:00"),
                ugram(
                    bucket(10.0, 20.0, 2.0 + 8.0 / 2.0),
                    bucket(20.0, 30.0, 6.0 + 8.0 / 2.0),
                    bucket(30.0, 40.0, 12.0 / 2.0 + 3.0),
                    bucket(40.0, 45.0, 12.0 / 4.0 + 5.0),
                    bucket(45.0, 50.0, 12.0 / 4.0 + 10.0),
                    bucket(50.0, 50.0, 1.0 + 0.0)),
                2));

        AggrGraphDataArrayList result = apply(EProjectId.GOLOVAN, source, interval, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.SUM)
            .setGridMillis(TimeUnit.MINUTES.toMillis(1))
            .build());

        assertThat(result, equalTo(expected));
    }

    @Test
    public void stopDownsampling() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point(ts("09:00:00"), 1),
            point(ts("09:00:15"), 2),
            point(ts("09:01:44"), 3),
            point(ts("10:00:00"), 4));

        AggrGraphDataIterable iterable = new AggrGraphDataIterable() {
            @Override
            public int getRecordCount() {
                return 100;
            }

            @Override
            public int elapsedBytes() {
                return 1024;
            }

            @Override
            public AggrGraphDataListIterator iterator() {
                return new AggrGraphDataListIterator(source.columnSetMask()) {
                    @Override
                    public boolean next(@Nonnull AggrPoint target) {
                        source.getDataTo(source.length() - 1, target);
                        return false;
                    }
                };
            }

            @Override
            public int columnSetMask() {
                return source.columnSetMask();
            }
        };

        var interval = interval("08:50:00", "08:55:00");
        var result = apply(EProjectId.SOLOMON, iterable, interval, OperationDownsampling.newBuilder()
            .setAggregation(Aggregation.LAST)
            .setGridMillis(TimeUnit.SECONDS.toMillis(1))
            .build());

        assertNotNull(result);

        var it = result.iterator();
        AggrPoint point = new AggrPoint();
        assertFalse(it.next(point));
        assertFalse(it.next(point));
        assertFalse(it.next(point));
    }

    private static long ts(String time) {
        return LocalDateTime.of(LocalDate.of(2000, Month.JANUARY, 1), LocalTime.parse(time))
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    private AggrGraphDataArrayList apply(AggrGraphDataArrayList source, OperationDownsampling opts) {
        LongArrayView timestamps = source.getTimestamps();
        Interval interval = Interval.millis(timestamps.first(), timestamps.last());
        return apply(source, interval, opts);
    }

    private AggrGraphDataArrayList apply(AggrGraphDataArrayList source, Interval interval, OperationDownsampling opts) {
        return apply(EProjectId.UNKNOWN, source, interval, opts);
    }

    private AggrGraphDataArrayList apply(EProjectId ownerId, AggrGraphDataArrayList source, Interval interval, OperationDownsampling opts) {
        var action = new ru.yandex.solomon.math.operation.map.OperationDownsampling<>(interval, opts);
        Metric result = action.apply(new Metric<>(null, StockpileColumns.typeByMask(source.columnSetMask()), ownerId, source));
        if (result.getTimeseries() == null) {
            return null;
        }

        return AggrGraphDataArrayList.of(result.getTimeseries().iterator());
    }

    private AggrGraphDataIterable apply(EProjectId ownerId, AggrGraphDataIterable source, Interval interval, OperationDownsampling opts) {
        var action = new ru.yandex.solomon.math.operation.map.OperationDownsampling<>(interval, opts);
        Metric result = action.apply(new Metric<>(null, StockpileColumns.typeByMask(source.columnSetMask()), ownerId, source));
        if (result.getTimeseries() == null) {
            return null;
        }

        return result.getTimeseries();
    }
}
