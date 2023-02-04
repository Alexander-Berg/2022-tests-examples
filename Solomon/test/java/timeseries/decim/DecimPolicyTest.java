package ru.yandex.solomon.model.timeseries.decim;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class DecimPolicyTest {

    @Test
    public void empty() {
        var policy = DecimPolicy.EMPTY;
        assertEquals(0, policy.getDecimBefore(System.currentTimeMillis()));
        assertEquals(0, policy.getDecimFrom(System.currentTimeMillis()));
    }

    @Test
    public void decimBefore() {
        var now = Instant.parse("2019-08-10T14:52:21Z");
        var exp = Instant.parse("2019-08-03T14:50:00Z");

        {
            var policy = DecimPolicy.newBuilder()
                .addPolicy(Duration.ofDays(7), Duration.ofMinutes(10))
                .build();

            long decimBefore = policy.getDecimBefore(now.toEpochMilli());
            assertEquals(exp, Instant.ofEpochMilli(decimBefore));
        }

        {
            var policy = DecimPolicy.newBuilder()
                .addPolicy(Duration.ofDays(7), Duration.ofMinutes(10))
                .addPolicy(Duration.ofDays(30), Duration.ofMinutes(30))
                .build();

            long decimBefore = policy.getDecimBefore(now.toEpochMilli());
            assertEquals(exp, Instant.ofEpochMilli(decimBefore));
        }
    }

    @Test
    public void decimFromOnePolicy() {
        var decimatedAt = Instant.parse("2019-08-10T14:52:21Z");
        var expected    = Instant.parse("2019-08-03T14:50:00Z");

        var policy = DecimPolicy.newBuilder()
            .addPolicy(Duration.ofDays(7), Duration.ofMinutes(10))
            .build();

        long from = policy.getDecimFrom(decimatedAt.toEpochMilli());
        assertEquals(expected, Instant.ofEpochMilli(from));
    }

    @Test
    public void decimFromManyPolicy() {
        var decimatedAt = Instant.parse("2019-08-24T14:52:21Z");
        var expected    = Instant.parse("2019-08-10T14:00:00Z");

        var policy = DecimPolicy.newBuilder()
            .addPolicy(Duration.ofDays(7), Duration.ofMinutes(10))
            .addPolicy(Duration.ofDays(14), Duration.ofHours(1))
            .build();

        long from = policy.getDecimFrom(decimatedAt.toEpochMilli());
        assertEquals(expected, Instant.ofEpochMilli(from));
        assertEquals(0, policy.getDecimFrom(0));
    }
}
