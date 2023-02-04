package ru.yandex.payments.tvm;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.github.benmanes.caffeine.cache.Ticker;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import ru.yandex.payments.tvm.cache.CaffeineServiceTicketsCache;
import ru.yandex.payments.tvm.cache.NonCachingServiceTicketsCache;
import ru.yandex.payments.tvm.client.TvmTicket.ServiceTvmTicket;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTicketsCacheTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private static ServiceTvmTicket ticket(long id) {
        return new ServiceTvmTicket(String.valueOf(id), id);
    }

    private static Mono<ServiceTvmTicket> ticketMono(long id) {
        return Mono.just(ticket(id));
    }

    @Test
    @DisplayName("Verify that 'NonCachingServiceTicketsCache' caches nothing")
    void testNonCachingServiceTicketsCache() {
        val cache = new NonCachingServiceTicketsCache();

        assertThat(cache.get(0L, id -> ticketMono(10L)).block(TIMEOUT))
                .isEqualTo(ticket(10L));

        assertThat(cache.get(0L, id -> ticketMono(20L)).block(TIMEOUT))
                .isEqualTo(ticket(20L));
    }

    @Test
    @DisplayName("Verify that 'CaffeineServiceTicketsCache' caches tickets and update them according to configuration")
    void testCaffeineServiceTicketsCache() {
        val config = new TvmConfiguration.Caching(true, Optional.empty());
        final var ticker = new Ticker() {
            private Duration value = Duration.ZERO;

            @Override
            public long read() {
                return value.toNanos();
            }

            void tick(Duration delta) {
                value = value.plus(delta);
            }
        };

        val expiry = Duration.ofSeconds(1);
        val cache = new CaffeineServiceTicketsCache(config, expiry, Executors.newSingleThreadExecutor(), ticker);

        assertThat(cache.get(0L, id -> ticketMono(10L)).block(TIMEOUT))
                .isEqualTo(ticket(10L));

        assertThat(cache.get(0L, id -> ticketMono(20L)).block(TIMEOUT))
                .isEqualTo(ticket(10L));

        ticker.tick(expiry);

        assertThat(cache.get(0L, id -> ticketMono(20L)).block(TIMEOUT))
                .isEqualTo(ticket(20L));
    }
}
