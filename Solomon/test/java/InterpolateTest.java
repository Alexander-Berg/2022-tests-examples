package ru.yandex.solomon.math;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class InterpolateTest {

    @Test
    public void linear() {
        Assert.assertEquals(5.0, Interpolate.LINEAR.interpolate(1, 4, 5, 8, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.LINEAR.interpolate(1, 4, 3, Double.NaN, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.LINEAR.interpolate(1, Double.NaN, 3, 6, 2), Double.MIN_VALUE);
    }

    @Test
    public void left() {
        Assert.assertEquals(4.0, Interpolate.LEFT.interpolate(1, 4, 3, 6, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.LEFT.interpolate(1, 4, 3, Double.NaN, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.LEFT.interpolate(1, Double.NaN, 3, 6, 2), Double.MIN_VALUE);
    }

    @Test
    public void right() {
        Assert.assertEquals(6.0, Interpolate.RIGHT.interpolate(1, 4, 3, 6, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.RIGHT.interpolate(1, 4, 3, Double.NaN, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.RIGHT.interpolate(1, Double.NaN, 3, 6, 2), Double.MIN_VALUE);
    }

    @Test
    public void none() {
        Assert.assertEquals(Double.NaN, Interpolate.NONE.interpolate(1, 4, 3, 6, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.NONE.interpolate(1, 4, 3, Double.NaN, 2), Double.MIN_VALUE);
        Assert.assertEquals(Double.NaN, Interpolate.NONE.interpolate(1, Double.NaN, 3, 6, 2), Double.MIN_VALUE);
    }

}
