package ru.yandex.solomon.math.stat;


import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vladimir Gordiychuk
 */
public class PercentileInplaceTest {
    private Percentile percentile = new Percentile();
    private PercentileInplace percentileInplace = new PercentileInplace();

    @Test
    public void percentile5() throws Exception {
        double expect = percentile.evaluate(new double[]{15, 20, 35, 40, 50}, 5);
        double result = percentileInplace.evaluate(new double[]{15, 20, 35, 40, 50}, 5);

        Assert.assertEquals(expect, result, 0);
    }

    @Test
    public void percentile75() throws Exception {
        double expect = percentile.evaluate(new double[]{13, 10, 3, 6, 16, 7, 8, 20, 8, 9, 15}, 75);
        double result = percentileInplace.evaluate(new double[]{13, 10, 3, 6, 16, 7, 8, 20, 8, 9, 15}, 75);

        Assert.assertEquals(expect, result, 0);
    }

    @Test
    public void percentile100() throws Exception {
        double expect = percentile.evaluate(new double[]{4, 123, 8, 1, 24, -123, 0, 29, 91, 24, 12, 85}, 100);
        double result = percentileInplace.evaluate(new double[]{4, 123, 8, 1, 24, -123, 0, 29, 91, 24, 12, 85}, 100);

        Assert.assertEquals(expect, result, 0);
    }

    @Test
    public void percentile50() throws Exception {
        double expect = percentile.evaluate(new double[]{82.16, 22.6, 30, 78, 50.2, 31.7, 28.9, 56.2}, 50);
        double result = percentileInplace.evaluate(new double[]{82.16, 22.6, 30, 78, 50.2, 31.7, 28.9, 56.2}, 50);

        Assert.assertEquals(expect, result, 0);
    }
}
