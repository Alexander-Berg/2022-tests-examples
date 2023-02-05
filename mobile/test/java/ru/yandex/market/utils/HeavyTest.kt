package ru.yandex.market.utils

import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class HeavyTest {

    @Test
    fun `Get method returns instance of heavy object`() {
        val holder = Heavy.initInBackground { DummyObject() }
        assert(holder.get() != null)
    }

    @Test(expected = ProviderException::class)
    fun `Get method throws exception when provider throws exception for init in background`() {
        val holder = Heavy.initInBackground<String> { throw ProviderException() }
        holder.get()
    }

    @Test(expected = ProviderException::class)
    fun `Get method throws exception when provider throws exception for init lazily`() {
        val holder = Heavy.initLazily<String> { throw ProviderException() }
        holder.get()
    }

    @Test
    fun `Holder init still working after provider exception`() {
        val executor = Executors.newSingleThreadExecutor()
        val provider = { throw ProviderException() }
        repeat(10) {
            val holder = Heavy.initWithExecutor<String>(executor, provider)
            try { holder.get() } catch (e: ProviderException) { /* skip on current thread */ }
        }
    }

    @Test
    fun `Holder always holds the same instance for init in background`() {
        val counter = AtomicInteger()
        val holder = Heavy.initInBackground {
            Thread.sleep(20)
            counter.incrementAndGet()
        }
        val expectedCount = 1
        var concurrentAssertionFailed = false
        val threads = 10
        val latch = CountDownLatch(threads)
        repeat(10) {
            Thread {
                if (holder.get() != expectedCount) {
                    concurrentAssertionFailed = true
                }
                latch.countDown()
            }.start()
        }
        latch.await()
        assert(!concurrentAssertionFailed) { "Concurrent assertion failed" }
        assert(holder.get() == expectedCount)
    }

    @Test
    fun `Holder always holds the same instance for init lazily`() {
        val counter = AtomicInteger()
        val holder = Heavy.initLazily {
            Thread.sleep(20)
            counter.incrementAndGet()
        }
        val expectedCount = 1
        var concurrentAssertionFailed = false
        val threads = 10
        val latch = CountDownLatch(threads)
        repeat(10) {
            Thread {
                if (holder.get() != expectedCount) {
                    concurrentAssertionFailed = true
                }
                latch.countDown()
            }.start()
        }
        latch.await()
        assert(!concurrentAssertionFailed) { "Concurrent assertion failed" }
        assert(holder.get() == expectedCount)
    }

    private class DummyObject

    private class ProviderException : Exception()

}