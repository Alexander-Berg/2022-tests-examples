package ru.yandex.solomon.math.stat;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ivan Tsybulin
 */
public class PartialOrdererTest {

    private static <T extends Throwable> T assertThrows(Class<T> expected, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable actualException) {
            if (expected.isInstance(actualException))
                return (T) actualException;
            else
                throw new AssertionError("Expected " + expected + ", but " + actualException.getClass() + " was thrown");
        }

        throw new AssertionError("No exception was thrown, expected " + expected);
    }

    @Test
    public void emptyTest() {
        double[] values = new double[] {};
        Throwable ex = assertThrows(
            ArrayIndexOutOfBoundsException.class,
            () -> PartialOrderer.reorderOutliers(values, 0, values.length, 0, 0)
        );
        Assert.assertEquals(ex.getMessage(),
            "Invalid upper quantille, high = 0 when slice last = 0");
    }

    @Test
    public void singleTest() {
        double[] values = new double[] { 42. };
        double[] expected = values.clone();
        PartialOrderer.reorderOutliers(values, 0, values.length, 0, 0);
        Assert.assertArrayEquals(values, expected, 0);
    }

    private void orderVerify(double[] vals, double[] orig, int first, int last, int low, int high) {
        double lowPivot = vals[low];
        double highPivot = vals[high];

        for (int i = first; i < low; i++) {
            Assert.assertTrue("Element " + i + " exceeds the lower pivot", vals[i] <= lowPivot);
        }
        for (int i = low+1; i < last; i++) {
            Assert.assertTrue("Element " + i + " is less than the lower pivot", vals[i] >= lowPivot);
        }
        for (int i = first; i < high; i++) {
            Assert.assertTrue("Element " + i + " exceeds the higher pivot", vals[i] <= highPivot);
        }
        for (int i = high+1; i < last; i++) {
            Assert.assertTrue("Element " + i + " is less than the higher pivot", vals[i] >= highPivot);
        }

        double[] sorted = orig.clone();
        double[] valsSorted = vals.clone();
        Arrays.sort(sorted, first, last);
        Arrays.sort(valsSorted, first, last);
        Assert.assertArrayEquals(sorted, valsSorted, 0);

        Assert.assertTrue("The lower quantile is misplaced", lowPivot == sorted[low]);
        Assert.assertTrue("The higher quantille is misplaced", highPivot == sorted[high]);
    }

    @Test
    public void reorderTest() {
        double[] values = new double[100];
        int low = 10;
        int high = 90;
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.cos(100.0 * i * i);
        }
        double[] orig = values.clone();
        PartialOrderer.reorderOutliers(values, 0, values.length, low, high);
        orderVerify(values, orig, 0, values.length, low, high);
    }

    @Test
    public void reorderRegionTest() {
        double[] values = new double[100];
        int first = 10;
        int last = 90;
        int low = 20;
        int high = 80;
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.cos(100.0 * i * i);
        }
        double[] orig = values.clone();
        PartialOrderer.reorderOutliers(values, first, last, low, high);
        orderVerify(values, orig, first, last, low, high);
    }

    @Test
    public void reorderFullRegionTest() {
        double[] values = new double[100];
        int first = 10;
        int last = 90;
        int low = first;
        int high = last-1;
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.cos(100.0 * i * i);
        }
        double[] orig = values.clone();
        PartialOrderer.reorderOutliers(values, first, last, low, high);
        orderVerify(values, orig, first, last, low, high);
    }

}
