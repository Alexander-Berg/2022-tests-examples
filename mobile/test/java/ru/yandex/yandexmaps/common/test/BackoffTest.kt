package ru.yandex.yandexmaps.common.test

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.yandexmaps.common.utils.extensions.rx.retryWithLinearBackoff
import java.util.concurrent.TimeUnit

class BackoffTest {

    @Test
    fun linearBackoffExceptionTest() {
        var serviceCalls = 0
        var errorCalls = 0
        val service = Maybe.timer(1, TimeUnit.MILLISECONDS)
            .map {
                serviceCalls++
                throw RuntimeException()
            }
        service.retryWithLinearBackoff(1, TimeUnit.MILLISECONDS, 3)
            .doOnError { errorCalls++ }
            .onErrorComplete()
            .blockingGet()

        assertThat(serviceCalls).isEqualTo(4)
        assertThat(errorCalls).isEqualTo(1)
    }

    @Test
    fun linearBackoffSuccessTest() {
        var serviceCalls = 0
        var errorCalls = 0
        var successCalls = 0
        val service = Maybe.timer(1, TimeUnit.MILLISECONDS)
            .map {
                if (++serviceCalls == 3)
                    it
                else
                    throw RuntimeException()
            }
        service.retryWithLinearBackoff(1, TimeUnit.MILLISECONDS, 3)
            .doOnError { errorCalls++ }
            .doOnSuccess { successCalls++ }
            .onErrorComplete()
            .blockingGet()

        assertThat(serviceCalls).isEqualTo(3)
        assertThat(errorCalls).isEqualTo(0)
        assertThat(successCalls).isEqualTo(1)
    }

    @Test
    fun disposeTest1() {
        var isComplete = false
        var isDisposed = false
        val compositeDisposable = CompositeDisposable()
        compositeDisposable.dispose()

        compositeDisposable.add(
            Observable.just(Unit)
                .observeOn(TestScheduler())
                .doOnComplete { isComplete = true }
                .doOnDispose { isDisposed = true }
                .subscribe { }
        )

        assertThat(isComplete).isEqualTo(false)
        assertThat(isDisposed).isEqualTo(true)
    }

    @Test
    fun disposeTest2() {
        var isComplete = false
        var isDisposed = false
        val compositeDisposable = CompositeDisposable()
        compositeDisposable.dispose()

        compositeDisposable.add(
            Observable.just(Unit)
                .doOnComplete { isComplete = true }
                .doOnDispose { isDisposed = true }
                .subscribe { }
        )

        assertThat(isComplete).isEqualTo(true)
        assertThat(isDisposed).isEqualTo(false)
    }
}
