package ru.yandex.infra.stage;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.util.AdaptiveRateLimiterImpl;
import ru.yandex.infra.stage.util.SettableClock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

public class AdaptiveRateLimiterImplTest {
    private static final String[] DefaultConfig = new String[]{
            "enabled = true",
            "activate_after_start = false",
            "default_rps = 50",
            "min_rps = 1",
            "max_rps = 10000",
            "review_interval = 10s",
            "rate_up_multiplier = 1.1",
            "rate_down_multiplier = 0.5",
            "auto_shutdown_interval = 10m"};

    private static final SettableClock CLOCK = new SettableClock();

    @Test
    public void startDisabled(){
        var limiter = getLimiter(ImmutableMap.of("activate_after_start", "false"));

        assertThat(limiter.isActive(), equalTo(false));
        assertThat(limiter.getRate(), equalTo(Double.NaN));
        assertThat(limiter.tryAcquire(), equalTo(true));
    }

    @Test
    public void startEnabled() {
        var limiter = getLimiter(ImmutableMap.of("activate_after_start", "true",
                                                 "default_rps", "31"));
        assertThat(limiter.isActive(), equalTo(true));
        assertThat(limiter.getRate(), equalTo(31.0));
        assertThat(limiter.tryAcquire(), equalTo(true));
    }

    @Test
    public void ignoreLimitsWithoutFailedRequest() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10"));

