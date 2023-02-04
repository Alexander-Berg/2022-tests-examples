package ru.yandex.payments.fnsreg;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Factory
@Requires(property = TestClockFactory.ENABLE_TEST_CLOCK, value = "true")
public class TestClockFactory {
    public static final String ENABLE_TEST_CLOCK = "enable-test-clock";

    @Bean
    @Singleton
    public Clock testClock() {
        return Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
    }
}
