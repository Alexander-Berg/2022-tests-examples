package ru.yandex.solomon.ut;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class ManualScheduledExecutorServiceTest {
    @Test
    public void passTime() throws InterruptedException {
        ManualClock clock = new ManualClock();
        ManualScheduledExecutorService service = new ManualScheduledExecutorService(10, clock);

        CountDownLatch barier = new CountDownLatch(2);
        service.schedule(barier::countDown, 1, TimeUnit.MINUTES);
        service.schedule(barier::countDown, 10, TimeUnit.MINUTES);

        boolean result = barier.await(ThreadLocalRandom.current().nextLong(10, 300), TimeUnit.MILLISECONDS);
        assertThat(result, equalTo(false));

        clock.passedTime(12, TimeUnit.MINUTES);
        assertThat(barier.await(1, TimeUnit.MINUTES), equalTo(true));
    }

    @Test
    public void callWithFixedRate() throws InterruptedException {
        ManualClock clock = new ManualClock();
        ManualScheduledExecutorService service = new ManualScheduledExecutorService(2, clock);

        CountDownLatch barier = new CountDownLatch(2);
        service.scheduleAtFixedRate(barier::countDown, 1, 1, TimeUnit.MINUTES);

        boolean result = barier.await(ThreadLocalRandom.current().nextLong(10, 300), TimeUnit.MILLISECONDS);
        assertFalse(result);

        clock.passedTime(1, TimeUnit.MINUTES);
        clock.passedTime(1, TimeUnit.MINUTES);
        clock.passedTime(1, TimeUnit.MINUTES);
        assertTrue(barier.await(1, TimeUnit.MINUTES));
    }
}