        for (int i = 0; i < 15; i++) {
            assertThat(limiter.tryAcquire(), equalTo(true));//Still can process request after 10th request
        }
        assertThat(limiter.isActive(), equalTo(false));
    }

    @Test
    public void activateAfterFailedRequest() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10"));

        assertThat(limiter.getRate(), equalTo(Double.NaN));
        assertThat(limiter.isActive(), equalTo(false));

        limiter.registerFailedResponse();
        assertThat(limiter.getRate(), equalTo(10.0));
        assertThat(limiter.isActive(), equalTo(true));
    }

    @Test
    public void activateAfterEachFailedRequest() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10"));

        assertThat(limiter.isActive(), equalTo(false));

        limiter.registerFailedResponse();
        assertThat(limiter.isActive(), equalTo(true));

        CLOCK.moveTime(Duration.ofHours(1));
        assertThat(limiter.isActive(), equalTo(true));
        limiter.tryAcquire();
        assertThat(limiter.isActive(), equalTo(false));

        limiter.registerFailedResponse();
        assertThat(limiter.isActive(), equalTo(true));
    }

    @Test
    public void tryAcquireLimit10() {
        var limiter = getLimiter(ImmutableMap.of("activate_after_start", "true",
                                                    "default_rps", "10"));

        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(), equalTo(true));
        }
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire(), equalTo(false));
        }
    }

    @Test
    public void renewLimitsAfterDelay() {
        var limiter = getLimiter(ImmutableMap.of("activate_after_start", "true",
                                                "default_rps", "10"));

        for (int i = 0; i < 15; i++) {
            limiter.tryAcquire();
        }
        assertThat(limiter.tryAcquire(), equalTo(false));

        CLOCK.incrementSecond();

        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(), equalTo(true));
        }
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire(), equalTo(false));
        }
    }

    @Test
    public void autoShutdownNotActivated() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10",
                                                 "review_interval", "500s",
                                                 "auto_shutdown_interval", "1m"));

        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire();
            if(i % 10 == 0) {
                limiter.registerFailedResponse();
            }
            CLOCK.incrementSecond();
        }

        assertThat(limiter.isActive(), equalTo(true));
    }

    @Test
    public void autoShutdownActivated() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10",
                                                "auto_shutdown_interval", "1m"));

        limiter.registerFailedResponse();

        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire();
            assertThat(limiter.isActive(), equalTo(i <= 60));

            CLOCK.incrementSecond();
        }

        assertThat(limiter.isActive(), equalTo(false));
    }

    @Test
    public void resetToDefault() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10",
                "review_interval", "10s",
                "auto_shutdown_interval", "1m"));

        limiter.registerFailedResponse();

        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire();
            assertThat(limiter.isActive(), equalTo(i <= 60));

            CLOCK.incrementSecond();
        }

        assertThat(limiter.isActive(), equalTo(false));
    }

    @Test
    public void rateChange() {
        var limiter = getLimiter(ImmutableMap.of("activate_after_start", "true",
                                                "default_rps", "10",
                                                "review_interval", "10s",
                                                "rate_up_multiplier", "1.2",
                                                "rate_down_multiplier","0.5"));

        assertThat(limiter.getRate(), equalTo(10.0));
        limiter.tryAcquire();
        CLOCK.moveTime(Duration.ofSeconds(2));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), equalTo(10.0));

        CLOCK.moveTime(Duration.ofSeconds(7));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), equalTo(10.0));

        CLOCK.moveTime(Duration.ofSeconds(1));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), equalTo(12.0));

        CLOCK.moveTime(Duration.ofSeconds(1));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), equalTo(12.0));

        CLOCK.moveTime(Duration.ofSeconds(9));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), closeTo(14.4, 0.01));

        CLOCK.moveTime(Duration.ofSeconds(10));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), closeTo(17.28, 0.01));

        CLOCK.moveTime(Duration.ofSeconds(10));
        limiter.registerFailedResponse();
        limiter.tryAcquire();
        assertThat(limiter.getRate(), closeTo(8.64, 0.01));
    }

    @Test
    public void rateMinMax() {
        var limiter = getLimiter(ImmutableMap.of("default_rps", "10",
                                                "rate_up_multiplier", "1.2",
                                                "rate_down_multiplier","0.5",
                                                "min_rps","5",
                                                "max_rps", "25"));

        limiter.registerFailedResponse();
        assertThat(limiter.getRate(), equalTo(10.0));

        for (int i = 0; i < 500; i++) {
            limiter.tryAcquire();
            CLOCK.incrementSecond();
        }

        assertThat(limiter.getRate(), equalTo(25.0));

        for (int i = 0; i < 500; i++) {
            limiter.tryAcquire();
            limiter.registerFailedResponse();
            CLOCK.incrementSecond();
        }

        assertThat(limiter.getRate(), equalTo(5.0));
    }

    @Test
    public void rateMinMax2() {
        var limiter = getLimiter(toMap(new String[]{"default_rps = 10",
                                                "rate_up_multiplier = 10",
                                                "rate_down_multiplier = 0.01",
                                                "min_rps = 5",
                                                "max_rps = 25",
                                                "review_interval = 10s"
        }));

        limiter.registerFailedResponse();
        assertThat(limiter.getRate(), equalTo(10.0));

        CLOCK.moveTime(Duration.ofSeconds(11));
        limiter.tryAcquire();
        assertThat(limiter.getRate(), equalTo(25.0));

        CLOCK.moveTime(Duration.ofSeconds(11));
        limiter.registerFailedResponse();
        limiter.tryAcquire();
        assertThat(limiter.getRate(), equalTo(5.0));
    }

    @Test
    public void maxConcurrentRequests() {
        var limiter = getLimiter(ImmutableMap.of("activate_after_start", "true",
                "default_rps", "10",
                "review_interval", "1s",
                "max_concurrent_requests", "2"));

        assertThat(limiter.getActiveRequestsCount(), equalTo(0));

        assertThat(limiter.incrementAndGet(), equalTo(1));
        assertThat(limiter.getActiveRequestsCount(), equalTo(1));
        assertThat(limiter.tryAcquire(), equalTo(true));
        assertThat(limiter.tryAcquire(), equalTo(true));
        assertThat(limiter.tryAcquire(), equalTo(true));

        assertThat(limiter.incrementAndGet(), equalTo(2));
        assertThat(limiter.getActiveRequestsCount(), equalTo(2));
        assertThat(limiter.tryAcquire(), equalTo(false));

        CLOCK.moveTime(Duration.ofSeconds(100));
        assertThat(limiter.tryAcquire(), equalTo(false));
        assertThat(limiter.getActiveRequestsCount(), equalTo(2));

        assertThat(limiter.decrementAndGet(), equalTo(1));
        assertThat(limiter.getActiveRequestsCount(), equalTo(1));
        assertThat(limiter.tryAcquire(), equalTo(true));
    }


    private static Map<String, String> toMap(String[] values) {
        return Arrays.stream(values)
                     .map(line -> line.split("="))
                     .collect(Collectors.toMap(a -> a[0].trim(), a -> a[1].trim()));
    }

    private static AdaptiveRateLimiterImpl getLimiter(Map<String, String> configValues) {
        return getLimiter(configValues, CLOCK);
    }

    public static AdaptiveRateLimiterImpl getLimiter(Map<String, String> configValues, SettableClock clock) {
        var config = ConfigFactory.empty()
                                  .withValue("config", ConfigValueFactory.fromMap(toMap(DefaultConfig)))
                                  .getConfig("config");

        if(configValues != null) {
            for (var pair: configValues.entrySet()) {
                config = config.withValue(pair.getKey(), ConfigValueFactory.fromAnyRef(pair.getValue()));
            }
        }

        return new AdaptiveRateLimiterImpl(clock, config);
    }
}
