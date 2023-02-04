package ru.yandex.solomon.model.timeseries;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnField;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.point.column.SummaryDoubleColumn;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.SummaryDouble;
import ru.yandex.solomon.util.collection.array.LongArrayView;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomHist;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;
import static ru.yandex.solomon.model.point.AggrPoints.lpoint;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Stepan Koltsov
 */
public class AggrGraphDataArrayListTest {

    @Test
    public void mergeAdjacent() {
        Random r = new Random(17);

        long ts0 = Instant.parse("2016-03-19T22:18:06Z").toEpochMilli();

        for (int i = 0; i < 10000; ++i) {
            AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
            AggrGraphDataArrayList x = new AggrGraphDataArrayList();

            int length = r.nextInt(100);
            for (int j = 0; j < length; ++j) {
                int s = 1 + r.nextInt(10);
                long ts = ts0 + r.nextInt(5) * 1000;

                double sum = 0;
                int count = 0;
                for (int k = 1; k <= s; ++k) {
                    double d = r.nextInt(100);
                    int c = r.nextInt(10);
                    x.addRecordFullForTest(ts, d, true, c);
                    sum += d;
                    count += c;
                }

                expected.addRecordFullForTest(ts, sum, true, count);
            }

            x.mergeAdjacent();
            expected.mergeAdjacent();

            assertEquals(expected, x);
        }
    }


