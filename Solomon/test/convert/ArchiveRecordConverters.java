package ru.yandex.stockpile.ser.test.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.salmon.proto.StockpileCanonicalProto;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointData;
import ru.yandex.solomon.model.point.column.HistogramColumn;
import ru.yandex.solomon.model.point.column.LogHistogramColumn;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.point.column.SummaryDoubleColumn;
import ru.yandex.solomon.model.point.column.SummaryInt64Column;
import ru.yandex.solomon.model.timeseries.AggrGraphDataIterable;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

/**
 * @author Vladimir Gordiychuk
 */
public class ArchiveRecordConverters {
    public static List<StockpileCanonicalProto.ArchiveRecord> toProto(AggrGraphDataIterable source) {
        List<StockpileCanonicalProto.ArchiveRecord> result = new ArrayList<>();
        AggrPoint pointData = new AggrPoint();
        StockpileColumnSet columnSet = source.columnSet();
        AggrGraphDataListIterator it = source.iterator();
        while (it.next(pointData)) {
            result.add(toProto(columnSet, pointData));
        }
        return result;
    }

    public static StockpileCanonicalProto.ArchiveRecord toProto(AggrPoint point) {
        return toProto(point.columnSet(), point);
    }

    public static StockpileCanonicalProto.ArchiveRecord toProto(StockpileColumnSet columnSet, AggrPointData pointData) {
        StockpileCanonicalProto.ArchiveRecord.Builder r = StockpileCanonicalProto.ArchiveRecord.newBuilder();
        if (columnSet.hasColumn(StockpileColumn.TS)) {
            r.setTsMillis(pointData.tsMillis);
        }
        if (columnSet.hasColumn(StockpileColumn.VALUE)) {
            r.setSum(pointData.valueNum);
            if (pointData.valueDenom != 0) {
                // TODO: somewhat weird
                r.setSumDenom(pointData.valueDenom);
            }
        }
        if (columnSet.hasColumn(StockpileColumn.MERGE)) {
            r.setMerge(pointData.merge);
        }
        if (columnSet.hasColumn(StockpileColumn.COUNT)) {
            r.setCount(Math.toIntExact(pointData.count));
        }
        if (columnSet.hasColumn(StockpileColumn.STEP)) {
            r.setStepMillis(pointData.stepMillis);
        }
        if (columnSet.hasColumn(StockpileColumn.LOG_HISTOGRAM)) {
            r.setLogHistogram(toProto(pointData.logHistogram));
        }
        if (columnSet.hasColumn(StockpileColumn.HISTOGRAM)) {
            r.setHistogram(toProto(pointData.histogram));
        }
        if (columnSet.hasColumn(StockpileColumn.ISUMMARY)) {
            r.setSummaryInt64(toProto(pointData.summaryInt64));
        }
        if (columnSet.hasColumn(StockpileColumn.DSUMMARY)) {
            r.setSummaryDouble(toProto(pointData.summaryDouble));
        }
        if (columnSet.hasColumn(StockpileColumn.LONG_VALUE)) {
            r.setLongValue(pointData.longValue);
        }
        return r.build();
    }

    public static AggrPoint fromProto(StockpileCanonicalProto.ArchiveRecord archiveRecord) {
        AggrPoint r = new AggrPoint();
        r.tsMillis = archiveRecord.getTsMillis();
        r.valueNum = archiveRecord.getSum();
        r.valueDenom = archiveRecord.getSumDenom();
        r.merge = archiveRecord.getMerge();
        r.count = archiveRecord.getCount();
        r.stepMillis = archiveRecord.getStepMillis();
        r.logHistogram = fromProto(archiveRecord.getLogHistogram());
        r.histogram = fromProto(archiveRecord.getHistogram());
        r.summaryInt64 = fromProto(archiveRecord.getSummaryInt64());
        r.summaryDouble = fromProto(archiveRecord.getSummaryDouble());
        r.longValue = archiveRecord.getLongValue();
        return r;
    }

