package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.solomon.model.point.RecyclableAggrPoint;
import ru.yandex.solomon.model.point.column.HistogramColumn;
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
public class TransferLogHistogramToUgramIteratorTest {

    @Test
    public void empty() {
        Histogram result = convert(LogHistogram.newInstance());
        assertEquals(Histogram.newInstance(), result);
    }

    @Test
    public void one() {
        Histogram result = convert(LogHistogram.newInstance()
            .setBase(1.5)
            .setStartPower(0)
            .setCountZero(0)
            .addBucket(1)
            .addBucket(0)
            .addBucket(3)
            .build());
        Histogram expected = Histogram.newInstance(
            new double[]{1, 1.5, 2.25, 3.375},
            new long[]{0, 1, 0, 3});
        assertEquals(expected, result);
    }

    @Test
    public void fewSameBounds() {
        var source = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(0)
                .setCountZero(0)
                .addBucket(1)
                .addBucket(0)
                .addBucket(3)
                .build()),
            point("2020-02-17T13:40:05Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(0)
                .setCountZero(0)
                .addBucket(2)
                .addBucket(2)
                .addBucket(2)
                .build()),
            point("2020-02-17T13:40:10Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(0)
                .setCountZero(0)
                .addBucket(3)
                .addBucket(3)
                .addBucket(3)
                .build()));

        var expected = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 1, 0, 3})),
            point("2020-02-17T13:40:05Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 2, 2, 2})),
            point("2020-02-17T13:40:10Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 3, 3, 3}))
        );

        AggrGraphDataArrayList result = convert(source);
        assertEquals(expected, result);
    }

    @Test
    public void fewBoundsAlighted() {
        var source = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(0)
                .addBucket(10)
                .build()),
            point("2020-02-17T13:40:05Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(1)
                .addBucket(20)
                .build()),
            point("2020-02-17T13:40:10Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(2)
                .addBucket(42)
                .build()));

        var expected = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 10, 0, 0})),
            point("2020-02-17T13:40:05Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 0, 20, 0})),
            point("2020-02-17T13:40:10Z", Histogram.newInstance(new double[]{1, 1.5, 2.25, 3.375}, new long[]{0, 0, 0, 42}))
        );

        AggrGraphDataArrayList result = convert(source);
        assertEquals(expected, result);
    }

    @Test
    public void fewAlreadyDecim() {
        var source = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(0)
                .addBucket(10)
                .build()),
            point("2020-02-17T13:45:00Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(1)
                .addBucket(20)
                .build()),
            point("2020-02-17T13:50:00Z", LogHistogram.newInstance()
                .setBase(1.5)
                .setStartPower(2)
                .addBucket(42)
                .build()));

        var expected = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", Histogram.newInstance(new double[]{1, 1.5}, new long[]{0, 10})),
            point("2020-02-17T13:45:00Z", Histogram.newInstance(new double[]{1.5, 2.25}, new long[]{0, 20})),
            point("2020-02-17T13:50:00Z", Histogram.newInstance(new double[]{2.25, 3.375}, new long[]{0, 42}))
        );

        AggrGraphDataArrayList result = convert(source);
        assertEquals(expected, result);
    }

    @Test
    public void negativeStartPower() {
        var source = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", LogHistogram.newInstance()
                .setBase(2)
                .setStartPower(-2)
                .addBucket(1)
                .addBucket(1)
                .addBucket(1)
                .build()),
            point("2020-02-17T13:40:05Z", LogHistogram.newInstance()
                .setBase(2)
                .setStartPower(-1)
                .addBucket(2)
                .addBucket(2)
                .addBucket(2)
                .build()),
            point("2020-02-17T13:40:10Z", LogHistogram.newInstance()
                .setBase(2)
                .setStartPower(0)
                .addBucket(3)
                .addBucket(3)
                .addBucket(3)
                .build()));

        // Negative start power necessary when user want to monitor number less then Math.pow(base, 0)
        var expected = AggrGraphDataArrayList.of(
            point("2020-02-17T13:40:00Z", Histogram.newInstance(new double[]{0.25, 0.5, 1, 2, 4, 8}, new long[]{0, 1, 1, 1, 0, 0})),
            point("2020-02-17T13:40:05Z", Histogram.newInstance(new double[]{0.25, 0.5, 1, 2, 4, 8}, new long[]{0, 0, 2, 2, 2, 0})),
            point("2020-02-17T13:40:10Z", Histogram.newInstance(new double[]{0.25, 0.5, 1, 2, 4, 8}, new long[]{0, 0, 0, 3, 3, 3}))
        );

        AggrGraphDataArrayList result = convert(source);
        assertEquals(expected, result);
    }

    @Test
    public void oneShiftStartPower() {
        Histogram result = convert(LogHistogram.newInstance()
            .setBase(1.5)
            .setStartPower(1)
            .setCountZero(0)
            .addBucket(1)
            .addBucket(2)
            .build());
        Histogram expected = Histogram.newInstance(
            new double[]{1.5, 2.25, 3.375},
            new long[]{0, 1, 2});
        assertEquals(expected, result);
    }

    @Test
    public void oneDiffBase() {
        Histogram result = convert(LogHistogram.newInstance()
            .setStartPower(32)
            .setBase(2)
            .setCountZero(0)
            .addBucket(623)
            .addBucket(96)
            .addBucket(1)
            .build());
        Histogram expected = Histogram.newInstance(
            new double[]{4294967296.0, 8589934592.0, 17179869184.0, 34359738368.0},
            new long[]{0, 623, 96, 1});
        assertEquals(expected, result);
    }

    @Test
    public void oneManyBuckets() {
        Histogram result = convert(LogHistogram.newInstance()
            .setBase(2)
            .setBuckets(new double[]{
                1, 2, 3, 0, 0, 0, 0, 0, 0, 0,
                4, 5, 6, 0, 0, 0, 0, 0, 0, 0,
                7, 8, 9, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            })
            .build());
        Histogram expected = Histogram.newInstance(
            new double[]{
                2, 4, 8,
                2048, 4096, 8192,
                2097152, 4194304, 8388608,
                1.1805916207174113E21},
            new long[]{
                1, 2, 3,
                4, 5, 6,
                7, 8, 9,
                1});
        assertEquals(expected, result);
    }

    @Test
    public void oneWithZeros() {
        Histogram result = convert(LogHistogram.newInstance()
            .setStartPower(1)
            .setCountZero(42)
            .addBucket(2)
            .addBucket(3)
            .build());
        Histogram expected = Histogram.newInstance(
            new double[]{0.01, 1.5, 2.25, 3.375},
            new long[]{42, 0, 2, 3});
        assertEquals(expected, result);
    }

    private Histogram convert(LogHistogram source) {
        var sourceList = AggrGraphDataArrayList.of(point(System.currentTimeMillis(), source));
        int mask = TsColumn.mask | HistogramColumn.mask;
        var it = TransferLogHistogramToUgramIterator.of(mask, sourceList.iterator());
        var point = RecyclableAggrPoint.newInstance();
        try {
            assertEquals(mask, it.columnSetMask());
            assertTrue(it.next(point));
            assertNotNull(point.histogram);
            return Histogram.copyOf(point.histogram);
        } finally {
            assertFalse(it.next(point));
            point.recycle();
        }
    }

    private AggrGraphDataArrayList convert(AggrGraphDataArrayList source) {
        int mask = TsColumn.mask | HistogramColumn.mask;
        var it = TransferLogHistogramToUgramIterator.of(mask, source.iterator());
        return AggrGraphDataArrayList.of(it);
    }
}
