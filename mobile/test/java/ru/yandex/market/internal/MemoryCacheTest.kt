package ru.yandex.market.internal

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import ru.yandex.market.utils.seconds
import ru.yandex.market.datetime.DateTimeProvider

class MemoryCacheTest {

    private val configuration = MemoryCache.Configuration(2, 10.seconds)
    private val timeProvider = mock<DateTimeProvider> {
        on { currentUtcTimeInMillis } doReturn 0
    }
    private val cache = MemoryCache<String, Int>(configuration, timeProvider)

    @Test
    fun `Adding item to cache works as expected`() {
        val key = "a"
        val value = 197
        cache.set(key, value)

        val fromCache = cache.get(key)

        assertThat(fromCache).isEqualTo(value)
    }

    @Test
    fun `Adding item to cache for same key rewrites previous entry`() {
        val key = "a"
        val firstValue = 197
        val secondValue = 123
        cache.set(key, firstValue)
        cache.set(key, secondValue)

        val fromCache = cache.get(key)

        assertThat(fromCache).isEqualTo(secondValue)
    }

    @Test
    fun `Adding too many items to cache rewrites oldest entry`() {
        cache.set("a", 1)
        cache.set("b", 2)
        cache.set("c", 3)

        val snapshot = cache.snapshot

        assertThat(snapshot).contains("b" to 2, "c" to 3)
    }

    @Test
    fun `Getting absent key returns null`() {
        cache.set("a", 1)
        cache.set("b", 2)

        val fromCache = cache.get("c")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `Getting value from empty cache is null`() {
        val fromCache = cache.get("a")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `Getting expired value returns null`() {
        cache.set("a", 1)
        cache.set("b", 2)
        whenever(timeProvider.currentUtcTimeInMillis)
            .thenReturn(configuration.entryLifetime.inMilliseconds.longValue + 10)

        val fromCache = cache.get("a")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `Remove expired values from cache on get`() {
        cache.set("a", 1)
        cache.set("b", 2)
        whenever(timeProvider.currentUtcTimeInMillis)
            .thenReturn(configuration.entryLifetime.inMilliseconds.longValue + 10)

        cache.get("a")

        assertThat(cache.snapshot).isEmpty()
    }

    @Test
    fun `Remove expired values from cache on put`() {
        cache.set("a", 1)
        cache.set("b", 2)
        whenever(timeProvider.currentUtcTimeInMillis)
            .thenReturn(configuration.entryLifetime.inMilliseconds.longValue + 10)

        cache.set("c", 3)

        assertThat(cache.snapshot).contains("c" to 3)
    }
}