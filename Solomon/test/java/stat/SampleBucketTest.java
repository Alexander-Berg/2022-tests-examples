package ru.yandex.solomon.math.stat;

import java.util.Random;

import org.apache.commons.math3.special.Erf;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ivan Tsybulin
 */
public class SampleBucketTest {

    @Test
    public void emptyBucketTest() {
        SampleBucket sb = new SampleBucket();
        Assert.assertEquals(Double.NaN, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(),  0);
    }

    @Test
    public void singleTest() {
        SampleBucket sb = new SampleBucket();
        sb.add(42);
        Assert.assertEquals(42, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(), 0);
    }

    @Test
    public void noDropTest() {
        SampleBucket sb = new SampleBucket();
        for (int i = 0; i <= 10; i++) {
            sb.add(i);
        }
        Assert.assertEquals(5, sb.getMean(), 1e-15);
        Assert.assertEquals(11, sb.getVariance(), 1e-15);
    }

    @Test
    public void singleWithDropTest() {
        SampleBucket sb = new SampleBucket(0.99); // Drops 99%
        sb.add(42);
        Assert.assertEquals(42, sb.getMean(), 0);
        Assert.assertEquals(Double.NaN, sb.getVariance(), 0);
    }

    @Test
    public void dropTest() {
        SampleBucket sb = new SampleBucket(0.2); // 20%, should drop 1 value from each side

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
    }

    @Test
    public void dropManyTest() {
        SampleBucket sb = new SampleBucket(0.99);

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
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwEverythingTest() {
        new SampleBucket(1);
    }

    @Test
    public void statisticalTest() {
        Random r = new Random(42);
        final double t = 2;
        final double alpha = Erf.erfc(t / Math.sqrt(2));
        SampleBucket sb = new SampleBucket(alpha);

        for (int i = 0; i < 100000; i++) {
            sb.add(r.nextGaussian());
        }

        double expectedMean = 0;
        double expectedVar = 1 - t * Math.sqrt(2 / Math.PI) * Math.exp(-t * t / 2) / (1 - alpha);
        Assert.assertEquals(expectedMean, sb.getMean(), 0.01);
        Assert.assertEquals(expectedVar, sb.getVariance(), 0.001);
    }
}
