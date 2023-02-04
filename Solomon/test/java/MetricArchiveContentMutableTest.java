package ru.yandex.solomon.codec;

import java.time.Instant;
import java.util.Random;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.solomon.codec.archive.MetricArchiveImmutable;
import ru.yandex.solomon.codec.archive.MetricArchiveMutable;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.point.column.CountColumn;
import ru.yandex.solomon.model.point.column.CountColumnRandomData;
import ru.yandex.solomon.model.point.column.MergeColumn;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.point.column.TsRandomData;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.point.column.ValueRandomData;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.solomon.model.timeseries.AggrGraphDataLists;
import ru.yandex.solomon.model.timeseries.GraphDataArrayList;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.model.point.AggrPoints.point;
import static ru.yandex.solomon.util.CloseableUtils.close;

/**
 * @author Stepan Koltsov
 */
public class MetricArchiveContentMutableTest {

    @Test
    public void shortRecord() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        GraphDataArrayList gd = new GraphDataArrayList();
        gd.add(ts, 17);
        gd.add(ts + 1000, 19);

        MetricArchiveMutable archive = allocate();
        gd.visit((tsMillis, value) -> archive.addRecord(AggrPoints.point(tsMillis, value)));

        GraphDataArrayList back = AggrGraphDataLists.toGraphDataArrayList(archive);

