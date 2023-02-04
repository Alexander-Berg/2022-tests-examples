package ru.auto.testextension

import rx.Observable
import rx.Single
import rx.observers.TestSubscriber

fun <T> testWithSubscriber(
        observable: Observable<T>,
        assertions: (TestSubscriber<T>) -> Unit
) {
    testWithSubscriber<T> { subscriber ->
        observable.subscribe(subscriber)
        assertions(subscriber)
    }
}

fun <T> testWithSubscriber(
        single: Single<T>,
        assertions: (TestSubscriber<T>) -> Unit = baseSingleAssertion()
) {
    testWithSubscriber<T> { subscriber ->
        single.subscribe(subscriber)
        assertions(subscriber)
    }
}

fun <T> testWithSubscriber(testActions: (TestSubscriber<T>) -> Unit) =
        testActions.invoke(TestSubscriber())

fun <T> TestSubscriber<T>.completedWithNoErrors() {
    awaitTerminalEvent()
    assertNoErrors()
    assertCompleted()
}

private fun <T> baseSingleAssertion(): (TestSubscriber<T>) -> Unit = { it.completedWithNoErrors() }

fun <T> Observable<T>.subscribeToTest(): TestSubscriber<T> {
    val testSub = TestSubscriber<T>()
    subscribe(testSub)
    return testSub
}

fun <T> Single<T>.subscribeToTest(): TestSubscriber<T> {
    val testSub = TestSubscriber<T>()
    subscribe(testSub)
    return testSub
}
