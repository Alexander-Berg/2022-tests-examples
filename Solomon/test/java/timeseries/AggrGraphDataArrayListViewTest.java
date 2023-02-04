package ru.yandex.solomon.model.timeseries;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.point.column.ValueObject;

/**
 * @author Stepan Koltsov
 */
public class AggrGraphDataArrayListViewTest {

    @Test
    public void sumAvg() {
        long ts0 = Instant.parse("2016-01-28T23:28:00Z").toEpochMilli();

        Assert.assertTrue(AggrGraphDataArrayList.empty().view().avgSum().isNaN());
        Assert.assertEquals(20.0, AggrGraphDataArrayList.listShort(ts0, 20).view().avgSum().divide(), 0);
        Assert.assertEquals(30.0, AggrGraphDataArrayList.listShort(ts0, 20, ts0 + 1000, 40).view().avgSum().divide(), 0);
        Assert.assertEquals(50.0, AggrGraphDataArrayList.listShort(ts0, 20, ts0 + 1000, 40, ts0 + 5000, 90).view().avgSum().divide(), 0);
    }

    @Test
    public void sumAvgSameDenom() {
        long ts0 = Instant.parse("2017-01-28T23:28:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();

        AggrPoint point = new AggrPoint();

        point.setTsMillis(ts0 + 1000);
        point.setValue(30, 15000);
        al.addRecord(point);

        point.setTsMillis(ts0 + 2000);
        point.setValue(60, 15000);
        al.addRecord(point);

        ValueObject avg = al.view().avgSum();
        Assert.assertEquals(new ValueObject(90, 30000), avg);
    }

    @Test
    public void sumAvgMixedWithNonZeroDenom() {
        long ts0 = Instant.parse("2017-01-28T23:28:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();

        AggrPoint point = new AggrPoint();

        point.setTsMillis(ts0 + 1000);
        point.setValue(30, 15000);
        al.addRecord(point);

        point.setTsMillis(ts0 + 2000);
        point.setValue(120, 30000);
        al.addRecord(point);

        ValueObject avg = al.view().avgSum();
        Assert.assertEquals(new ValueObject(3, ValueColumn.DEFAULT_DENOM), avg);
    }

    @Test
    public void sumAvgMixedWithZeroDenom() {
        long ts0 = Instant.parse("2017-01-28T23:28:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();

        AggrPoint point = new AggrPoint();

        point.setTsMillis(ts0 + 1000);
        point.setValue(30, 15000);
        al.addRecord(point);

        point.setTsMillis(ts0 + 2000);
        point.setValue(4, ValueColumn.DEFAULT_DENOM);
        al.addRecord(point);

        ValueObject avg = al.view().avgSum();
        Assert.assertEquals(new ValueObject(3, ValueColumn.DEFAULT_DENOM), avg);
    }

    @Test
    public void complexCase() {
        long ts0 = Instant.parse("2017-01-28T23:28:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();

        AggrPoint point = new AggrPoint();

        point.setTsMillis(ts0 + 1000);
        point.setValue(30, 15000);
        al.addRecord(point);

        point.setTsMillis(ts0 + 2000);
        point.setValue(60, 15000);
        al.addRecord(point);

        point.setTsMillis(ts0 + 2000);
        point.setValue(3, ValueColumn.DEFAULT_DENOM);
        al.addRecord(point);

        ValueObject avg = al.view().avgSum();
        Assert.assertEquals(new ValueObject(3, ValueColumn.DEFAULT_DENOM), avg);
    }

    @Test
    public void sumAvgDeriv() {
        long ts0 = Instant.parse("2016-01-28T23:28:00Z").toEpochMilli();
        Assert.assertTrue(AggrGraphDataArrayList.empty().view().avgSumDeriv().isNaN());
        Assert.assertTrue(AggrGraphDataArrayList.listShort(ts0, 100).view().avgSumDeriv().isNaN());
        Assert.assertEquals(50, AggrGraphDataArrayList.listShort(ts0, 100, ts0 + 2000, 200).view().avgSumDeriv().divide(), 0);

        AggrGraphDataArrayList l = AggrGraphDataArrayList.listShort(
            ts0 + 1000, 100,
            ts0 + 2000, 300,
            ts0 + 5000, 900);
        Assert.assertEquals(200, l.view().avgSumDeriv().divide(), 0);
    }

    @Test
    public void drop() {
        long ts0 = Instant.parse("2017-01-17T20:51:00Z").toEpochMilli();

        AggrGraphDataArrayListView l = AggrGraphDataArrayList.listShort(
            ts0 + 1000, 10,
            ts0 + 2000, 20,
            ts0 + 3000, 30)
                .view();

        Assert.assertEquals(l, l.dropPointsByTsBeforeInSorted(ts0));
        Assert.assertEquals(l, l.dropPointsByTsBeforeInSorted(ts0 + 1000));
        Assert.assertEquals(l.slice(1, 3), l.dropPointsByTsBeforeInSorted(ts0 + 1500));
        Assert.assertEquals(l.slice(1, 3), l.dropPointsByTsBeforeInSorted(ts0 + 2000));
        Assert.assertEquals(l.slice(2, 3), l.dropPointsByTsBeforeInSorted(ts0 + 3000));
        Assert.assertEquals(l.slice(3, 3), l.dropPointsByTsBeforeInSorted(ts0 + 4000));
    }
}
