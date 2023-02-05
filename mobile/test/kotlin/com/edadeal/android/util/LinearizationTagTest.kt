package com.edadeal.android.util

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.CompletableSubject
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class LinearizationTagTest {
    private val scheduler = TestScheduler()
    private val tag = LinearizationTag()
    private val result = StringBuilder()
    private val appender = Consumer<String> { result.append(it) }

    @Test
    fun `rx streams with tag should execute sequentially`() {
        val startItems = listOf("A", "B", "C")

        Observable.interval(1, TimeUnit.SECONDS, scheduler).take(startItems.size.toLong())
            .map { startItems[it.toInt()] }
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)
        Maybe.fromCallable { "D" }
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)
        Single.fromCallable { "E" }
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)

        scheduler.triggerActions()
        assertEquals("", result.toString())

        repeat(startItems.size) { scheduler.advanceTimeBy(1, TimeUnit.SECONDS) }
        assertEquals("ABCDE", result.toString())
    }

    @Test
    fun `disposed rx streams and observers should not alter execution of others`() {
        val start = CompletableSubject.create()
        start.linearize(tag).subscribe()

        val disposingSingleObserver = object : SingleObserver<String> {
            override fun onSuccess(v: String) = appender.accept(v)
            override fun onError(e: Throwable) = appender.accept("onError")
            override fun onSubscribe(d: Disposable) = d.dispose()
        }
        val errorCompletableObserver = TestObserver<Int>()

        Maybe.just("A")
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)
        Completable.error(Exception())
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(errorCompletableObserver)
        Single.just("C")
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)
        Completable.complete()
            .subscribeOn(scheduler)
            .linearize(tag).subscribe { appender.accept("D") }
        Single.just("E")
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(disposingSingleObserver)
        val disposable = Maybe.just("F")
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)
        Observable.just("E")
            .subscribeOn(scheduler)
            .linearize(tag).subscribe(appender)

        scheduler.triggerActions()
        errorCompletableObserver.assertNoErrors()
        assertEquals("", result.toString())

        disposable.dispose()
        scheduler.triggerActions()
        errorCompletableObserver.assertNoErrors()
        assertEquals("", result.toString())

        start.onComplete()
        scheduler.triggerActions()
        assertEquals("ACDE", result.toString())
        assertEquals(1, errorCompletableObserver.errorCount())
    }
}
