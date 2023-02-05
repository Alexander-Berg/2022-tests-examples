package com.yandex.autotests;

import com.yandex.autotests.fakes.Math;
import com.yandex.autotests.runner.device.DeviceJUnit4Runner;
import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

@RunWith(DeviceJUnit4Runner.class)
public class MathTest {

    @TestCaseId(6)
    @Test
    public void testAdd() {
        assertEquals(4, Math.add(2, 2));
    }

    @TestCaseId(11)
    @Test
    public void testAddDelayed() throws InterruptedException {
        final AtomicReference<Integer> sum = new AtomicReference<>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Math.addDelayed(42, 41, integer -> {
            sum.set(integer);
            countDownLatch.countDown();
        });
        countDownLatch.await();
        assertEquals(83, sum.get().intValue());
    }
}
