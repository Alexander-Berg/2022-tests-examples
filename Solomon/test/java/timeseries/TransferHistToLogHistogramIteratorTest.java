package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.LogHistogramColumn;
import ru.yandex.solomon.model.point.column.TsColumn;
import ru.yandex.solomon.model.type.Histogram;
import ru.yandex.solomon.model.type.LogHistogram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Vladimir Gordiychuk
 */
public class TransferHistToLogHistogramIteratorTest {

    @Test
    public void one() {
        var result = convert(Histogram.newInstance(
            new double[]{1, 1.5, 2.25, 3.375},
            new long[]{0, 1, 0, 3}));
        var expected = LogHistogram.newInstance()
            .setBase(1.5)
            .setStartPower(0)
            .setCountZero(0)
            .addBucket(1)
            .addBucket(0)
            .addBucket(3)
            .build();
        assertEquals(expected, result);
    }

    @Test
    public void oneShiftStartPower() {
        var result = convert(Histogram.newInstance(
            new double[]{1.5, 2.25, 3.375},
            new long[]{0, 1, 2}));
        var expected = LogHistogram.newInstance()
            .setBase(1.5)
            .setStartPower(1)
            .setCountZero(0)
            .addBucket(1)
            .addBucket(2)
            .build();
        assertEquals(expected, result);
    }

    @Test
    public void oneManyBuckets() {
        var result = convert(Histogram.newInstance(
            new double[]{
                1.5, 2.25, 3.375,
                86.49755859375, 129.746337890625, 194.6195068359375,
                4987.885095119476, 7481.8276426792145, 11222.741464018822,
                2.120255184830252E12},
            new long[]{
                1, 2, 3,
                4, 5, 6,
                7, 8, 9,
                1}));
        var expected = LogHistogram.newInstance()
            .setBuckets(new double[]{
                1, 2, 3, 0, 0, 0, 0, 0, 0, 0,
                4, 5, 6, 0, 0, 0, 0, 0, 0, 0,
                7, 8, 9, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            })
            .build();
        assertEquals(expected, result);
    }

    @Test
    public void oneWithZeros() {
        var result = convert(Histogram.newInstance(
            new double[]{0.01, 1.5, 2.25, 3.375},
            new long[]{42, 0, 2, 3}));
        var expected = LogHistogram.newInstance()
            .setStartPower(1)
            .setCountZero(42)
            .addBucket(2)
            .addBucket(3)
            .build();
        assertEquals(expected, result);
    }

    private LogHistogram convert(Histogram source) {
        var sourceList = AggrGraphDataArrayList.of(point(System.currentTimeMillis(), source));
        int mask = TsColumn.mask | LogHistogramColumn.mask;
        var it = new TransferHistToLogHistogramIterator(mask, sourceList.iterator());
        var point = RecyclableAggrPoint.newInstance();
        try {
            assertEquals(mask, it.columnSetMask());
            assertTrue(it.next(point));
            assertNotNull(point.logHistogram);
            return LogHistogram.copyOf(point.logHistogram);
        } finally {
            assertFalse(it.next(point));
            point.recycle();
        }
    }
}
