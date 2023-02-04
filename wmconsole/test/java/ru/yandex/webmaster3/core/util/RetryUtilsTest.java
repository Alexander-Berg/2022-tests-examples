package ru.yandex.webmaster3.core.util;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author avhaliullin
 */
public class RetryUtilsTest {
    private static final int ATTEMPTS = 5;
    private static final RetryUtils.RetryPolicy INSTANT_POLICY = RetryUtils.instantRetry(ATTEMPTS);

    @Test
    public void queryShouldPassthroughValue() throws InterruptedException {
        Object value = new Object();
        for (int attempts = 0; attempts < ATTEMPTS; attempts++) {
            MutableInt attemptsCountdown = new MutableInt(attempts);
            Object returnedValue = RetryUtils.query(INSTANT_POLICY, () -> {
                if (attemptsCountdown.getValue() > 0) {
                    attemptsCountdown.decrement();
                    throw new RuntimeException();
                } else {
                    return value;
                }
            });
            Assert.assertTrue(value == returnedValue);
        }
    }

    @Test
    public void queryShouldRethrowLastException() throws Exception {
        Exception originalException = new Exception("Test exception");
        try {
            RetryUtils.query(INSTANT_POLICY, () -> {
                throw originalException;
            });
            Assert.fail("No exception were rethrown");
        } catch (Exception e) {
            if (e != originalException) {
                throw e;
            }
        }
    }

    @Test
    public void queryShouldThrowOnInterrupt() throws InterruptedException {
        AtomicReference<Exception> result = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                RetryUtils.query(
                        failedAttempts -> Optional.of(Duration.standardMinutes(1)),
                        () -> {
                            throw new RuntimeException();
                        }
                );
            } catch (Exception e) {
                result.set(e);
            }
        });
        t.start();
        t.interrupt();
        t.join(1000);
        Exception exception = result.get();
        Assert.assertNotNull("Exception should be thrown", exception);
        Assert.assertTrue("Exception should be InterruptedException", exception instanceof InterruptedException);
    }

    @Test
    public void tryExecuteShouldReturnTrueOnSuccess() throws InterruptedException {
        for (int attempts = 0; attempts < ATTEMPTS; attempts++) {
            MutableInt attemptsCountdown = new MutableInt(attempts);
            boolean success = RetryUtils.tryExecute(INSTANT_POLICY, () -> {
                if (attemptsCountdown.getValue() > 0) {
                    attemptsCountdown.decrement();
                    throw new RuntimeException();
                }
            });
            Assert.assertTrue("Should return true when success happening at " + (attempts + 1) + " of " + ATTEMPTS + " attempt", success);
        }
    }

    @Test
    public void tryExecuteShouldReturnFalseWhenNoAttemptsLeft() throws Exception {
        boolean success = RetryUtils.tryExecute(INSTANT_POLICY, () -> {
            throw new RuntimeException();
        });
        Assert.assertFalse("Should return false for always-throwing execution", success);
    }

    @Test
    public void tryExecuteShouldThrowOnInterrupt() throws InterruptedException {
        AtomicReference<Exception> result = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                RetryUtils.tryExecute(
                        failedAttempts -> Optional.of(Duration.standardMinutes(1)),
                        () -> {
                            throw new RuntimeException();
                        }
                );
            } catch (Exception e) {
                result.set(e);
            }
        });
        t.start();
        t.interrupt();
        t.join(1000);
        Exception exception = result.get();
        Assert.assertNotNull("Exception should be thrown", exception);
        Assert.assertTrue("Exception should be InterruptedException", exception instanceof InterruptedException);
    }
}
