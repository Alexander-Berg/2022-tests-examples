package com.yandex.launcher.testutils;

import com.yandex.launcher.loaders.favicons.FavIconDimens;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by lapaevm on 26.05.16.
 */
public class IconRateTest {

    private int sizeMin;
    private int sizeMax;
    private int sizeTarget;

    @Test
    public void mainTest() {
        printVariants();

        calcDims(100);

        assertLess(38, 100);
        assertLess(38, 75);
        assertLess(75, 100);

        assertLess(16, 32);
        assertLess(82, 99);
        assertLess(82, 105);
        assertLess(125, 105);

        assertLess(37, 38);
        assertLess(37, 80);
    }

    private void assertLess(int lessRateSize, int moreRateSize) {
        float rateLess = rate(lessRateSize);
        float rateMore = rate(moreRateSize);
        Assert.assertTrue(String.format("%.2f < %.2f", rateLess, rateMore), rateLess < rateMore);
    }



    private void calcDims(int dim54dp) {
        sizeMax = dim54dp;
        sizeMin = (int) (dim54dp * 0.38f);
        sizeTarget = FavIconDimens.calcStretchSize(sizeMin, sizeMax);
    }

    private float rate(int size) {
        return FavIconDimens.calculateIconRate(size, sizeMin, sizeMax, sizeTarget);
    }

    private void printVariants() {
        int dens[] = new int[] {54, 83, 100, 142};

        for (int d : dens) {
            calcDims(d);
            System.out.println(String.format("====> icon %d", d));
            for (int i = 1; i < 20; i++) {
                int size = i*10;
                float rate = rate(size);
                System.out.println( String.format("sizes %d | %d | %d - ic %d - %.2f", sizeMin, sizeTarget, sizeMax, size, rate) );
            }
        }
    }

}
