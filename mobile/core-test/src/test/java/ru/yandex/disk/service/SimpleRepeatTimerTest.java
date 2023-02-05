package ru.yandex.disk.service;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

public class SimpleRepeatTimerTest {

    @Test
    public void shouldIncreaseExponentially() throws Exception {
        SimpleRepeatTimer timer
                = new SimpleRepeatTimer(new ReturnIntervalCenterRandom(), 1000, 10000);

        assertThat(timer.getDelay(0), equalTo(1000L));
        assertThat(timer.getDelay(1), equalTo(2000L));
        assertThat(timer.getDelay(2), equalTo(4000L));
    }

    @Test
    public void shouldReturnPlusMinusHalf() throws Exception {
        final AtomicInteger randomValue = new AtomicInteger();
        Random pseudoRandom = new Random() {
            @Override
            public int nextInt(int n) {
                return randomValue.get();
            }
        };
        SimpleRepeatTimer timer = new SimpleRepeatTimer(pseudoRandom, 1000, 10000);

        assertThat(timer.getDelay(0), equalTo(500L));

        randomValue.set(1000);
        assertThat(timer.getDelay(0), equalTo(1500L));
    }

    @Test
    public void shouldReturnNotMoreThenMax() throws Exception {
        SimpleRepeatTimer timer =
                new SimpleRepeatTimer(new ReturnIntervalCenterRandom(), 1000, 10000);

        assertThat(timer.getDelay(5), lessThan(10000L + 1));
    }

    @Test
    public void shouldLimitAttemptCount() throws Exception {
        Random random = new Random();
        SimpleRepeatTimer timer = new SimpleRepeatTimer(random, 1000, 10000);
        assertThat(timer.getDelay(100), equalTo(10000L));
    }

    @Test
    public void shouldLimitAttemptCountWithBigNumbers() throws Exception {
        Random random = new Random();
        SimpleRepeatTimer timer = new SimpleRepeatTimer(random, Integer.MAX_VALUE / 100,
                Integer.MAX_VALUE / 2);
        assertThat(timer.getDelay(100), equalTo((long) Integer.MAX_VALUE / 2));
    }

    private static class ReturnIntervalCenterRandom extends Random {
        @Override
        public int nextInt(int n) {
            return n / 2;
        }
    }
}