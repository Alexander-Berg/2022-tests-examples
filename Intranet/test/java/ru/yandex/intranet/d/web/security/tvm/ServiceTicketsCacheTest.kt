package ru.yandex.intranet.d.web.security.tvm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import ru.yandex.intranet.d.web.security.tvm.model.TvmTicket
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * Service tickets cache test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
class ServiceTicketsCacheTest {

    @Test
    fun testSuccess() {
        val counter = AtomicLong(0L)
        val cache = ServiceTicketsCache(
            1L,
            Duration.ofHours(1L),
            10L,
            1L,
            Duration.ofMillis(1L)
        ) { _, destinations ->
            counter.incrementAndGet()
            Mono.just(destinations.associateBy({ v -> v }, { v -> TvmTicket("test", v.toLong(), null) }))
        }
        val ticket = cache.getServiceTicket(42L).block()
        Assertions.assertEquals("test", ticket)
        val anotherTicket = cache.getServiceTicket(42L).block()
        Assertions.assertEquals("test", anotherTicket)
        Assertions.assertEquals(1, counter.get())
    }

    @Test
    fun testRetry() {
        val counter = AtomicLong(0L)
        val cache = ServiceTicketsCache(
            1L,
            Duration.ofHours(1L),
            10L,
            1L,
            Duration.ofMillis(1L)
        ) { _, destinations ->
            val newCounter = counter.incrementAndGet()
            if (newCounter <= 1) {
                Mono.error(WebClientResponseException(500, "Test", null ,null, null))
            } else {
                Mono.just(destinations.associateBy({ v -> v }, { v -> TvmTicket("test", v.toLong(), null) }))
            }
        }
        val ticket = cache.getServiceTicket(42L).block()
        Assertions.assertEquals("test", ticket)
        val anotherTicket = cache.getServiceTicket(42L).block()
        Assertions.assertEquals("test", anotherTicket)
        Assertions.assertEquals(2, counter.get())
    }

    @Test
    fun testError() {
        val cache = ServiceTicketsCache(
            1L,
            Duration.ofHours(1L),
            10L,
            1L,
            Duration.ofMillis(1L)
        ) { _, _ ->
            Mono.error(WebClientResponseException(500, "Test", null ,null, null))
        }
        var exception = false
        try {
            cache.getServiceTicket(42L).block()
        } catch (e: Exception) {
            exception = true
        }
        Assertions.assertTrue(exception)
    }

}
