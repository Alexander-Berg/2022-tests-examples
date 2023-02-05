package com.yandex.launcher.testutils;

import com.yandex.launcher.common.util.ViewUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by lapaevm on 05.04.16.
 */
public class ViewUtilsInvalidateInfoTest {

    //region ------------------------------------------ Test cases

    @Test
    public void caseNoPause() {
        int num = 72;
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        logNTimes(info, num);
        Assert.assertEquals("total", num, info.getAmountTotal());
        Assert.assertEquals("last", num, info.getAmountLastSec());
        Assert.assertEquals("prev", 0, info.getAmountPrevSec());
    }

    @Test
    public void caseActualize() {
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        int logs[] = {15, 0, 10, 0};
        long[] pauses = {1100, 4000, 1100, 0};
        genericTest(info, logs, pauses);
    }

    @Test
    public void caseActualize2() {
        int num = 333;
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        logNTimes(info, num);
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        info.actualizePeriod();
        Assert.assertEquals("total", num, info.getAmountTotal());
        Assert.assertEquals("last", 0, info.getAmountLastSec());
        Assert.assertEquals("prev", num, info.getAmountPrevSec());
    }

    @Test
    public void caseZero() {
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        int logs[] = {15, 20, 0, 0};
        long[] pauses = {1000, 4000, 100, 0};
        genericTest(info, logs, pauses);
    }

    @Test
    public void caseSmallPause() {
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        int[] logs = {15, 20};
        long[] pause = {1100, 0};
        genericTest(info, logs, pause);
    }

    @Test
    public void caseBigPause() {
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        int[] logs = {50, 100, 3, 52};
        long[] pause = {1500, 2500, 1100, 0};
        genericTest(info, logs, pause);
    }

    @Test
    public void caseCascadePauseOdd() {
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        int[] logs = {5, 10, 15, 20, 3, 12, 9};
        long[] pause = {1100, 2500, 1100, 1200, 1100, 1300, 0};
        genericTest(info, logs, pause);
    }

    @Test
    public void caseCascadePauseEven() {
        ViewUtils.InvalidateInfo info = new ViewUtils.InvalidateInfo();
        int[] logs = {5, 3, 1, 70, 13, 24, 91, 33};
        long[] pause = {1100, 2500, 1100, 1200, 1100, 1100, 1100, 0};
        genericTest(info, logs, pause);
    }

    //endregion

    //region ------------------------------------------ Inner logic methods

    private void genericTest(ViewUtils.InvalidateInfo info, int[] logs, long[] pauses) {
        int sum = 0;
        for (int i : logs) {
            sum += i;
        }
        try {
            for (int i=0; i < logs.length; i++) {
                logNTimes(info, logs[i]);
                Thread.sleep(pauses[i]);
            }
            info.actualizePeriod();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals("total "+info, sum, info.getAmountTotal());
        Assert.assertEquals("last " + info, logs[logs.length - 1], info.getAmountLastSec());
        Assert.assertEquals("prev "+info, logs[logs.length-2], info.getAmountPrevSec());
    }

    private void logNTimes(ViewUtils.InvalidateInfo info, int n) {
        for (int i = 0; i < n; i++) {
            info.log();
        }
    }

    //endregion

}
