package ru.yandex.solomon.model.point.column;

import org.junit.Test;

import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.solomon.model.point.column.HistogramColumn.merge;

/**
 * @author Vladimir Gordiychuk
 */
public class HistogramColumnTest {
    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void mergeHistogramWhenLeftNull() {
        var right = histogram(new double[]{10, 30, 50}, new long[]{1, 0, 5});
        var result = merge(null, right);

        assertThat(result, equalTo(right));
    }

    @Test
    public void mergeHistogramWhenRightNull() {
        var left = histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3});
        var result = merge(left, null);

        assertThat(result, equalTo(left));
    }

    @Test
    public void mergeBothNullHistogram() {
        var result = merge(null, null);
        assertThat(result, nullValue());
    }

    @Test
    public void mergeWithLeftEmptyHistogram() {
        var left = histogram(new double[0], new long[0]);
        var right = histogram(new double[]{1, 5, 10}, new long[]{0, 3, 0});
        var result = merge(left, right);

        assertThat(result, equalTo(right));
    }

    @Test
    public void mergeWithRightEmptyHistogram() {
        var left = histogram(new double[]{1, 5, 10}, new long[]{0, 3, 0});
        var right = histogram(new double[0], new long[0]);
        var result = merge(left, right);

        assertThat(result, equalTo(left));
    }

    @Test
    public void mergeHistogramWithSameBounds() {
        var left = histogram(new double[]{10, 30, 50}, new long[]{1, 2, 3});
        var right = histogram(new double[]{10, 30, 50}, new long[]{5, 1, 4});

        var expected = histogram(new double[]{10, 30, 50}, new long[]{6, 3, 7});
        var result = merge(left, right);

        assertThat(result, equalTo(expected));
    }

    @Test
    public void mergeHistogramWithDifferentBounds() {
        var left = histogram(
            new double[]{10, 30, 50},
            new long[]{1, 2, 3});
        var right = histogram(
            new double[]{10, 20, 30, 40, 50},
            new long[]{1, 2, 3, 4, 5});

        var expected = histogram(
            new double[]{10, 20, 30, 40, 50},
            new long[]{1 + 1, 2, 3 + 2, 4, 5 + 3});

        var result = merge(left, right);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void mergeHistogramRightSmaller() {
        var left = histogram(
                new double[]{10, 30, 50},
                new long[]{1, 2, 3});
        var right = histogram(
                new double[]{1, 2},
                new long[]{1, 1});

        var expected = histogram(
                new double[]{1, 2},
                new long[]{1, 7});

        var result = merge(left, right);
        assertThat(result, equalTo(expected));
    }

    @Test
    public void mergeIncreaseDenom() {
        var left = histogram(new double[]{10, 30, 50}, new long[]{50, 0, 0}).setDenom(5_000);
        var right = histogram(new double[]{10, 30, 50}, new long[]{100, 0, 0}).setDenom(10_000);

        var merged = merge(left, right);
        assertNotNull(merged);
        assertEquals(10, left.valueDivided(0), 0);
        assertEquals(10, right.valueDivided(0), 0);
        assertEquals(12.5, merged.valueDivided(0), 0);
        assertEquals(10_000, merged.getDenom());
    }

    @Test
    public void mergeDecreaseDenom() {
        var left = histogram(new double[]{10, 30, 50}, new long[]{100, 0, 0}).setDenom(10_000);
        var right = histogram(new double[]{10, 30, 50}, new long[]{50, 0, 0}).setDenom(5_000);

        var merged = merge(left, right);
        assertNotNull(merged);
        assertEquals(10, left.valueDivided(0), 0);
        assertEquals(10, right.valueDivided(0), 0);
        assertEquals(50, merged.valueDivided(0), 0);
        assertEquals(5_000, merged.getDenom());
    }

    @Test
    public void mergeIncreaseDenomDiffBuckets() {
        var left = histogram(new double[]{10, 30, 50}, new long[]{50, 0, 0}).setDenom(5_000);
        var right = histogram(new double[]{10, 30, 50, 100}, new long[]{100, 0, 0, 0}).setDenom(10_000);

        var merged = merge(left, right);
        assertNotNull(merged);
        assertEquals(10, left.valueDivided(0), 0);
        assertEquals(10, right.valueDivided(0), 0);
        assertEquals(12.5, merged.valueDivided(0), 0);
        assertEquals(10_000, merged.getDenom());
    }

    @Test
    public void mergeDecreaseDenomDiffBuckets() {
        var left = histogram(new double[]{10, 30, 50}, new long[]{100, 0, 0}).setDenom(10_000);
        var right = histogram(new double[]{10, 30, 50, 100}, new long[]{50, 0, 0, 0}).setDenom(5_000);

        var merged = merge(left, right);
        assertNotNull(merged);
        assertEquals(10, left.valueDivided(0), 0);
        assertEquals(10, right.valueDivided(0), 0);
        assertEquals(50, merged.valueDivided(0), 0);
        assertEquals(5_000, merged.getDenom());
    }
}
