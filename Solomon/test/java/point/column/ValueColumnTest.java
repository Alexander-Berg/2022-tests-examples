package ru.yandex.solomon.model.point.column;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class ValueColumnTest {

    @Test
    public void divide() {
        Assert.assertEquals(3, ValueColumn.divide(3, 0), 0);
        Assert.assertEquals(3, ValueColumn.divide(6, 2000), 0);
    }

    private void testMerge(double en, long ed, double an, long ad, double bn, long bd) {
        double mn = ValueColumn.merge0(an, ad, bn, bd);
        long md = ValueColumn.merge1(an, ad, bn, bd);
        Assert.assertEquals(new ValueObject(en, ed), new ValueObject(mn, md));
    }

    @Test
    public void merge() {
        testMerge(5, 0, 3, 0, 2, 0);
        testMerge(5, 1000, 3, 1000, 2, 1000);
        testMerge(5, 0, 3, 1000, 4, 2000);

        testMerge(Double.NaN, ValueColumn.DEFAULT_DENOM, Double.NaN, 100, Double.NaN, 200);
        testMerge(Double.NaN, ValueColumn.DEFAULT_DENOM, Double.NaN, 100, Double.NaN, 100);

        testMerge(300, 5000, Double.NaN, 6000, 300, 5000);
        testMerge(300, 5000, 300, 5000, Double.NaN, 6000);

        testMerge(300, 5000, 300, 5000, Double.NaN, ValueColumn.DEFAULT_DENOM);
        testMerge(300, 5000, Double.NaN, ValueColumn.DEFAULT_DENOM, 300, 5000);
    }

}
