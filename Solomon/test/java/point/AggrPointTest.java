package ru.yandex.solomon.model.point;

import java.util.Objects;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryDoubleSnapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.model.point.column.HistogramColumn;
import ru.yandex.solomon.model.point.column.LongValueColumn;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.StockpileColumnSet;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static ru.yandex.solomon.model.point.AggrPointDataTestSupport.randomPoint;

/**
 * @author Stepan Koltsov
 */
public class AggrPointTest {

    @Test
    public void sameTsAndValueEqual() throws Exception {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.shortPoint(ts, 123);
        AggrPoint point2 = AggrPoint.shortPoint(ts, 123);

        Assert.assertThat(point1, CoreMatchers.equalTo(point2));
    }

    @Test
    public void sameTsAndDiffValueNotEqual() throws Exception {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.shortPoint(ts, 123);
        AggrPoint point2 = AggrPoint.shortPoint(ts, 321);

        Assert.assertThat(point1, CoreMatchers.not(CoreMatchers.equalTo(point2)));
    }

    @Test
    public void sameTsAndLogHistogramEqual() throws Exception {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(1d, 2d, 3d));
        AggrPoint point2 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(1d, 2d, 3d));

        Assert.assertThat(point1, CoreMatchers.equalTo(point2));
    }

    @Test
    public void sameTsAdnDiffLogHistogramNotEqual() throws Exception {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(1d, 2d, 3d));
        AggrPoint point2 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(3d, 2d, 1d));

        Assert.assertThat(point1, CoreMatchers.not(CoreMatchers.equalTo(point2)));
    }

    @Test
    public void hashCodeDiffForDiffHistogram() throws Exception {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(1d, 1d, 2d));
        AggrPoint point2 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(2d, 1d, 1d));

        Assert.assertThat(point1.hashCode(), CoreMatchers.not(CoreMatchers.equalTo(point2.hashCode())));
    }

    @Test
    public void hashCodeSameForSameHistogram() throws Exception {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(1d, 1d, 2d));
        AggrPoint point2 = AggrPoint.shortPoint(ts, LogHistogram.ofBuckets(1d, 1d, 2d));

        Assert.assertThat(point1.hashCode(), CoreMatchers.equalTo(point2.hashCode()));
    }

    @Test
    public void toStringTest() {
        for (int i = 0; i < 50; ++i) {
            AggrPointData data = randomPoint();
            System.out.println(data);
        }
    }

    @Test
    public void buildMultiplePoints() throws Exception {
        AggrPointBuilder builder = AggrPoint.builder().time("2017-06-02T15:34:36Z");
        AggrPoint first = builder.doubleValue(5).build();
        AggrPoint second = builder.doubleValue(10).build();

        Assert.assertThat(first, CoreMatchers.not(CoreMatchers.equalTo(second)));
    }

    @Test
    public void sameTsAndHistogramEqual() {
         var histogram = histogram(new double[]{10, 20, 30}, new long[]{1, 2, 3});
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.builder()
                .time(ts)
                .histogram(histogram)
                .build();
        AggrPoint point2 = AggrPoint.builder()
                .time(ts)
                .histogram(histogram)
                .build();
        assertThat(point1, equalTo(point2));
    }

    @Test
    public void sameTsAndSummaryInt64Equal() {
        SummaryInt64Snapshot summary = new ImmutableSummaryInt64Snapshot(2, 4000, 2, 3998);
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.builder()
                .time(ts)
                .summary(summary)
                .build();
        AggrPoint point2 = AggrPoint.builder()
                .time(ts)
                .summary(summary)
                .build();

        assertThat(point1, equalTo(point2));
    }

    @Test
    public void sameTsAndSummaryInt64NotEqual() {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.builder()
                .time(ts)
                .summary(new ImmutableSummaryInt64Snapshot(2, 4000, 2, 3998))
                .build();
        AggrPoint point2 = AggrPoint.builder()
                .time(ts)
                .summary(new ImmutableSummaryInt64Snapshot(5, 123, 1, 100))
                .build();

        assertThat(point1, not(equalTo(point2)));
    }

    @Test
    public void sameTsAndSummaryDoubleEqual() {
        SummaryDoubleSnapshot summary = new ImmutableSummaryDoubleSnapshot(100, 1000d, 2.5d, 150d);
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.builder()
                .time(ts)
                .summary(summary)
                .build();
        AggrPoint point2 = AggrPoint.builder()
                .time(ts)
                .summary(summary)
                .build();

        assertThat(point1, equalTo(point2));
    }

    @Test
    public void sameTsAndSummaryDoubleNotEqual() {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.builder()
                .time(ts)
                .summary(new ImmutableSummaryDoubleSnapshot(100, 1000d, 2.5d, 150d))
                .build();
        AggrPoint point2 = AggrPoint.builder()
                .time(ts)
                .summary(new ImmutableSummaryDoubleSnapshot(10000, 100500.5d, 5d, 10d))
                .build();

        assertThat(point1, not(equalTo(point2)));
    }

    @Test
    public void sameTsAndDiffHistogramNotEqual() {
        long ts = System.currentTimeMillis();
        AggrPoint point1 = AggrPoint.builder()
                .time(ts)
                .histogram(histogram(new double[]{10, 20, 30}, new long[]{1, 2, 3}))
                .build();
        AggrPoint point2 = AggrPoint.builder()
                .time(ts)
                .histogram(histogram(new double[]{10, 20, 30}, new long[]{3, 2, 1}))
                .build();
        assertThat(point1, not(equalTo(point2)));
    }

    @Test
    public void histogramMask() {
        AggrPoint point = AggrPoint.builder()
                .time(System.currentTimeMillis())
                .histogram(histogram(new double[]{10, 20, 30}, new long[]{3, 2, 1}))
                .build();

        StockpileColumnSet columnSet = new StockpileColumnSet(point.columnSetMask());
        assertThat(columnSet.toString(), columnSet.hasColumn(StockpileColumn.TS), equalTo(true));
        assertThat(columnSet.toString(), columnSet.hasColumn(StockpileColumn.HISTOGRAM), equalTo(true));
        assertThat(columnSet.toString(), columnSet.hasColumn(StockpileColumn.VALUE), equalTo(false));
    }

    @Test
    public void histogramClear() {
        AggrPoint point = AggrPoint.builder()
                .time(System.currentTimeMillis())
                .histogram(histogram(new double[]{10, 20, 30}, new long[]{3, 2, 1}))
                .build();

        point.clearField(StockpileColumn.HISTOGRAM);
        assertThat(point.histogram, equalTo(HistogramColumn.DEFAULT_VALUE));
    }

    @Test
    public void histogramMaskAfterSet() {
        AggrPoint point = new AggrPoint();
        point.setTsMillis(System.currentTimeMillis());
    }

    @Test
    public void testEqualsAllDiff() {
        long ts = System.currentTimeMillis();
        for (int index = 0; index < 100; index++) {
            AggrPoint one = randomPoint();
            one.tsMillis = ts;
            AggrPoint two = randomPoint();
            two.tsMillis = ts;
            if (one.equals(two)) {
                AggrPoint tree = randomPoint(two.columnSet);
                assertNotEquals(one, tree);
                assertNotEquals(two, tree);
            } else {
                assertNotEquals(one, two);
            }
        }
    }

    @Test
    public void testEqualsSame() {
        for (int index = 0; index < 100; index++) {
            AggrPoint one = randomPoint();
            AggrPoint two = one.withMask(one.columnSetMask());
            assertThat(one, equalTo(two));
        }
    }

    @Test
    public void notEqualFull() {
        AggrPoint left = randomPoint(StockpileColumnSet.maxMask);
        AggrPoint right = randomPoint(StockpileColumnSet.maxMask);
        assertNotEquals(left, right);
    }

    @Test
    public void notEqualByColumn() {
        for (StockpileColumn column : StockpileColumn.values()) {
            AggrPoint left = null;
            AggrPoint right = null;
            for (int index = 0; index < 100; index++) {
                left = randomPoint(column.mask());
                right = randomPoint(column.mask());
                if (!Objects.equals(left, right)) {
                    break;
                }
            }

            assertNotEquals(column.name(), left, right);
        }
    }

    @Test
    public void equalFull() {
        AggrPoint left = randomPoint(StockpileColumnSet.maxMask);
        assertEquals(left, left);
    }

    @Test
    public void clearLongValue() {
        long now = System.currentTimeMillis();
        AggrPoint point = new AggrPoint();
        point.setTsMillis(now);
        point.setLongValue(42L);
        assertEquals(42L, point.longValue);
        point.clearField(StockpileColumn.LONG_VALUE);
        assertEquals(LongValueColumn.DEFAULT_VALUE, point.longValue);
        assertEquals(now, point.tsMillis);
    }

    @Test
    public void copyFromCopyTwo() {
        for (StockpileColumn column : StockpileColumn.values()) {
            AggrPoint left = randomPoint(column.mask());
            AggrPoint right = new AggrPoint();
            left.copyTo(right);
            assertEquals(column.name(), left, right);
        }
    }

    @Test
    public void zeroIsValidValueForLongColumn() {
        AggrPoint point = new AggrPoint();
        point.setTsMillis(System.currentTimeMillis());
        point.setLongValue(0);
        assertEquals(StockpileColumn.TS.mask() | StockpileColumn.LONG_VALUE.mask(), point.columnSet);
        assertEquals(0, point.longValue);
    }

    private Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }
}
