package ru.yandex.yandexbus.inhouse.extensions

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest
import rx.Observable
import rx.schedulers.Schedulers
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

class OnTimeoutNextTest : BaseTest() {

    private lateinit var scheduler: TestScheduler

    @Before
    override fun setUp() {
        super.setUp()

        scheduler = TestScheduler()
    }

    @Test
    fun `fallback before original`() {
        val timeout = 1L
        val delay = 10L

        val subscriber = asyncObservable(delay)
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, scheduler) { FALLBACK }
            .test()

        subscriber.assertNoValues()

        scheduler.advanceTimeBy(timeout, TimeUnit.MILLISECONDS)

        subscriber.assertValues(FALLBACK)

        scheduler.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

        subscriber.assertValues(FALLBACK, ORIGINAL)
    }

    @Test
    fun `fallback suppressed with original`() {
        val timeout = 10L
        val delay = 1L

        val subscriber = asyncObservable(delay)
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, scheduler) { FALLBACK }
            .test()

        subscriber.assertNoValues()

        scheduler.advanceTimeBy(timeout, TimeUnit.MILLISECONDS)

        subscriber.assertValues(ORIGINAL)

        scheduler.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

        subscriber.assertValues(ORIGINAL)
    }

    @Test
    fun `fallback suppressed after completion`() {
        val timeout = 10L
        val delay = 1L

        val subscriber = asyncCompletion(delay)
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, scheduler) { FALLBACK }
            .test()

        scheduler.advanceTimeBy(timeout, TimeUnit.MILLISECONDS)

        subscriber
            .assertNoValues()
            .assertCompleted()
    }

    @Test
    fun `fallback suppressed with sync observable`() {
        val timeout = 0L

        val subscriber = syncObservable()
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, scheduler) { FALLBACK }
            .test()

        scheduler.advanceTimeBy(timeout, TimeUnit.MILLISECONDS)

        subscriber
            .assertValues(ORIGINAL)
            .assertCompleted()
    }

    @Test
    fun `allow equal fallback`() {
        val timeout = 1L
        val delay = 10L

        val subscriber = asyncObservable(delay)
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, scheduler) { ORIGINAL }
            .test()

        subscriber.assertNoValues()

        scheduler.advanceTimeBy(timeout, TimeUnit.MILLISECONDS)

        subscriber.assertValues(ORIGINAL)

        scheduler.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

        subscriber.assertValues(ORIGINAL, ORIGINAL)
    }

    @Test
    fun `suppress equal fallback with original`() {
        val timeout = 10L
        val delay = 1L

        val subscriber = asyncObservable(delay)
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, scheduler) { ORIGINAL }
            .test()

        subscriber.assertNoValues()

        scheduler.advanceTimeBy(timeout, TimeUnit.MILLISECONDS)

        subscriber.assertValues(ORIGINAL)

        scheduler.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

        subscriber.assertValues(ORIGINAL)
    }

    @Test
    fun `fallback before original on different schedulers`() {
        val timeout = 1L
        val delay = 10L

        val subscriber = asyncObservable(delay)
            .onTimeoutNext(timeout, TimeUnit.MILLISECONDS, Schedulers.computation()) { FALLBACK }
            .test()

        scheduler.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

        val values = subscriber
            .awaitValueCount(1, 100, TimeUnit.MILLISECONDS)
            .onNextEvents

        // either FALLBACK, ORIGINAL or ORIGINAL are ok
        // depends on how threads would behave
        assertEquals(values.last(), ORIGINAL)
    }

    private fun syncObservable(): Observable<Int> {
        return Observable.just(ORIGINAL)
    }

    private fun asyncObservable(delay: Long): Observable<Int> {
        return Observable.just(ORIGINAL).delay(delay, TimeUnit.MILLISECONDS, scheduler)
    }

    private fun asyncCompletion(delay: Long): Observable<Int> {
        return Observable.empty<Int>().delay(delay, TimeUnit.MILLISECONDS, scheduler)
    }

    private companion object {

        const val ORIGINAL = 1
        const val FALLBACK = 0
    }
}
