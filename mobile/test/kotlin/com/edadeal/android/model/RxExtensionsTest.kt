package com.edadeal.android.model

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class RxExtensionsTest {

    @Test
    fun `awaitAll should signal after all specified items emitted by upstream`() {
        val subject = PublishSubject.create<Int>()
        val observer = TestObserver<Int>()
        subject.hide().awaitAll(0..2).subscribe(observer)
        observer.assertNotComplete()

        (0..1).forEach(subject::onNext)
        observer.assertNotComplete()

        subject.onNext(2)
        observer.assertComplete()

        observer.dispose()
    }

    @Test
    fun `awaitAll should signal if upstream signal onComplete`() {
        val subject = PublishSubject.create<Int>()
        val observerHalf = TestObserver<Int>()
        subject.hide().awaitAll(0..1).subscribe(observerHalf)
        val observerFull = TestObserver<Int>()
        subject.hide().awaitAll(0..2).subscribe(observerFull)
        observerHalf.assertNotComplete()
        observerFull.assertNotComplete()

        (0..1).forEach(subject::onNext)
        observerHalf.assertComplete()
        observerFull.assertNotComplete()

        subject.onComplete()
        observerHalf.assertComplete()
        observerFull.assertComplete()

        observerHalf.dispose()
        observerFull.dispose()
    }
}
