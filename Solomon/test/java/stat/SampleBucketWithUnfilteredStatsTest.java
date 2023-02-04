package ru.yandex.solomon.math.stat;

import java.util.Random;

import org.apache.commons.math3.special.Erf;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ivan Tsybulin
 */
public class SampleBucketWithUnfilteredStatsTest {

    @Test
    public void emptyBucketTest() {
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats();
        Assert.assertEquals(Double.NaN, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(),  0);
        Assert.assertEquals(Double.NaN, sb.getUnfilteredMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getUnfilteredVariance(),  0);
    }

    @Test
    public void singleTest() {
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats();
        sb.add(42);
        Assert.assertEquals(42, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(), 0);
        Assert.assertEquals(42, sb.getUnfilteredMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getUnfilteredVariance(), 0);
    }

    @Test
    public void noDropTest() {
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats();
        for (int i = 0; i <= 10; i++) {
            sb.add(i);
        }
        Assert.assertEquals(5, sb.getMean(), 1e-15);
        Assert.assertEquals(11, sb.getVariance(), 1e-15);
        Assert.assertEquals(sb.getMean(), sb.getUnfilteredMean(), 1e-15);
        Assert.assertEquals(sb.getVariance(), sb.getUnfilteredVariance(), 1e-15);
    }

    @Test
    public void singleWithDropTest() {
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats(0.99); // Drops 99%
        sb.add(42);
        Assert.assertEquals(42, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(), 0);
        Assert.assertEquals(42, sb.getUnfilteredMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getUnfilteredVariance(), 0);
    }

    @Test
    public void dropTest() {
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats(0.2); // 20%, should drop 1 value from each side

        sb.add(2);
        sb.add(3);
        sb.add(1);
        sb.add(0);
        sb.add(8);
        sb.add(9);
        sb.add(6);
        sb.add(7);
        sb.add(5);
        sb.add(10);
        sb.add(4);

        Assert.assertEquals(5, sb.getMean(), 1e-15);
        Assert.assertEquals(7.5, sb.getVariance(), 1e-15);
        Assert.assertEquals(5, sb.getUnfilteredMean(), 1e-15);
        Assert.assertEquals(11, sb.getUnfilteredVariance(), 1e-15);
    }

    @Test
    public void dropManyTest() {
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats(0.99);

        sb.add(2);
        sb.add(3);
        sb.add(1);
        sb.add(0);
        sb.add(8);
        sb.add(9);
        sb.add(6);
        sb.add(7);
        sb.add(5);
        sb.add(10);
        sb.add(4);

        Assert.assertEquals(5, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(), 0);
        Assert.assertEquals(5, sb.getUnfilteredMean(), 1e-15);
        Assert.assertEquals(11, sb.getUnfilteredVariance(), 1e-15);
    }

    @Test
    public void statisticalTest() {
        Random r = new Random(42);
        final double t = 2;
        final double alpha = Erf.erfc(t / Math.sqrt(2));
        SampleBucketWithUnfilteredStats sb = new SampleBucketWithUnfilteredStats(alpha);

        for (int i = 0; i < 100000; i++) {
            sb.add(r.nextGaussian());
        }

        double expectedMean = 0;
        double expectedVar = 1 - t * Math.sqrt(2 / Math.PI) * Math.exp(-t * t / 2) / (1 - alpha);
        Assert.assertEquals(expectedMean, sb.getMean(), 0.01);
        Assert.assertEquals(expectedVar, sb.getVariance(), 0.001);

        Assert.assertEquals(0, sb.getUnfilteredMean(), 0.01);
        Assert.assertEquals(1, sb.getUnfilteredVariance(), 0.001);
    }
}