        Assert.assertEquals(gd, back);
        close(archive);
    }

    @Test
    public void shortRecordOverwrite() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable archive = allocate();
        archive.addRecord(point(ts, 17));
        archive.addRecord(point(ts, 19));

        AggrGraphDataArrayList back = archive.toAggrGraphDataArrayList();

        Assert.assertEquals(AggrGraphDataArrayList.of(point(ts, 19)), back);
        close(archive);
    }

    @Test
    public void shortThenFull() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        b.addRecord(point(ts, 10));
        b.addRecord(point(ts + 10000, 20, true, 2));

        AggrGraphDataArrayList r = AggrGraphDataLists.toAggrListUnsorted(b);
        Assert.assertEquals(2, r.getRecordCount());
        Assert.assertEquals(StockpileColumnSet.fromColumnsVa(StockpileColumn.TS, StockpileColumn.VALUE, StockpileColumn.MERGE, StockpileColumn.COUNT), r.columnSet());
        Assert.assertEquals(ts, r.getAnyPoint(0).getTsMillis());
        Assert.assertEquals(ts + 10000, r.getAnyPoint(1).getTsMillis());
        close(b);
    }

    @Test
    public void fullThenShort() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        b.addRecord(point(ts, 20, true, 2));
        b.addRecord(point(ts + 1000, 30));

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordFullForTest(ts, 20, true, 2);
        expected.addRecordFullForTest(ts + 1000, 30, MergeColumn.DEFAULT_VALUE, CountColumn.DEFAULT_VALUE);

        Assert.assertEquals(expected, AggrGraphDataLists.toAggrListUnsorted(b));
        close(b);
    }

    @Test
    public void nonIntersectingColumnsInAddLast() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();

        {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(ts);
            point.setValue(20, ValueColumn.DEFAULT_DENOM);
            point.setMerge(true);
            b.addRecord(point);
        }

        {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(ts + 1000);
            point.setValue(30, ValueColumn.DEFAULT_DENOM);
            point.setCount(3);
            b.addRecord(point);
        }

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordFullForTest(ts, 20, true, CountColumn.DEFAULT_VALUE);
        expected.addRecordFullForTest(ts + 1000, 30, MergeColumn.DEFAULT_VALUE, 3);

        Assert.assertEquals(expected, AggrGraphDataLists.toAggrListUnsorted(b));
        close(b);
    }

    @Test
    public void nonIntersectingColumns2() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();

        {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(ts);
            point.setValue(20, 0);
            point.setMerge(true);
            b.addRecord(point);
        }

        {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(ts + 1000);
            point.setValue(30, 0);
            point.setMerge(true);
            b.addRecord(point);
        }

        {
            AggrPoint point = new AggrPoint();
            point.setTsMillis(ts + 2000);
            point.setValue(40, 0);
            point.setCount(3);
            b.addRecord(point);
        }

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordFullForTest(ts, 20, true, CountColumn.DEFAULT_VALUE);
        expected.addRecordFullForTest(ts + 1000, 30, true, CountColumn.DEFAULT_VALUE);
        expected.addRecordFullForTest(ts + 2000, 40, MergeColumn.DEFAULT_VALUE, 3);

        Assert.assertEquals(expected, AggrGraphDataLists.toAggrListUnsorted(b));
        close(b);
    }

    @Test
    public void fullOverwritesShort() {
        long ts = Instant.parse("2016-03-26T04:00:02Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        b.addRecord(point(ts, 10, true, 20));
        b.addRecord(point(ts, 30));

        MetricArchiveMutable reference = allocate();
        reference.addRecord(point(ts, 30, MergeColumn.DEFAULT_VALUE, CountColumn.DEFAULT_VALUE));

        Assert.assertEquals(reference.toAggrGraphDataArrayList(), b.toAggrGraphDataArrayList());
        close(b, reference);
    }

    @Test
    public void longAddsOnAdd() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        b.addRecord(point(ts, 10, true, 1));
        b.addRecord(point(ts, 12, true, 1));

        AggrGraphDataArrayList aggrList = b.toAggrGraphDataArrayList();
        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecord(point(ts, 22, true, 2));

        Assert.assertEquals(expected, aggrList);
        close(b);
    }

    @Test
    public void sortAndMerge() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        b.addRecord(point(ts + 1000, 17));
        b.addRecord(point(ts - 1000, 19));

        Assert.assertEquals(GraphDataArrayList.of(ts + 1000, 17, ts - 1000, 19), AggrGraphDataLists.toGraphDataArrayList(b));

        b.sortAndMerge();

        Assert.assertEquals(GraphDataArrayList.of(ts - 1000, 19, ts + 1000, 17), AggrGraphDataLists.toGraphDataArrayList(b));
        close(b);
    }

    @Test
    public void checkItMergesSelf() {
        long ts = Instant.parse("2015-12-11T12:13:14Z").toEpochMilli();

        MetricArchiveMutable b = allocate();

        for (int i = 0; i < 3; ++i) {
            b.addRecord(point(ts - 1000, 17));
            b.addRecord(point(ts + 1000, 19));
        }

        Assert.assertEquals(6, b.getRecordCount());

        for (int i = 0; i < 1000; ++i) {
            b.addRecord(point(ts - 1000, 17));
            b.addRecord(point(ts + 1000, 19));

            if (b.getRecordCount() <= 3) {
                close(b);
                return;
            }
        }

        Assert.fail("did not merge");
    }

    @Test
    public void removeBefore() {
        long ts = Instant.parse("2015-11-12T13:14:15Z").toEpochMilli();

        MetricArchiveMutable b = allocate();

        b.addRecord(point(ts - 1000, 17));
        b.addRecord(point(ts + 1000, 19));
        b.addRecord(point(ts, 23));

        b.setDeleteBefore(ts);

        GraphDataArrayList actual = AggrGraphDataLists.toGraphDataArrayList(b);
        GraphDataArrayList expected = GraphDataArrayList.of(ts + 1000, 19, ts, 23);
        Assert.assertEquals(expected, actual);
        close(b);
    }

    @Test
    public void toImmutable() {
        {
            long ts = Instant.parse("2016-01-29T00:40:43Z").toEpochMilli();

            MetricArchiveMutable b = allocate();
            b.addRecord(point(ts + 1000, 10));
            b.addRecord(point(ts + 3000, 20));
            b.addRecord(point(ts + 2000, 30));

            b.sortAndMerge();

            MetricArchiveMutable expected = allocate();
            expected.addRecord(point(ts + 1000, 10));
            expected.addRecord(point(ts + 2000, 30));
            expected.addRecord(point(ts + 3000, 20));

            Assert.assertEquals(expected, b);
            close(b, expected);
        }

        {
            MetricArchiveMutable b = allocate();
            MetricArchiveMutable expected = allocate();

            Assert.assertEquals(expected, b);
            close(b, expected);
        }
    }

    @Test
    public void bug1() {
        long ts = Instant.parse("2016-03-23T00:40:43Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        AggrGraphDataArrayList l = new AggrGraphDataArrayList();

        b.addRecord(point(ts, 10));
        l.addRecord(point(ts, 10));

        b.addRecord(point(ts, 20, true, 1));
        l.addRecord(point(ts, 20, true, 1));

        b.addRecord(point(ts, 30, true, 2));
        l.addRecord(point(ts, 30, true, 2));

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordFullForTest(ts, 60, false, 3);

        l.mergeAdjacent();

        Assert.assertEquals(expected, b.toAggrGraphDataArrayList());
        Assert.assertEquals(expected, l);
        close(b);
    }

    @Test
    public void randomFull() {

        Random random = new Random(17);

        AggrGraphDataArrayList l = new AggrGraphDataArrayList();

        for (int i = 0; i < 9000; ++i) {
            long ts = TsRandomData.randomTs(random);
            l.addRecordFullForTest(ts, ValueRandomData.randomNum(random), random.nextBoolean(), CountColumnRandomData.randomCount(random));
        }

        MetricArchiveMutable b = allocate();
        b.addAll(l);

        l.sortAndMerge();

        AggrGraphDataArrayList bl = b.toAggrGraphDataArrayList();

        Assert.assertArrayEquals(l.tss(), bl.tss());
        Assert.assertArrayEquals(l.values(), bl.values(), 0);
        Assert.assertArrayEquals(l.counts(), bl.counts());
        Assert.assertTrue(Cf.BooleanArray.equals(l.merges(), bl.merges()));
        Assert.assertEquals(l, bl);
        close(b);
    }

    @Test
    public void iterator() {
        long ts0 = Instant.parse("2016-01-09T12:13:45Z").toEpochMilli();

        MetricArchiveMutable b = allocate();
        b.addRecord(point(ts0, 20));
        b.addRecord(point(ts0 + 1000, 30));

        AggrGraphDataListIterator it = b.iterator();
        AggrPoint pointData = new AggrPoint();
        Assert.assertTrue(it.next(pointData));
        Assert.assertEquals(ts0, pointData.tsMillis);
        Assert.assertEquals(20, pointData.valueNum, 0);
        Assert.assertTrue(it.next(pointData));
        Assert.assertEquals(ts0 + 1000, pointData.tsMillis);
        Assert.assertEquals(30, pointData.valueNum, 0);
        Assert.assertFalse(it.next(pointData));
        close(b);
    }

    @Test
    public void numberPointsWithSameTsMerged() throws Exception {
        MetricArchiveMutable content = allocate();
        content.addRecord(numberPoint("2017-06-01T09:14:22Z", 5));
        content.addRecord(numberPoint("2017-06-01T09:14:22Z", 15));
        content.addRecord(numberPoint("2017-06-01T09:15:00Z", 4));
        content.addRecord(numberPoint("2017-06-01T09:14:22Z", 9));

        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            numberPoint("2017-06-01T09:14:22Z", 9),
            numberPoint("2017-06-01T09:15:00Z", 4)
        );

        assertThat(result, CoreMatchers.equalTo(expected));
        close(content);
    }

    @Test
    public void logHistogramPointsWithSameTsMerged() throws Exception {
        MetricArchiveMutable content = allocate();
        content.addRecord(logHistogramPoint("2017-06-01T09:14:22Z", LogHistogram.ofBuckets(1, 2, 3)));
        content.addRecord(logHistogramPoint("2017-06-01T09:14:22Z", LogHistogram.ofBuckets(2, 5, 10, 15)));
        content.addRecord(logHistogramPoint("2017-06-01T09:15:00Z", LogHistogram.ofBuckets(1, 9, 10)));
        content.addRecord(logHistogramPoint("2017-06-01T09:14:22Z", LogHistogram.ofBuckets(5, 0, 2)));

        AggrGraphDataArrayList result = content.toAggrGraphDataArrayList();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            logHistogramPoint("2017-06-01T09:14:22Z", LogHistogram.ofBuckets(5, 0, 2)),
            logHistogramPoint("2017-06-01T09:15:00Z", LogHistogram.ofBuckets(1, 9, 10))
        );

        assertThat(result, CoreMatchers.equalTo(expected));
        close(content);
    }

    @Test
    public void toImmutableNoCopyReallyImmutable() {
        MetricArchiveMutable content = allocate();
        content.addRecord(point("2018-07-24T07:30:00Z", 1));
        content.addRecord(point("2018-07-24T07:30:02Z", 2));
        content.addRecord(point("2018-07-24T07:30:05Z", 5));

        MetricArchiveImmutable copy = content.toImmutableNoCopy();

        content.addRecord(point("2018-07-24T07:30:06Z", 6));
        content.addRecord(point("2018-07-24T07:30:03Z", 3));
        content.addRecord(point("2018-07-24T07:30:04Z", 4));
        content.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2018-07-24T07:30:00Z", 1),
                point("2018-07-24T07:30:02Z", 2),
                point("2018-07-24T07:30:05Z", 5)
        );

        assertThat(copy.toAggrGraphDataArrayList(), equalTo(expected));
        close(content, copy);
    }

    private MetricArchiveMutable allocate() {
        return new MetricArchiveMutable();
    }

    private static AggrPoint numberPoint(String time, double value) {
        return AggrPoint.builder().time(time).doubleValue(value).build();
    }

    private static AggrPoint logHistogramPoint(String time, LogHistogram histogram) {
        return AggrPoint.builder().time(time).logHistogram(histogram).build();
    }
}
