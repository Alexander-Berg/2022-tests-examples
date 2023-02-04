package ru.yandex.solomon.codec.archive;

import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.Test;

import ru.yandex.solomon.codec.archive.header.MetricHeader;
import ru.yandex.solomon.codec.serializer.StockpileFormat;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataLists;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Sergey Polovko
 */
public class MetricArchiveUtilsTest {

    @Test
    public void merge() throws Exception {
        MetricArchiveMutable a1 = new MetricArchiveMutable();
        a1.addRecord(point(10, 21));
        a1.addRecord(point(15, 22));
        a1.addRecord(point(20, 23));
        a1.addRecord(point(25, 24));
        a1.addRecord(point(30, 25));

        MetricArchiveMutable a2 = new MetricArchiveMutable();
        a2.addRecord(point(10, 11));
        a2.addRecord(point(20, 12));
        a2.addRecord(point(30, 13));
        a2.addRecord(point(40, 14));
        a2.addRecord(point(50, 15));

        AggrGraphDataArrayList merged = mergeToList(a1, a2);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordShort(10, 11);
        expected.addRecordShort(15, 22);
        expected.addRecordShort(20, 12);
        expected.addRecordShort(25, 24);
        expected.addRecordShort(30, 13);
        expected.addRecordShort(40, 14);
        expected.addRecordShort(50, 15);

        assertEquals(expected, merged);

        MetricArchiveMutable mergedArchive = merge(a1, a2);
        assertEquals(expected, mergedArchive.toAggrGraphDataArrayList());
        close(a1, a2, mergedArchive);
    }

    @Test
    public void mergeWithDeleteBefore() throws Exception {
        // deleteBefore has priority determined by order of given archives

        {
            MetricArchiveMutable a1 = new MetricArchiveMutable();
            a1.addRecord(point(30, 25));
            a1.setDeleteBefore(30);

            MetricArchiveMutable a2 = new MetricArchiveMutable();
            a2.addRecord(point(10, 11));
            a2.addRecord(point(20, 12));
            a2.addRecord(point(30, 13));

            AggrGraphDataArrayList merged = mergeToList(a1, a2);

            AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
            expected.addRecordShort(10, 11);
            expected.addRecordShort(20, 12);
            expected.addRecordShort(30, 13);

            assertEquals(expected, merged);
            close(a1, a2);
        }

        {
            MetricArchiveMutable a1 = new MetricArchiveMutable();
            a1.addRecord(point(10, 11));
            a1.addRecord(point(20, 12));
            a1.addRecord(point(30, 13));

            MetricArchiveMutable a2 = new MetricArchiveMutable();
            a2.addRecord(point(30, 25));
            a2.setDeleteBefore(30);

            AggrGraphDataArrayList merged = mergeToList(a1, a2);

            AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
            expected.addRecordShort(30, 25);

            assertEquals(expected, merged);
            close(a1, a2);
        }
    }

    @Test
    public void downgradeArchiveVersion() throws Exception {
        Assume.assumeTrue(StockpileFormat.MIN != StockpileFormat.MAX);
        final StockpileFormat initFormat = StockpileFormat.MAX;

        MetricArchiveImmutable archive = archive(initFormat,
            AggrPoint.builder().time("2017-06-16T09:44:30Z").doubleValue(1).build(),
            AggrPoint.builder().time("2017-06-16T09:44:35Z").doubleValue(2).build(),
            AggrPoint.builder().time("2017-06-16T09:44:40Z").doubleValue(3).build()
        );

        StockpileFormat previousFormat = StockpileFormat.byNumber(initFormat.getFormat() - 1);
        MetricArchiveImmutable newArchive = MetricArchiveUtils.repack(previousFormat, archive);

        assertThat(archive.getFormat(), equalTo(initFormat));
        assertThat(newArchive.getFormat(), equalTo(previousFormat));
        assertThat(AggrGraphDataLists.toAggrListUnsorted(newArchive), equalTo(AggrGraphDataLists.toAggrListUnsorted(archive)));
        close(archive, newArchive);
    }

    @Test
    public void upgradeArchiveVersion() throws Exception {
        final StockpileFormat initFormat = StockpileFormat.MIN;

        MetricArchiveImmutable archive = archive(initFormat,
            AggrPoint.builder().time("2017-06-16T09:44:30Z").doubleValue(3).build(),
            AggrPoint.builder().time("2017-06-16T09:44:35Z").doubleValue(2).build(),
            AggrPoint.builder().time("2017-06-16T09:44:40Z").doubleValue(1).build()
        );

        StockpileFormat nextFormat = StockpileFormat.MAX;
        MetricArchiveImmutable newArchive = MetricArchiveUtils.repack(nextFormat, archive);

        assertThat(archive.getFormat(), equalTo(initFormat));
        assertThat(newArchive.getFormat(), equalTo(nextFormat));
        assertThat(AggrGraphDataLists.toAggrListUnsorted(newArchive), equalTo(AggrGraphDataLists.toAggrListUnsorted(archive)));
        close(archive, newArchive);
    }

    @Test
    public void avoidRepackIfAvailable() {
        MetricArchiveImmutable content = archive(StockpileFormat.MIN,
                point("2017-06-16T09:44:30Z", 1),
                point("2017-06-16T09:44:35Z", 2),
                point("2017-06-16T09:44:40Z", 3));

        MetricArchiveImmutable result = MetricArchiveUtils.encode(StockpileFormat.MIN, content);
        assertThat(result, sameInstance(content));
        close(content);
    }

    @Test
    public void repackIfEncodedByDiffFormat() {
        Assume.assumeTrue(StockpileFormat.MIN != StockpileFormat.MAX);
        MetricArchiveImmutable content = archive(StockpileFormat.MAX,
                point("2017-06-16T09:44:30Z", 1),
                point("2017-06-16T09:44:35Z", 2),
                point("2017-06-16T09:44:40Z", 3));

        MetricArchiveImmutable result = MetricArchiveUtils.encode(StockpileFormat.MIN, content);
        assertNotSame(content, result);
        assertEquals(StockpileFormat.MIN, result.getFormat());
        assertEquals(AggrGraphDataLists.toAggrListUnsorted(content), AggrGraphDataLists.toAggrListUnsorted(result));
        close(content, result);
    }

    private AggrGraphDataArrayList mergeToList(MetricArchiveMutable... archives) {
        var immutable = Stream.of(archives)
            .map(MetricArchiveMutable::toImmutable)
            .toArray(MetricArchiveImmutable[]::new);
        try {
            return MetricArchiveUtils.mergeToList(immutable);
        } finally {
            Stream.of(immutable).forEach(MetricArchiveImmutable::close);
        }
    }

    private MetricArchiveMutable merge(MetricArchiveMutable... archives) {
        var immutable = Stream.of(archives)
            .map(MetricArchiveMutable::toImmutable)
            .toArray(MetricArchiveImmutable[]::new);

        try {
            return MetricArchiveUtils.merge(immutable);
        } finally {
            Stream.of(immutable).forEach(MetricArchiveImmutable::close);
        }
    }



    private static MetricArchiveImmutable archive(StockpileFormat format, AggrPoint... points) {
        MetricArchiveMutable archive = new MetricArchiveMutable(MetricHeader.defaultValue, format);
        for (AggrPoint point : points) {
            archive.addRecord(point);
        }

        try {
            return archive.toImmutableNoCopy();
        } finally {
            archive.close();
        }
    }
}
