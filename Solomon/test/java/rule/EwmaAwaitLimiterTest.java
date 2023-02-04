package ru.yandex.solomon.alert.rule;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class EwmaAwaitLimiterTest {
    private AwaitLimiter awaitLimiter;
    private Random random;

    @Before
    public void setUp() {
        awaitLimiter = new EwmaAwaitLimiter(4, TimeUnit.SECONDS.toNanos(30));
        random = new Random(42);
    }

    private long nextLong(long bound) {
        return (long)(bound * random.nextDouble());
    }

    @Test
    public void allOkBigAwait() {
        for (int i = 0; i < 1000; i++) {
            long elapsed = TimeUnit.SECONDS.toNanos(3) + nextLong(TimeUnit.SECONDS.toNanos(2));
            assertThat(awaitLimiter.availableWaitNanos(), greaterThan(TimeUnit.SECONDS.toNanos(30)));
            awaitLimiter.update(elapsed);
        }
    }

    @Test
    public void failAndRecover() {
        for (int i = 0; i < 10; i++) {
            long elapsed = TimeUnit.SECONDS.toNanos(45) + nextLong(TimeUnit.SECONDS.toNanos(30));
            awaitLimiter.update(elapsed);
        }

        assertThat(awaitLimiter.availableWaitNanos(), equalTo(0L));

        for (int i = 0; i < 10; i++) {
            long elapsed = TimeUnit.SECONDS.toNanos(10) + nextLong(TimeUnit.SECONDS.toNanos(5));
            awaitLimiter.update(elapsed);
        }

        assertThat(awaitLimiter.availableWaitNanos(), greaterThan(TimeUnit.SECONDS.toNanos(10)));
    }

    @Test
    public void periodicallyFail() {
        long SECOND = TimeUnit.SECONDS.toNanos(1);
        int failed = 0;
        for (int i = 0; i < 10000; i++) {
            long elapsed;
            long await = awaitLimiter.availableWaitNanos();
            if (random.nextDouble() < 0.05) {
                elapsed = 30 * SECOND + nextLong(30 * SECOND);
            } else {
                elapsed = nextLong(10 * SECOND);
            }
            if (elapsed > await) {
                failed++;
            }
            awaitLimiter.update(elapsed);
        }

        assertThat(failed, lessThan(5));
    }

    @Test
    public void failOftenButNotDegradeOverall() {
        long SECOND = TimeUnit.SECONDS.toNanos(1);
        long totalElapsed = 0;
        long totalUnlimited = 0;
        int total = 10000;
        for (int i = 0; i < total; i++) {
            long elapsed;
            long await = awaitLimiter.availableWaitNanos();
            if (random.nextBoolean()) {
                elapsed = 45 * SECOND + nextLong(30 * SECOND);
            } else {
                elapsed = nextLong(10 * SECOND);
            }
            totalElapsed += Math.min(elapsed, await);
            totalUnlimited += elapsed;
            awaitLimiter.update(elapsed);
        }

        double avgElapsed = 1.0 * totalElapsed / total;
        double avgUnlimited = 1.0 * totalUnlimited / total;
        assertThat(avgElapsed, lessThan(15d * SECOND));
        assertThat(avgUnlimited, greaterThan(30d * SECOND));
    }
}
