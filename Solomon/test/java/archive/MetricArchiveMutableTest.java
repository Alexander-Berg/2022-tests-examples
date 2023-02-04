package ru.yandex.solomon.codec.archive;

import java.time.Instant;

import io.netty.buffer.ByteBufAllocator;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.codec.archive.header.MetricHeader;
import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.NettyBitBuf;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.CountColumn;
import ru.yandex.solomon.model.point.column.HistogramColumn;
import ru.yandex.solomon.model.point.column.LongValueColumn;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Maksim Leonov
 */
public class MetricArchiveMutableTest {
    @Test
    public void testGraphDataWrongOrder() {
        long TIME_1 = System.currentTimeMillis();
        long TIME_2 = TIME_1 + 15_000;

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.addRecord(point(TIME_1, 1, true, 1));
        archive.addRecord(point(TIME_2, 1, true, 1));
        archive.addRecord(point(TIME_1, 1, true, 1));


        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point(TIME_1, 2, true, 2),
                point(TIME_2, 1, true, 1)
        );

        assertEquals(expected, archive.toAggrGraphDataArrayList());
        close(archive);
    }

    @Test
    public void immutableNotEqualWithDifferentLogHistogram() throws Exception {
        MetricArchiveMutable first = new MetricArchiveMutable();
        first.addRecord(point("2017-05-17T16:01:00Z", LogHistogram.ofBuckets(3, 4, 5)));
        first.addRecord(point("2017-05-17T16:00:00Z", LogHistogram.ofBuckets(3, 3, 3)));
        first.addRecord(point("2017-05-17T16:03:00Z", LogHistogram.ofBuckets(1, 2, 3)));

        MetricArchiveMutable second = new MetricArchiveMutable();
        first.addRecord(point("2017-05-17T16:01:00Z", LogHistogram.ofBuckets(1, 1, 1)));
        first.addRecord(point("2017-05-17T16:00:00Z", LogHistogram.ofBuckets(3, 3, 3)));
        first.addRecord(point("2017-05-17T16:03:00Z", LogHistogram.ofBuckets(2, 2, 2)));

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
        close(first, second);
    }

    @Test
    public void immutableArchiveEqualWithSameLogHistogramContent() throws Exception {
        MetricArchiveMutable mutable = new MetricArchiveMutable();
        LogHistogram.newBuilder()
            .setBuckets(new double[]{3, 4, 5})
            .setCountZero(5)
            .setStartPower(-1)
            .build();

        mutable.addRecord(point(
            "2017-05-17T16:01:00Z",
            LogHistogram.newBuilder()
                .setBuckets(new double[]{3, 4, 5})
                .setCountZero(5)
                .setStartPower(-1)
                .build()
            )
        );

        mutable.addRecord(point(
            "2017-05-17T16:00:00Z",
            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 0, 5})
                .setCountZero(10)
                .setStartPower(-1)
                .build()
            )
        );

        mutable.addRecord(point(
            "2017-05-17T16:02:00Z",
            LogHistogram.newBuilder()
                .setBuckets(new double[]{1, 2, 3, 4, 5, 6, 7})
                .setCountZero(10)
                .setStartPower(5)
                .build()
            )
        );

        var immutable = mutable.toImmutable();
        var immutableNoCopy = mutable.toImmutableNoCopy();
        assertEquals(immutable, immutableNoCopy);
        close(mutable, immutable, immutableNoCopy);
    }

    @Test
    public void cloneToUnsealedWhenUnsealed() {
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.addRecord(point("2018-01-18T13:00:00Z", 0));
        source.addRecord(point("2018-01-18T13:01:00Z", 1));
        source.addRecord(point("2018-01-18T13:02:00Z", 2));

        MetricArchiveMutable copy = source.cloneToUnsealed();
        source.addRecord(point("2018-01-18T13:03:00Z", 3));

        copy.addRecord(point("2018-01-18T13:01:30Z", 1.5));
        copy.addRecord(point("2018-01-18T13:02:30Z", 2.5));


        Assert.assertThat("Changed in cloned archive should not affect source archive",
                source.toAggrGraphDataArrayList(),
                Matchers.equalTo(AggrGraphDataArrayList.of(
                        point("2018-01-18T13:00:00Z", 0),
                        point("2018-01-18T13:01:00Z", 1),
                        point("2018-01-18T13:02:00Z", 2),
                        point("2018-01-18T13:03:00Z", 3)
                )));

        Assert.assertThat("Changes in source archive after copy should not affect copied archive",
                copy.toAggrGraphDataArrayList(),
                Matchers.equalTo(AggrGraphDataArrayList.of(
                        point("2018-01-18T13:00:00Z", 0),
                        point("2018-01-18T13:01:00Z", 1),
                        point("2018-01-18T13:01:30Z", 1.5),
                        point("2018-01-18T13:02:00Z", 2),
                        point("2018-01-18T13:02:30Z", 2.5)
                )));
        close(source, copy);
    }

    @Test
    public void getLatestTsMillisOnSorted() {
        MetricArchiveMutable content = new MetricArchiveMutable();
        content.addRecord(point("2018-01-18T13:00:00Z", 0));
        content.addRecord(point("2018-01-18T13:01:00Z", 1));
        content.addRecord(point("2018-01-18T13:02:00Z", 2));

        Assert.assertThat(content.getLastTsMillis(), equalTo(timiToMillis("2018-01-18T13:02:00Z")));
        close(content);
    }

    @Test
    public void getLatestTsMillisOnUnsorted() {
        MetricArchiveMutable content = new MetricArchiveMutable();
        content.addRecord(point("2018-01-18T13:00:00Z", 0));
        content.addRecord(point("2018-01-18T13:02:00Z", 2));
        content.addRecord(point("2018-01-18T13:01:00Z", 1));

        assertThat(content.getLastTsMillis(), equalTo(timiToMillis("2018-01-18T13:02:00Z")));
        close(content);
    }

    @Test
    public void toImmutableNoCopySortIfNeed() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-01-18T13:00:00Z", 0),
                point("2018-01-18T13:02:00Z", 2),
                point("2018-01-18T13:01:00Z", 1));

        MetricArchiveMutable archive = MetricArchiveMutable.of(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-01-18T13:00:00Z", 0),
                point("2018-01-18T13:01:00Z", 1),
                point("2018-01-18T13:02:00Z", 2));

        var immutableNoCopy = archive.toImmutableNoCopy();
        AggrGraphDataArrayList result = immutableNoCopy.toAggrGraphDataArrayList();
        assertThat(result, equalTo(expected));
        close(archive, immutableNoCopy);
    }

    @Test
    public void toImmutableNoCopyDropDuplicatesIfNeed() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2018-01-18T13:00:00Z", 0),
                point("2018-01-18T13:01:00Z", 1),
                point("2018-01-18T13:01:00Z", 2));

        MetricArchiveMutable content = MetricArchiveMutable.of(source);

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-01-18T13:00:00Z", 0),
                point("2018-01-18T13:01:00Z", 2));

        var immutable = content.toImmutableNoCopy();
        AggrGraphDataArrayList result = immutable.toAggrGraphDataArrayList();
        assertThat(result, equalTo(expected));
        close(content, immutable);
    }

    @Test
    public void actualizeKindByMaskDGauge() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        assertThat(archive.getType(), equalTo(MetricType.METRIC_TYPE_UNSPECIFIED));
        archive.addRecord(point("2018-01-18T13:00:00Z", 42));
        assertThat(archive.getType(), equalTo(MetricType.DGAUGE));
        close(archive);
    }

    @Test
    public void actualizeKindByMaskHist() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        assertThat(archive.getType(), equalTo(MetricType.METRIC_TYPE_UNSPECIFIED));
        archive.addRecord(point("2018-01-18T13:00:00Z", dhistogram(new double[]{100, 200, 300}, new long[]{3, 2, 0})));
        assertThat(archive.getType(), equalTo(MetricType.HIST));
        close(archive);
    }

    @Test
    public void kindChangeFromDGaugeToRate() {
        MetricArchiveMutable archive = new MetricArchiveMutable(MetricHeader.defaultValue, StockpileFormat.CURRENT);
        archive.setType(MetricType.DGAUGE);
        archive.addRecord(point("2018-01-18T13:00:00Z", 70d));
        archive.addRecord(point("2018-01-18T13:00:10Z", 20d));
        archive.addRecord(point("2018-01-18T13:00:20Z", 50d));
        archive.addRecord(point("2018-01-18T13:00:30Z", 25d));
        assertEquals(StockpileColumns.minColumnSet(MetricType.DGAUGE), archive.columnSetMask());

        archive.setType(MetricType.RATE);
        assertEquals("Change metric kind also include change mask",
                StockpileColumns.minColumnSet(MetricType.RATE), archive.columnSetMask());

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-01-18T13:00:00Z", 700),
                lpoint("2018-01-18T13:00:10Z", 900),
                lpoint("2018-01-18T13:00:20Z", 1400),
                lpoint("2018-01-18T13:00:30Z", 1650));

        AggrGraphDataArrayList result = archive.toAggrGraphDataArrayList();
        assertEquals(expected, result);
        close(archive);
    }

    @Test
    public void kindChangeFromRateToDGauge() {
        MetricArchiveMutable archive = new MetricArchiveMutable(MetricHeader.defaultValue, StockpileFormat.CURRENT);
        archive.setType(MetricType.RATE);
        archive.addRecord(lpoint("2018-01-18T13:00:00Z", 100L));
        archive.addRecord(lpoint("2018-01-18T13:00:10Z", 200L));
        archive.addRecord(lpoint("2018-01-18T13:00:20Z", 700L));
        archive.addRecord(lpoint("2018-01-18T13:00:30Z", 950L));
        assertEquals(StockpileColumns.minColumnSet(MetricType.RATE), archive.columnSetMask());

        archive.setType(MetricType.DGAUGE);
        assertEquals("Change metric kind also include change mask",
                StockpileColumns.minColumnSet(MetricType.DGAUGE), archive.columnSetMask());

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                // absent
                point("2018-01-18T13:00:10Z", 10d),
                point("2018-01-18T13:00:20Z", 50d),
                point("2018-01-18T13:00:30Z", 25d));

        AggrGraphDataArrayList result = archive.toAggrGraphDataArrayList();
        result.foldDenomIntoOne();
        assertEquals(expected, result);
        close(archive);
    }

    @Test
    public void notAbleExtendToNotValidMask() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        assertThat("By default archive doesn't have column mask",
                archive.columnSetMask(), equalTo(StockpileColumnSet.empty.columnSetMask()));

        archive.setType(MetricType.DGAUGE);
        assertThat("As only archive initialized with kind mask changed to minimal correspond to specified kind",
                archive.columnSetMask(), equalTo(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask()));

        int expendValidMask = StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.COUNT.mask();
        archive.ensureCapacity(expendValidMask, 10);
        assertThat("Column mask extends as only requested additional column",
                archive.columnSetMask(), equalTo(expendValidMask));

        try {
            int notValidMask = expendValidMask | StockpileColumn.HISTOGRAM.mask();
            archive.ensureCapacity(notValidMask, 10);
            fail("Not able add not valid column for particular kind");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            archive.addRecord(randomPoint(MetricType.HIST));
            fail("Not able add not valid column for particular kind");
        } catch (IllegalArgumentException e) {
            // ok
        }

        assertThat(archive.columnSetMask(), equalTo(expendValidMask));
        close(archive);
    }

    @Test
    public void notAbleWriteWithValidMask() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        assertThat(archive.columnSetMask(), equalTo(StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask()));

        int expendValidMask = StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask() | StockpileColumn.COUNT.mask();
        archive.addRecord(randomPoint(expendValidMask));
        assertThat("Column mask extends as only requested additional column",
                archive.columnSetMask(), equalTo(expendValidMask));

        try {
            int notValidMask = expendValidMask | StockpileColumn.HISTOGRAM.mask();
            archive.addRecord(randomPoint(notValidMask));
            fail("Not able add not valid column for particular kind");
        } catch (IllegalArgumentException e) {
            // ok
        }

        assertThat(archive.columnSetMask(), equalTo(expendValidMask));
        close(archive);
    }

    @Test
    public void changeKindOnlyOnSorted() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);
        archive.addRecord(point("2018-01-18T13:00:00Z", 70d));
        archive.addRecord(point("2018-01-18T13:00:30Z", 25d));
        archive.addRecord(point("2018-01-18T13:00:20Z", 50d));
        archive.addRecord(point("2018-01-18T13:00:10Z", 20d));
        assertEquals(StockpileColumns.minColumnSet(MetricType.DGAUGE), archive.columnSetMask());

        archive.setType(MetricType.RATE);
        assertEquals("Change metric kind also include change mask",
                StockpileColumns.minColumnSet(MetricType.RATE), archive.columnSetMask());

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-01-18T13:00:00Z", 700),
                lpoint("2018-01-18T13:00:10Z", 900),
                lpoint("2018-01-18T13:00:20Z", 1400),
                lpoint("2018-01-18T13:00:30Z", 1650));

        AggrGraphDataArrayList result = archive.toAggrGraphDataArrayList();
        assertEquals(expected, result);
        close(archive);
    }

    @Test
    public void changeKindOnlyOnMerged() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);

        archive.addRecord(point("2018-01-18T13:00:00Z", 2d, true, 1));
        archive.addRecord(point("2018-01-18T13:00:00Z", 1d, true, 1));
        archive.addRecord(point("2018-01-18T13:00:00Z", 3d, true, 1));

        archive.addRecord(point("2018-01-18T13:00:10Z", 5d, true, 1));
        archive.addRecord(point("2018-01-18T13:00:10Z", 2d, true, 1));
        archive.addRecord(point("2018-01-18T13:00:10Z", 3d, true, 1));

        archive.addRecord(point("2018-01-18T13:00:20Z", 10d, true, 1));
        archive.addRecord(point("2018-01-18T13:00:20Z", 15d, true, 1));

        archive.setType(MetricType.RATE);
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                lpoint("2018-01-18T13:00:00Z", 60, true, 3),
                lpoint("2018-01-18T13:00:10Z", 160, true, 3),
                lpoint("2018-01-18T13:00:20Z", 410, true, 2));

        AggrGraphDataArrayList result = archive.toAggrGraphDataArrayList();
        assertEquals(expected, result);
        close(archive);
    }

    @Test
    public void writeDefaultValueForHistogram() {
        final long ts0 = System.currentTimeMillis();

        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.HIST);

        archive.addRecord(point(ts0 + 1_000, HistogramColumn.DEFAULT_VALUE));
        archive.addRecord(point(ts0 + 2_000, HistogramColumn.DEFAULT_VALUE));
        archive.addRecord(point(ts0 + 3_000, HistogramColumn.DEFAULT_VALUE));

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList(StockpileColumn.TS.mask() | StockpileColumn.HISTOGRAM.mask(), 3);
        expected.addRecord(point(ts0 + 1_000, Histogram.newInstance()));
        expected.addRecord(point(ts0 + 2_000, Histogram.newInstance()));
        expected.addRecord(point(ts0 + 3_000, Histogram.newInstance()));

        AggrGraphDataArrayList list = archive.toAggrGraphDataArrayList();
        assertEquals(expected, list);
        close(archive);
    }

    @Test
    public void memorySizeEmptyArchive() {
        MetricArchiveMutable archive = new MetricArchiveMutable();

        ClassLayout cl = ClassLayout.parseClass(MetricArchiveMutable.class);
        System.out.println(cl.toPrintable(archive));

        GraphLayout gl = GraphLayout.parseInstance(archive);
        System.out.println(gl.toPrintable());

        // Exclude enums because exists into single instance into app
        long enumSize = GraphLayout.parseInstance(archive.getType(), archive.getFormat()).totalSize();
        assertEquals(gl.totalSize() - enumSize, archive.memorySizeIncludingSelf());
        close(archive);
    }

    @Test
    public void mergeSummaryInt64() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.ISUMMARY);

        final long ts0 = System.currentTimeMillis();
        archive.addRecord(
            AggrPoint.builder()
                .time(ts0)
                .merge(true)
                .count(1)
                .summary(isummary(5, 150, 2, 100, 25))
                .build());

        archive.addRecord(
            AggrPoint.builder()
                .time(ts0)
                .merge(true)
                .count(1)
                .summary(isummary(3, 500, 4, 200, 33))
                .build());

        archive.addRecord(
            AggrPoint.builder()
                .time(ts0)
                .merge(true)
                .count(1)
                .summary(isummary(1, 10, 10, 10, 10))
                .build());

        archive.sortAndMerge();
        AggrGraphDataArrayList result = archive.toAggrGraphDataArrayList();
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(AggrPoint.builder()
            .time(ts0)
            .merge(true)
            .count(3)
            .summary(isummary(5 + 3 + 1, 150 + 500 + 10, 2, 200, 10))
            .build());

        assertEquals(expected, result);
        close(archive);
    }

    @Test
    public void mergeSummaryDouble() {
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DSUMMARY);

        final long ts0 = System.currentTimeMillis();
        archive.addRecord(
            AggrPoint.builder()
                .time(ts0)
                .merge(true)
                .count(1)
                .summary(dsummary(5, 150.4, 2, 100, 25.5))
                .build());

        archive.addRecord(
            AggrPoint.builder()
                .time(ts0)
                .merge(true)
                .count(1)
                .summary(dsummary(3, 500.2, 4, 200, 33.3))
                .build());

        archive.addRecord(
            AggrPoint.builder()
                .time(ts0)
                .merge(true)
                .count(1)
                .summary(dsummary(1, 10.1, 10.1, 10.1, 10.1))
                .build());

        archive.sortAndMerge();
        AggrGraphDataArrayList result = archive.toAggrGraphDataArrayList();
        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(AggrPoint.builder()
            .time(ts0)
            .merge(true)
            .count(3)
            .summary(dsummary(5 + 3 + 1, 150.4 + 500.2 + 10.1, 2, 200, 10.1))
            .build());

        assertEquals(expected, result);
        close(archive);
    }

    @Test
    public void mergeAggregateByReplace() {
        long ts0 = System.currentTimeMillis();
        MetricArchiveMutable archive = new MetricArchiveMutable();
        archive.setType(MetricType.DGAUGE);

        // was
        archive.addRecord(point(ts0, 42, true, 10));

        // replacement
        archive.addRecord(point(ts0, 50, false, 10));

        archive.sortAndMerge();

        var expected = AggrGraphDataArrayList.of(point(ts0, 50, false, 10));
        assertEquals(expected, archive.toAggrGraphDataArrayList());
        close(archive);
    }

    @Test
    public void getFirstTsMillis() {
        MetricArchiveMutable source = new MetricArchiveMutable();
        source.addRecord(point("2018-01-18T13:00:00Z", 0));
        source.addRecord(point("2018-01-18T13:01:00Z", 1));
        source.addRecord(point("2018-01-18T13:02:00Z", 2));

        long expected = timiToMillis("2018-01-18T13:00:00Z");
        assertEquals(expected, source.getFirstTsMillis());
        var immutable = source.toImmutable();
        assertEquals(expected, immutable.getFirstTsMillis());
        close(source, immutable);
    }

    @Test
    public void safeBitBufType() {
        var archive = new MetricArchiveMutable() {
            @Override
            protected BitBuf allocateBuffer(int records, int bytes) {
                return new NettyBitBuf(ByteBufAllocator.DEFAULT.directBuffer(bytes), 0);
            }
        };

        archive.setType(MetricType.IGAUGE);
        archive.addRecord(randomPoint(TsColumn.mask | LongValueColumn.mask));
        var bufferInit = archive.getCompressedDataRaw();
        assertThat(bufferInit, instanceOf(NettyBitBuf.class));
        assertTrue(bufferInit.isDirect());

        // extend mask
        archive.addRecord(randomPoint(TsColumn.mask | LongValueColumn.mask | CountColumn.mask));
        var bufferExtendMask = archive.getCompressedDataRaw();
        assertThat(bufferExtendMask, instanceOf(NettyBitBuf.class));
        assertTrue(bufferExtendMask.isDirect());
        assertEquals(0, bufferInit.refCnt());

        // sort and merge
        while (archive.isSorted()) {
            archive.addRecord(randomPoint(TsColumn.mask | LongValueColumn.mask));
        }
        archive.sortAndMerge();
        var bufferSortAndMerge = archive.getCompressedDataRaw();
        assertThat(bufferSortAndMerge, instanceOf(NettyBitBuf.class));
        assertTrue(bufferSortAndMerge.isDirect());
        assertEquals(0, bufferExtendMask.refCnt());

        // delete before
        archive.setDeleteBefore(archive.getLastTsMillis() - 1);
        var bufferDeleteBefore = archive.getCompressedDataRaw();
        assertThat(bufferDeleteBefore, instanceOf(NettyBitBuf.class));
        assertTrue(bufferDeleteBefore.isDirect());
        assertEquals(0, bufferSortAndMerge.refCnt());

        // change kind
        archive.setType(MetricType.DGAUGE);
        var bufferKind = archive.getCompressedDataRaw();
        assertThat(bufferKind, instanceOf(NettyBitBuf.class));
        assertTrue(bufferKind.isDirect());
        assertEquals(0, bufferDeleteBefore.refCnt());

        archive.close();
        assertEquals(0, bufferKind.refCnt());
    }

    private SummaryInt64Snapshot isummary(long count, long sum, long min, long max, long last) {
        return new ImmutableSummaryInt64Snapshot(count, sum, min, max, last);
    }

    private SummaryDoubleSnapshot dsummary(long count, double sum, double min, double max, double last) {
        return new ImmutableSummaryDoubleSnapshot(count, sum, min, max, last);
    }

    private static long timiToMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }
}
