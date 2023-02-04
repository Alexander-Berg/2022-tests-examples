package ru.yandex.solomon.model.type;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class HistogramTest {

    @Test
    public void denom() {
        var histogram = Histogram.newInstance().setUpperBound(0, 200)
            .setBucketValue(0, 4200)
            .setDenom(10_000);

        assertEquals(10_000, histogram.getDenom());
        assertEquals(4200, histogram.value(0));
        assertEquals(420.0, histogram.valueDivided(0), 0);
    }

    @Test
    public void copyDenomToo() {
        var histogram = Histogram.newInstance()
            .setUpperBound(0, 100)
            .setBucketValue(0, 42)
            .setDenom(15_000);

        assertEquals(15_000, histogram.getDenom());
        var copy = Histogram.copyOf(histogram);
        assertEquals(15_000, copy.getDenom());
    }

    @Test
    public void noDenom() {
        var histogram = Histogram.newInstance()
            .setUpperBound(0, 100)
            .setBucketValue(0, 42);

        assertEquals(0, histogram.getDenom());
        assertEquals(42, histogram.value(0));
        assertEquals(42.0, histogram.valueDivided(0), 0);
    }
}
