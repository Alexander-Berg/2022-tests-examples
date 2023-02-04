package ru.yandex.solomon.model.type;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.monlib.metrics.histogram.Histograms;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class MutableHistogramTest {

    private static Histogram dhistogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void mergeToInf() {
        MutableHistogram histogram = new MutableHistogram();
        histogram.addHistogram(dhistogram(new double[]{100, 200, 300}, new long[]{1, 0, 5}));
        histogram.addHistogram(dhistogram(new double[]{10, 20, 30}, new long[]{3, 2, 1}));

        var result = histogram.snapshot();
        var expected = dhistogram(new double[]{10, 20, 30, Histograms.INF_BOUND }, new long[]{3, 2, 1, 6});
        assertThat(result, equalTo(expected));
    }

    @Test
    public void mergeToInfPositiveInf() {
        MutableHistogram histogram = new MutableHistogram();
        histogram.addHistogram(dhistogram(new double[]{100, 200, 300}, new long[]{1, 0, 5}));
        histogram.addHistogram(dhistogram(new double[]{10, 20, Double.POSITIVE_INFINITY}, new long[]{3, 2, 1}));

        var result = histogram.snapshot();
        var expected = dhistogram(new double[]{10, 20, Double.POSITIVE_INFINITY }, new long[]{3, 2, 7});
        assertThat(result, equalTo(expected));
    }

    @Test
    public void mergeSameBounds() {
        MutableHistogram histogram = new MutableHistogram();
        histogram.addHistogram(dhistogram(new double[]{10, 20, 30}, new long[]{1, 0, 5}));
        histogram.addHistogram(dhistogram(new double[]{10, 20, 30}, new long[]{3, 2, 1}));
        histogram.addHistogram(dhistogram(new double[]{10, 20, 30}, new long[]{1, 2, 3}));

        var result = histogram.snapshot();
        var expected = dhistogram(new double[]{10, 20, 30}, new long[]{5, 4, 9});
        assertThat(result, equalTo(expected));
    }

    @Test
    public void maxBucketSizeTruncateLeft() {
        long[] buckets = new long[Histograms.MAX_BUCKETS_COUNT];
        Arrays.fill(buckets, 1);
        long[] expectedBuckets = new long[Histograms.MAX_BUCKETS_COUNT];
        Arrays.fill(expectedBuckets, 2);
        // left bucket truncated
        expectedBuckets[0] = 3;
        expectedBuckets[expectedBuckets.length - 1] = 1;

        double[] boundOne = new double[Histograms.MAX_BUCKETS_COUNT];
        double[] boundTwo = new double[Histograms.MAX_BUCKETS_COUNT];

        for (int index = 0; index < boundOne.length; index++) {
            boundOne[index] = (index + 1) * 10;
            boundTwo[index] = (index + 2) * 10; // left shifted
        }

        MutableHistogram histogram = new MutableHistogram();
        histogram.addHistogram(dhistogram(boundOne, buckets));
        histogram.addHistogram(dhistogram(boundTwo, buckets));

        var snapshot = histogram.snapshot();
        var expected = dhistogram(boundTwo, expectedBuckets);

        assertEquals(expected, snapshot);
    }

    @Test
    public void maxBucketSizeTruncateRight() {
        long[] buckets = new long[Histograms.MAX_BUCKETS_COUNT];
        Arrays.fill(buckets, 1);
        double[] boundOne = new double[Histograms.MAX_BUCKETS_COUNT];
        double[] boundTwo = new double[Histograms.MAX_BUCKETS_COUNT];

        for (int index = 0; index < boundOne.length; index++) {
            boundOne[index] = (index + 2) * 10;
            boundTwo[index] = (index + 1) * 10; // left shifted
        }

        MutableHistogram histogram = new MutableHistogram();
        histogram.addHistogram(dhistogram(boundOne, buckets));
        histogram.addHistogram(dhistogram(boundTwo, buckets));

        double[] expectedBounds = Arrays.copyOf(boundTwo, boundTwo.length);
        long[] expectedBuckets = new long[Histograms.MAX_BUCKETS_COUNT];
        Arrays.fill(expectedBuckets, 2);
        expectedBuckets[0] = 1;
        // right bucket truncated
        expectedBuckets[expectedBuckets.length - 1] = 3;
        expectedBounds[expectedBounds.length - 1] = Histograms.INF_BOUND;

        var snapshot = histogram.snapshot();
        assertEquals(dhistogram(expectedBounds, expectedBuckets), snapshot);
    }

    @Test
    public void mergeDiffBounds() {
        double[] boundsOne = new double[5];
        double[] boundsTwo = new double[5];

        for (int index = 0; index < 5; index++) {
            boundsOne[index] = index + 1;
            boundsTwo[index] = index + 1 + 3;
        }

        long[] buckets = new long[boundsOne.length];
        Arrays.fill(buckets, 1);

        MutableHistogram histogram = new MutableHistogram();
        // {1: 1, 2: 1, 3: 1, 4: 1, 5: 1}
        histogram.addHistogram(dhistogram(boundsOne, buckets));
        // {4: 1, 5: 1, 6: 1, 7: 1, 8: 1}
        histogram.addHistogram(dhistogram(boundsTwo, buckets));

        // {4: 5, 5: 2, 6: 1, 7: 1, 8: 1}
        var expected = dhistogram(boundsTwo, new long[]{5, 2, 1, 1, 1});
        var result = histogram.snapshot();
        assertEquals(result, expected);
    }

    @Test
    public void increaseDenom() {
        var left = dhistogram(new double[]{10, 30, 50}, new long[]{50, 0, 0}).setDenom(5_000);
        var right = dhistogram(new double[]{10, 30, 50}, new long[]{100, 0, 0}).setDenom(10_000);

        var merged = merge(left, right);
        assertNotNull(merged);
        assertEquals(10, left.valueDivided(0), 0);
        assertEquals(10, right.valueDivided(0), 0);
        assertEquals(20, merged.valueDivided(0), 0);
        assertEquals(10_000, merged.getDenom());
    }

    @Test
    public void decreaseDenom() {
        var left = dhistogram(new double[]{10, 30, 50}, new long[]{100, 0, 0}).setDenom(10_000);
        var right = dhistogram(new double[]{10, 30, 50}, new long[]{50, 0, 0}).setDenom(5_000);

        var merged = merge(left, right);
        assertNotNull(merged);
        assertEquals(10, left.valueDivided(0), 0);
        assertEquals(10, right.valueDivided(0), 0);
        assertEquals(20, merged.valueDivided(0), 0);
        assertEquals(5_000, merged.getDenom());
    }

    private Histogram merge(Histogram left, Histogram right) {
        MutableHistogram histogram = new MutableHistogram();
        histogram.addHistogram(left);
        histogram.addHistogram(right);
        return histogram.snapshot();
    }
}
