package ru.yandex.qe.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.http.retries.ExponentialBackoffStrategy;

/**
 * Established by terry
 * on 26.01.16.
 */
public class ExponentialBackoffStrategyTest {

    @Test
    public void check_interval() {
        final ExponentialBackoffStrategy exponentialBackoffStrategy = new ExponentialBackoffStrategy();
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(0), 0);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(1), 300);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(2), 600);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(3), 1200);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(4), 2400);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(5), 4800);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(6), 9600);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(7), 19200);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(8), 38400);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(9), 60000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(10), 60000);
    }

    @Test
    public void check_interval_scale_factor() {
        final ExponentialBackoffStrategy exponentialBackoffStrategy = new ExponentialBackoffStrategy(1000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(0), 0);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(1), 1000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(2), 2000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(3), 4000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(4), 8000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(5), 16000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(6), 32000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(7), 60000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(8), 60000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(9), 60000);
        Assertions.assertEquals(exponentialBackoffStrategy.delayBeforeNextRetry(10), 60000);
    }
}
