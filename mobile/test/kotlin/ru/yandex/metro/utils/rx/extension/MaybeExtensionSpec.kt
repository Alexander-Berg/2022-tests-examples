package ru.yandex.metro.utils.rx.extension

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Maybe
import io.reactivex.MaybeOnSubscribe
import io.reactivex.schedulers.TestScheduler
import ru.yandex.metro.CallableSpek

private const val TEST_TIMES = 10 // Arbitrary number but > 1 to make it sensible

class SwitchIfEmptyEagerSpec : CallableSpek(Maybe<*>::switchIfEmptyEager, {
    it("should not subscribe during assembly time") {
        val onSubscribe = mock<MaybeOnSubscribe<Unit>>()
        val testIntermediateSources = Array(TEST_TIMES) {
            Maybe.create<Unit>(onSubscribe)
        }
        testIntermediateSources.reduce { acc, maybe ->
            acc.switchIfEmptyEager(maybe)
        }

        verify(onSubscribe, never()).subscribe(any())
    }

    it("should subscribe eagerly to all") {
        val onSubscribe = mock<MaybeOnSubscribe<Unit>>()
        val testIntermediateSources = Array(TEST_TIMES) {
            Maybe.create<Unit>(onSubscribe)
        }
        testIntermediateSources.reduce { acc, maybe ->
            acc.switchIfEmptyEager(maybe)
        }.subscribe()

        verify(onSubscribe, times(TEST_TIMES)).subscribe(any())
    }

    it("should emit first non empty") {
        val testScheduler = TestScheduler()
        var counter = 0
        val emptyIntermediateSources = Array(TEST_TIMES) {
            Maybe.empty<Int>()
        }
        val valueIntermediateSources = Array(TEST_TIMES) {
            Maybe.fromCallable { ++counter }
        }
        val testIntermediateSources = (emptyIntermediateSources + valueIntermediateSources).map {
            it.observeOn(testScheduler)
        }
        val tester = testIntermediateSources.reduce { acc, maybe ->
            acc.switchIfEmptyEager(maybe)
        }.test()

        testScheduler.triggerActions()
        tester.assertResult(1)
    }

    it("should preserve assembly time order favoring upstream source over downstream") {
        val testScheduler1 = TestScheduler()
        val testScheduler2 = TestScheduler()
        val maybe1 = Maybe.fromCallable { 1 }.observeOn(testScheduler1)
        val maybe2 = Maybe.fromCallable { 2 }.observeOn(testScheduler2)
        val tester = maybe1
                .switchIfEmptyEager(maybe2)
                .test()

        testScheduler2.triggerActions()
        testScheduler1.triggerActions()
        tester.assertResult(1)
    }
})
