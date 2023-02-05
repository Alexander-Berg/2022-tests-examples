package ru.yandex.market.utils

import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.schedulers.TestScheduler
import org.junit.Test

class RxJavaExtensionsTest {

    @Test
    fun `Single retry extension retries until success with specified delay`() {
        val helloWorld = "Hello World!"
        val singleProvider = mock<() -> SingleSource<String>> {
            on { invoke() } doReturnConsecutively listOf(Single.error(RuntimeException()), Single.just(helloWorld))
        }
        val delay = 5.seconds
        val scheduler = TestScheduler()
        val observer = Single.defer(singleProvider)
            .onErrorRetry(delay, scheduler)
            .test()

        observer.assertNoValues()
            .assertNoErrors()
            .assertNotComplete()

        scheduler.advanceTimeBy(delay)

        observer.assertNoErrors()
            .assertValue(helloWorld)
            .assertComplete()
    }

    @Test
    fun `Pair with previous works as expected`() {
        val first = "first"
        val second = "second"
        val third = "third"
        Observable.just(first, second, third)
            .joinValues()
            .test()
            .assertValues(
                AdjoiningValues(null, first),
                AdjoiningValues(first, second),
                AdjoiningValues(second, third)
            )
    }
}