    private static StockpileCanonicalProto.LogHistogram toProto(LogHistogram logHistogram) {
        StockpileCanonicalProto.LogHistogram.Builder builder = StockpileCanonicalProto.LogHistogram.newBuilder()
                .setCountZero(logHistogram.getCountZero())
                .setMaxBucketsSize(logHistogram.getMaxBucketsSize())
                .setBase(logHistogram.getBase())
                .setStartPower(logHistogram.getStartPower());

        for (int index = 0; index < logHistogram.countBucket(); index++) {
            builder.addBuckets(logHistogram.getBucketValue(index));
        }

        return builder.build();
    }

    private static StockpileCanonicalProto.Histogram toProto(Histogram histogram) {
        StockpileCanonicalProto.Histogram.Builder builder = StockpileCanonicalProto.Histogram.newBuilder();
        for (int index = 0; index < histogram.count(); index++) {
            builder.addBounds(histogram.upperBound(index));
            builder.addBuckets(histogram.value(index));
        }
        return builder.build();
    }

    private static StockpileCanonicalProto.SummaryInt64 toProto(SummaryInt64Snapshot summary) {
        return StockpileCanonicalProto.SummaryInt64.newBuilder()
                .setCount(summary.getCount())
                .setSum(summary.getSum())
                .setMin(summary.getMin())
                .setMax(summary.getMax())
                .setLast(summary.getLast())
                .build();
    }

    private static StockpileCanonicalProto.SummaryDouble toProto(SummaryDoubleSnapshot summary) {
        return StockpileCanonicalProto.SummaryDouble.newBuilder()
                .setCount(summary.getCount())
                .setSum(summary.getSum())
                .setMin(summary.getMin())
                .setMax(summary.getMax())
                .setLast(summary.getLast())
                .build();
    }

    private static LogHistogram fromProto(StockpileCanonicalProto.LogHistogram proto) {
        if (Objects.equals(proto, StockpileCanonicalProto.LogHistogram.getDefaultInstance())) {
            return LogHistogramColumn.DEFAULT_VALUE;
        }

        return LogHistogram.newBuilder()
                .setStartPower(proto.getStartPower())
                .setCountZero(proto.getCountZero())
                .setMaxBucketsSize(proto.getMaxBucketsSize())
                .setBase(proto.getBase())
                .setBuckets(proto.getBucketsList().stream()
                        .mapToDouble(Double::doubleValue)
                        .toArray())
                .build();
    }

    private static Histogram fromProto(StockpileCanonicalProto.Histogram proto) {
        if (Objects.equals(proto, StockpileCanonicalProto.Histogram.getDefaultInstance())) {
            return HistogramColumn.DEFAULT_VALUE;
        }

        int size = proto.getBoundsCount();
        var hist = Histogram.newInstance();

        for (int index = 0; index < size; index++) {
            hist.setUpperBound(index, proto.getBounds(index));
            hist.setBucketValue(index, proto.getBuckets(index));
        }

        return hist;
    }

    private static SummaryInt64Snapshot fromProto(StockpileCanonicalProto.SummaryInt64 proto) {
        if (Objects.equals(proto, StockpileCanonicalProto.SummaryInt64.getDefaultInstance())) {
            return SummaryInt64Column.DEFAULT_VALUE;
        }

        return new ImmutableSummaryInt64Snapshot(proto.getCount(), proto.getSum(), proto.getMin(), proto.getMax(), proto.getLast());
    }

    private static SummaryDoubleSnapshot fromProto(StockpileCanonicalProto.SummaryDouble proto) {
        if (Objects.equals(proto, StockpileCanonicalProto.SummaryDouble.getDefaultInstance())) {
            return SummaryDoubleColumn.DEFAULT_VALUE;
        }

        return new ImmutableSummaryDoubleSnapshot(proto.getCount(), proto.getSum(), proto.getMin(), proto.getMax(), proto.getLast());
    }
}