    @Test
    public void filterInPlace() {
        long ts0 = Instant.parse("2016-08-29T20:33:26Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();
        AggrGraphDataArrayList ep = new AggrGraphDataArrayList();

        al.addRecordShort(ts0, 10);
        ep.addRecordShort(ts0, 10);

        al.addRecordShort(ts0 + 100, 30);

        al.addRecordShort(ts0 + 300, 20);
        ep.addRecordShort(ts0 + 300, 20);

        al.filterInPlace(pointData -> pointData.valueNum != 30);

        assertEquals(ep, al);
    }

    @Test
    public void testEmpty() {
        AggrGraphDataArrayList data = new AggrGraphDataArrayList();
        data.toGraphDataShort();
    }

    @Test
    public void toStringTest() {
        for (int i = 0; i < 10; ++i) {
            AggrGraphDataArrayList l = new AggrGraphDataArrayList();
            l.addRecord(randomPoint());
            System.out.println(l);
        }
    }

    @Test
    public void mergesReallyCompact() throws Exception {
        long ts0 = Instant.parse("2016-12-20T10:20:30Z").toEpochMilli();
        AggrGraphDataArrayList al = AggrGraphDataArrayList.empty();
        AggrPoint point = new AggrPoint();
        point.setTsMillis(ts0);
        point.setMerge(true);
        al.addRecord(point);

        Field field = AggrGraphDataArrayList.arraysMh.fieldForColumn(StockpileColumnField.MERGE);
        Object merge = field.get(al);

        Assert.assertSame(true, merge);
    }

    @Test
    public void emptyArraysCapacity() throws Exception {
        AggrGraphDataArrayList a = new AggrGraphDataArrayList(StockpileColumnSet.maxMask, 100);
        for (StockpileColumnField field : StockpileColumnField.values()) {
            Object array = AggrGraphDataArrayList.arraysMh.getArrayForField(field, a);
            if (field.compactArrays) {
                Assert.assertSame(field.defaultValue(), array);
            }
        }
    }

    @Test
    public void bug1() throws Exception {
        long ts = Instant.parse("2017-01-13T23:15:00Z").toEpochMilli();

        AggrGraphDataArrayList a = new AggrGraphDataArrayList();
        a.addRecordShort(ts, 10);
        a.addRecordFullForTest(ts + 1000, 20, true, 5);

        assertEquals(false, a.getMergeOrDefault(0));
        assertEquals(true, a.getMergeOrDefault(1));
    }

    @Test
    public void random() throws Exception {
        Random r = new Random(17);

        for (int i = 0; i < 1000; ++i) {
            AggrGraphDataArrayList al = new AggrGraphDataArrayList();
            ArrayList<AggrPoint> points = new ArrayList<>();

            int len = 1 + r.nextInt(50);

            for (int j = 0; j < len; ++j) {
                AggrPoint point = randomPoint();
                points.add(point);
                al.addRecord(point);
            }

            for (int j = 0; j < points.size(); j++) {
                AggrPoint expected = points.get(j).withMask(al.columnSetMask());
                AggrPoint point = al.getAnyPoint(j);
                assertEquals(expected, point);
            }
        }
    }

    @Test
    public void toSumList() {
        long ts = Instant.parse("2017-01-23T23:15:00Z").toEpochMilli();

        AggrGraphDataArrayList l = new AggrGraphDataArrayList();
        AggrPoint p = new AggrPoint();

        p.setTsMillis(ts + 1000);
        p.setValue(60, 2000);
        l.addRecord(p);

        p.setTsMillis(ts + 2000);
        p.setValue(80, 4000);
        l.addRecord(p);

        GraphData s = l.toGraphDataShort();

        GraphData expected = GraphData.graphData(ts + 1000, 30, ts + 2000, 20);

        assertEquals(expected, s);
    }

    @Test
    public void testLongSum() {
        long ts = Instant.parse("2017-01-23T23:15:00Z").toEpochMilli();

        AggrGraphDataArrayList l = new AggrGraphDataArrayList();

        AggrPoint p = lpoint(ts + 1000, 42);
        l.addRecord(p);

        p = lpoint(ts + 2000, 43);
        l.addRecord(p);

        GraphData s = l.toGraphDataShort();
        GraphData expected = GraphData.graphData(ts + 1000, 42, ts + 2000, 43);
        assertEquals(expected, s);
    }

    @Test
    public void retain() {
        AggrGraphDataArrayList l = new AggrGraphDataArrayList();
        l.addRecordShort(10, 1.0);
        l.addRecordShort(20, 2.0);
        l.addRecordShort(30, 3.0);
        l.addRecordShort(40, 4.0);

        l.retainIf(p -> p.tsMillis >= 40 || p.getValueNum() == 1.0);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordShort(10, 1.0);
        expected.addRecordShort(40, 4.0);

        assertEquals(expected, l);
    }

    @Test
    public void getTimestamps() {
        final long ts0 = Instant.parse("2018-10-26T11:00:00Z").toEpochMilli();

        final int mask = StockpileColumn.TS.mask() | StockpileColumn.VALUE.mask();
        AggrGraphDataArrayList list = new AggrGraphDataArrayList(mask, 100);
        assertEquals(LongArrayView.empty, list.getTimestamps());

        long[] expected = new long[200];
        for (int index = 0; index < 200; index++) {
            long ts = ts0 + 10_000 * index;
            expected[index] = ts;
            list.addRecordShort(ts, 42.0);
        }

        assertEquals(200, list.getTimestamps().length());
        assertArrayEquals(expected, list.getTimestamps().copyOrArray());
    }

    @Test
    public void objectsCopied() {
        SummaryDouble temp = SummaryDouble.newInstance()
            .setMax(10)
            .setMin(5)
            .setCount(2)
            .setSum(14);

        AggrPoint point = new AggrPoint();
        point.setTsMillis(100);
        point.setSummaryDouble(temp);

        AggrGraphDataArrayList list = new AggrGraphDataArrayList();
        list.addRecord(point);

        var one = SummaryDoubleColumn.copy(temp);
        temp.setCount(100500)
            .setSum(42);

        point.setTsMillis(200);
        point.setSummaryDouble(temp);
        list.addRecord(point);

        list.sortAndMerge();
        var two = SummaryDoubleColumn.copy(temp);
        temp.recycle();

        assertEquals(one, list.getSummaryDouble(0));
        assertEquals(two, list.getSummaryDouble(1));
    }

    @Test
    public void mutableFieldsHistogram() {
        var expected = Histogram.copyOf(randomHist(ThreadLocalRandom.current()));

        var point = RecyclableAggrPoint.newInstance();
        point.setTsMillis(System.currentTimeMillis());
        point.setHistogram(Histogram.copyOf(expected));

        AggrGraphDataArrayList list = new AggrGraphDataArrayList();
        list.addRecord(point);

        for (int index = 0; index < 3; index++) {
            point.recycle();
            point = RecyclableAggrPoint.newInstance();
            {
                var it = list.iterator();
                assertTrue(it.next(point));
                assertEquals(expected, point.histogram);
            }
            {
                list.getDataTo(0, point);
                assertEquals(expected, point.histogram);
            }
        }
    }

    @Test
    public void mergeAggregateByReplace() {
        long ts0 = System.currentTimeMillis();
        AggrGraphDataArrayList list = new AggrGraphDataArrayList();
        // was
        list.addRecord(point(ts0, 42, true, 10));

        // replacement
        list.addRecord(point(ts0, 50, false, 15));

        list.sortAndMerge();
        var expected = AggrGraphDataArrayList.of(point(ts0, 50, false, 15));
        assertEquals(expected, list);
    }

    @Test
    public void emptyEq() {
        AggrGraphDataArrayList one = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, 5);
        AggrGraphDataArrayList two = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, 10);
        assertEquals(one, two);
        assertEquals(two, one);
    }

    @Test
    public void emptyEqTwo() {
        AggrGraphDataArrayList one = new AggrGraphDataArrayList(TsColumn.mask | ValueColumn.mask, 5);
        AggrGraphDataArrayList two = AggrGraphDataArrayList.empty();
        assertEquals(one, two);
        assertEquals(two, one);
    }
}
