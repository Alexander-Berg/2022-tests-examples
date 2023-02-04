package ru.yandex.solomon.math.operation.comparation;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Vladimir Gordiychuk
 */
public class HistogramComparatorTest {

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void empty() {
        int result = compare(histogram(new double[0], new long[0]), histogram(new double[0], new long[0]));
        assertThat(result, equalTo(0));
    }

    @Test
    public void same() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{1, 2, 3});
        var b = histogram(new double[]{10, 20, 30}, new long[]{1, 2, 3});

        assertThat(compare(a, b), equalTo(0));
        assertThat(compare(b, a), equalTo(0));
    }

    @Test
    public void sameBoundsLessAtRightBound() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{9, 4, 3});
        var b = histogram(new double[]{10, 20, 30}, new long[]{8, 4, 3});

        assertThat(compare(a, b), equalTo(1));
        assertThat(compare(b, a), equalTo(-1));
    }

    @Test
    public void compareFromMaxBounds() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{100, 200, 3});
        var b = histogram(new double[]{10, 20, 30}, new long[]{8, 4, 20});

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void compareDiffBounds() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{5, 1, 0});
        var b = histogram(new double[]{100, 200, 300}, new long[]{3, 5, 0});

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void comparePartlySameBounds() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{0, 2, 8});
        var b = histogram(new double[]{10, 20, 30, 50, 100}, new long[]{1, 2, 3, 0, 0});

        assertThat(compare(a, b), equalTo(1));
        assertThat(compare(b, a), equalTo(-1));
    }

    @Test
    public void comparePartlySameBounds2() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{0, 2, 8});
        var b = histogram(new double[]{10, 20, 30, 50, 100}, new long[]{1, 2, 3, 2, 0});

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void zeroAndWithValues() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{0, 0, 0});
        var b = histogram(new double[]{10, 20, 30, 50, 100}, new long[]{0, 0, 3, 0, 0});

        assertThat(compare(a, b), equalTo(-1));
        assertThat(compare(b, a), equalTo(1));
    }

    @Test
    public void zeroAndWithValues2() {
        var a = histogram(new double[]{10, 20, 30, 50, 100}, new long[]{0, 0, 3, 0, 0});
        var b = histogram(new double[]{10, 20, 30}, new long[]{0, 0, 0});

        assertThat(compare(a, b), equalTo(1));
        assertThat(compare(b, a), equalTo(-1));
    }

    @Test
    public void zeroAndZero() {
        var a = histogram(new double[]{10, 20, 30}, new long[]{0, 0, 0});
        var b = histogram(new double[]{10, 20, 30, 50, 100}, new long[]{0, 0, 0, 0, 0});

        assertThat(compare(a, b), equalTo(0));
        assertThat(compare(b, a), equalTo(0));
    }

    @Test
    public void streamSort() {
        var source = Arrays.asList(
                histogram(new double[]{10, 20, 30}, new long[]{0, 0, 0}),
                histogram(new double[]{10, 20, 30}, new long[]{0, 2, 0}),
                histogram(new double[]{10, 20, 30}, new long[]{3, 0, 1}),
                histogram(new double[]{10, 20, 30}, new long[]{1, 1, 1}),
                histogram(new double[]{10, 20, 30, 50}, new long[]{1, 3, 1, 0}),
                histogram(new double[]{10, 20}, new long[]{10, 2}));

        Collections.shuffle(source);

        Histogram[] expected = {
                histogram(new double[]{10, 20, 30, 50}, new long[]{1, 3, 1, 0}),
                histogram(new double[]{10, 20, 30}, new long[]{1, 1, 1}),
                histogram(new double[]{10, 20, 30}, new long[]{3, 0, 1}),
                histogram(new double[]{10, 20}, new long[]{10, 2}),
                histogram(new double[]{10, 20, 30}, new long[]{0, 2, 0}),
                histogram(new double[]{10, 20, 30}, new long[]{0, 0, 0}),
        };

        Histogram[] result = source.stream()
                .sorted(new HistogramComparator().reversed())
                .toArray(Histogram[]::new);

        Assert.assertArrayEquals(expected, result);
    }

    private int compare(Histogram left, Histogram right) {
        return new HistogramComparator().compare(left, right);
    }
}
