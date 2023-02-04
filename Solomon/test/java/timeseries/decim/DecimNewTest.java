package ru.yandex.solomon.model.timeseries.decim;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointData;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.point.column.StockpileColumns;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayListViewIterator;
import ru.yandex.solomon.model.timeseries.AggrGraphDataListIterator;
import ru.yandex.solomon.model.timeseries.aggregation.collectors.PointValueCollector;
import ru.yandex.solomon.model.type.LogHistogram;
import ru.yandex.stockpile.api.EDecimPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Stepan Koltsov
 */
public class DecimNewTest {

    interface DecimViewTemplate {
        void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point);
        void setExpected(AggrPointData expected);
    }

    private static final long decimStepMillis = 300_000;

    private void decimViewTest(DecimViewTemplate template) {
        long ts0 = Instant.parse("2017-03-08T12:13:15Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();

        template.addPoints(ts0, al, new AggrPoint());

        AggrPointData target = decim(al, ts0, decimStepMillis);
        AggrPointData expected = new AggrPointData();
        expected.tsMillis = ts0;
        template.setExpected(expected);

        Assert.assertEquals(expected, target);
    }

    private AggrPointData decim(AggrGraphDataArrayList source, long ts0, long targetStep) {
        PointValueCollector collector =
                DecimPointValueCollector.of(StockpileColumns.typeByMask(source.columnSetMask()));
        AggrPoint temp = new AggrPoint();
        AggrGraphDataArrayListViewIterator it = source.iterator();
        while (it.next(temp)) {
            collector.append(temp);
        }

        AggrPoint target = new AggrPoint(source.columnSetMask());
        collector.compute(target);
        target.tsMillis = ts0;
        target.stepMillis = Math.max(target.stepMillis, targetStep);
        target.clearFields(StockpileColumnSet.maxMask & ~source.columnSetMask());
        return new AggrPointData(target);
    }

    @Test
    public void decimViewValueSimple() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                al.addRecordShort(ts0 + 1000, 10);
                al.addRecordShort(ts0 + 2000, 20);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.valueNum = 15;
            }
        });
    }

    @Test
    public void decimViewValueDenomNonDefaultId() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                point.setTsMillis(ts0 + 1000);
                point.setValue(2, 3000);
                al.addRecord(point);

                point.setTsMillis(ts0 + 2000);
                point.setValue(4, 3000);
                al.addRecord(point);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.valueNum = 6;
                expected.valueDenom = 6000;
            }
        });
    }

    @Test
    public void decimViewValueDenomDefaultAndNonDefault() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                point.setTsMillis(ts0 + 1000);
                point.setValue(10, 5000);
                al.addRecord(point);

                point.setTsMillis(ts0 + 2000);
                point.setValue(4, ValueColumn.DEFAULT_DENOM);
                al.addRecord(point);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.valueNum = 3;
            }
        });
    }

    @Test
    public void decimViewValueDenomDifferent() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                point.setTsMillis(ts0 + 1000);
                point.setValue(6, 3000); // 2
                al.addRecord(point);

                point.setTsMillis(ts0 + 2000);
                point.setValue(8, 2000); // 4
                al.addRecord(point);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.valueNum = 3;
            }
        });
    }

    @Test
    public void decimViewStepMin() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                point.setTsMillis(ts0 + 1000);
                point.setStepMillis(1000);
                al.addRecord(point);

                point.setTsMillis(ts0 + 2000);
                point.setStepMillis(2000);
                al.addRecord(point);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.stepMillis = decimStepMillis;
            }
        });
    }

    @Test
    public void decimViewStepMax() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                point.setTsMillis(ts0 + 1000);
                point.setStepMillis(1000_000);
                al.addRecord(point);

                point.setTsMillis(ts0 + 2000);
                point.setStepMillis(2000_000);
                al.addRecord(point);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.stepMillis = 2000_000;
            }
        });
    }

    @Test
    public void decimViewCount() {
        decimViewTest(new DecimViewTemplate() {
            @Override
            public void addPoints(long ts0, AggrGraphDataArrayList al, AggrPoint point) {
                point.setTsMillis(ts0 + 1000);
                point.setCount(100);
                al.addRecord(point);

                point.setTsMillis(ts0 + 2000);
                point.setCount(200);
                al.addRecord(point);
            }

            @Override
            public void setExpected(AggrPointData expected) {
                expected.count = 150;
            }
        });
    }

    @Test
    public void decimNew() {
        long ts0 = Instant.parse("2016-03-26T17:39:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();
        al.addRecordShort(ts0 + 1000, 10);
        al.addRecordShort(ts0 + 2000, 20);

        decim(al, new DecimPolicy(86400 * 1000, 60000), ts0 + 3 * 86400 * 1000);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordShort(ts0, 15);

        Assert.assertEquals(expected, al);
    }

    @Test
    public void decimNewPreservesTsOfSinglePoint() {
        long ts0 = Instant.parse("2017-02-26T17:39:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();
        al.addRecordShort(ts0 + 3600 * 1000, 20);
        al.addRecordShort(ts0 + 3600 * 2000, 10);

        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();
        expected.addRecordShort(timeMillis("2017-02-26T18:00:00.000Z"), 20);
        expected.addRecordShort(timeMillis("2017-02-26T19:00:00.000Z"), 10);

        decim(al, new DecimPolicy(12 * 3600 * 1000, 3600 * 1000), ts0 + 86400 * 1000);

        Assert.assertEquals(expected, al);
    }

    @Test
    public void decimNewHoleInRange() {
        long ts0 = Instant.parse("2017-03-07T12:00:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();
        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        AggrPoint point = new AggrPoint();

        for (int m : new int[] { 0, 2 }) {

            point.setTsMillis(ts0 + m * 60000);
            point.setValue(10, ValueColumn.DEFAULT_DENOM);
            al.addRecord(point);

            point.setTsMillis(ts0 + m * 60000 + 15000);
            point.setValue(20, ValueColumn.DEFAULT_DENOM);
            al.addRecord(point);

            point.setTsMillis(ts0 + m * 60000 + 30000);
            point.setValue(30, ValueColumn.DEFAULT_DENOM);
            al.addRecord(point);

            point.setTsMillis(ts0 + m * 60000 + 45000);
            point.setValue(40, ValueColumn.DEFAULT_DENOM);
            al.addRecord(point);

            point.setTsMillis(ts0 + m * 60000);
            point.setValue(25, ValueColumn.DEFAULT_DENOM);
            expected.addRecord(point);
        }

        decim(al, new DecimPolicy(3600 * 1000, 60 * 1000), ts0 + 86400 * 1000);

        Assert.assertEquals(expected, al);
    }

    @Test
    public void decimMinValueDecim() {
        long ts0 = 0;
        long ts1 = Instant.parse("2017-03-08T12:13:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();
        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        al.addRecordShort(ts0 + 1000, 10);
        al.addRecordShort(ts0 + 2000, 20);

        al.addRecordShort(ts1 + 3000, 30);
        al.addRecordShort(ts1 + 4000, 40);

        expected.addRecordShort(ts1, 35);

        decim(al, new DecimPolicy(86400_000, 60_000), ts1 + 2 * 86400_000);

        Assert.assertEquals(expected, al);
    }

    @Test
    public void decimMinValueKeep() {
        long ts0 = Instant.parse("2000-01-01T00:00:00Z").toEpochMilli();
        long ts1 = Instant.parse("2017-03-08T12:13:00Z").toEpochMilli();

        AggrGraphDataArrayList al = new AggrGraphDataArrayList();
        AggrGraphDataArrayList expected = new AggrGraphDataArrayList();

        al.addRecordShort(ts0 + 1000, 10);

        al.addRecordShort(ts1 + 3000, 30);
        al.addRecordShort(ts1 + 4000, 40);

        expected.addRecordShort(ts0, 10);
        expected.addRecordShort(ts1, 35);

        decim(al, new DecimPolicy(86400_000, 60_000), ts1 + 2 * 86400_000);

        Assert.assertEquals(expected, al);
    }

    @Test
    public void decimLogHistogram() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-12T13:00:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-12T13:00:15Z", LogHistogram.ofBuckets(4, 5, 6)),
            point("2017-05-12T13:00:30Z", LogHistogram.ofBuckets(7, 8, 9, 10)),

            point("2017-05-12T13:30:00Z", LogHistogram.ofBuckets(1, 2, 3)),
            point("2017-05-12T13:30:15Z", LogHistogram.ofBuckets(1, 2)),
            point("2017-05-12T13:30:30Z", LogHistogram.ofBuckets(1, 2, 3)),

            point("2017-05-12T14:30:00Z", LogHistogram.ofBuckets(1, 2, 3))
        );

        DecimPolicy afterOneDayBy30MinutesPolicy = new DecimPolicy(
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.MINUTES.toMillis(30)
        );

        AggrGraphDataArrayList result = source.clone();
        decim(result, afterOneDayBy30MinutesPolicy, timeMillis("2017-05-16T00:00:00Z"));

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2017-05-12T13:00:00Z", LogHistogram.ofBuckets((1 + 4 + 7), (2 + 5 + 8), (3 + 6 + 9), 10)),
            point("2017-05-12T13:30:00Z", LogHistogram.ofBuckets((1 + 1 + 1), (2 + 2 + 2), (3 + 3))),
            point("2017-05-12T14:30:00Z", LogHistogram.ofBuckets(1, 2, 3))
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void decimFirstIntoEpochPoints() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("1970-01-01T00:00:01.000Z", 167),
                point("1970-01-01T00:00:15.000Z", 167),
                point("1970-01-01T00:25:27.000Z", 167),
                point("2018-05-31T09:42:01.000Z", 166),
                point("2018-05-31T10:14:01.000Z", 166));

        DecimPolicy policy = DecimPoliciesPredefined.policyFromProto(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS);

        AggrGraphDataArrayList result = source.clone();
        decim(result, policy, System.currentTimeMillis());


        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("1970-01-01T00:25:00.000Z", 167),
                point("2018-05-31T09:40:00.000Z", 166),
                point("2018-05-31T10:10:00.000Z", 166)
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    public void decimFirstIntoEpochPointsToEmpty() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("1970-01-01T00:00:01.000Z", 167),
                point("1970-01-01T00:00:15.000Z", 167));

        DecimPolicy policy = DecimPoliciesPredefined.policyFromProto(EDecimPolicy.POLICY_5_MIN_AFTER_7_DAYS);

        AggrGraphDataArrayList result = source.clone();
        decim(result, policy, System.currentTimeMillis());

        assertThat(result.length(), equalTo(0));
    }

    private static AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    private static AggrPoint point(String time, LogHistogram histogram) {
        return AggrPoint.shortPoint(timeMillis(time), histogram);
    }

    private static long timeMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }

    private static void decim(AggrGraphDataArrayList arrayList, DecimPolicy policy, long now) {
        MetricType type = StockpileColumns.typeByMask(arrayList.columnSetMask());
        AggrGraphDataListIterator iterator =
                DecimatingAggrGraphDataIterator.of(type, arrayList.iterator(), policy, now);
        int index = 0;
        AggrPoint tmp = new AggrPoint();
        while (iterator.next(tmp)) {
            arrayList.setData(index++, tmp);
        }
        arrayList.truncate(index);
    }
}
