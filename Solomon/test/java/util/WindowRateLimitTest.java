package ru.yandex.solomon.alert.util;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.solomon.ut.ManualClock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class WindowRateLimitTest {

    @Test
    public void limitByDays() {
        WindowRateLimit limit = new WindowRateLimit(10, TimeUnit.DAYS);
        IntStream.range(0, 10)
                .parallel()
                .mapToObj(index -> limit.attempt())
                .forEach(result -> {
                    assertThat(result, equalTo(true));
                });

        assertThat(limit.attempt(), equalTo(false));
        assertThat(limit.attempt(), equalTo(false));
    }

    @Test
    public void limitBySeconds() throws InterruptedException {
        WindowRateLimit limit = new WindowRateLimit(3, TimeUnit.SECONDS);

        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(false));

        TimeUnit.SECONDS.sleep(1L);

        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(false));
    }

    @Test
    public void limitByHours() {
        ManualClock clock = new ManualClock();
        WindowRateLimit limit = new WindowRateLimit(clock, 3, TimeUnit.HOURS);

        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(false));

        clock.passedTime(1, TimeUnit.MINUTES);

        assertThat(limit.attempt(), equalTo(false));
        assertThat(limit.attempt(), equalTo(false));

        clock.passedTime(1, TimeUnit.HOURS);

        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(true));
        assertThat(limit.attempt(), equalTo(false));
    }
}
