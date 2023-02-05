package ru.yandex.metro.utils.android.lifecycle

import androidx.lifecycle.Lifecycle
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

const val GOOD_VALUE = 1
const val BAD_VALUE = 2

class LifecycleRxHelperSpec : Spek({
    describe("android lifecycle rx range operators") {
        fun testEvents() = Observable.create<Lifecycle.Event> { emitter ->
            emitter.onNext(Lifecycle.Event.ON_CREATE)
            emitter.onNext(Lifecycle.Event.ON_START)
            emitter.onNext(Lifecycle.Event.ON_RESUME)
            emitter.onNext(Lifecycle.Event.ON_PAUSE)
            emitter.onNext(Lifecycle.Event.ON_STOP)
            emitter.onNext(Lifecycle.Event.ON_DESTROY)
            emitter.onNext(Lifecycle.Event.ON_START)
            emitter.onNext(Lifecycle.Event.ON_RESUME)
            emitter.onNext(Lifecycle.Event.ON_PAUSE)
            emitter.onNext(Lifecycle.Event.ON_STOP)
            emitter.onNext(Lifecycle.Event.ON_DESTROY)
        }

        context("the mapper is observable source") {
            it("should resubscribe on range cycle") {
                val testScheduler = TestScheduler()
                val testObserver = TestObserver<Int>()

                testEvents()
                        .switchMapWithinRange(Lifecycle.Event.ON_START to Lifecycle.Event.ON_STOP) {
                            Observable.merge(
                                    Observable.just(GOOD_VALUE),
                                    Observable.just(BAD_VALUE).delay(0, TimeUnit.SECONDS, testScheduler)
                            )
                        }
                        .subscribe(testObserver)

                testScheduler.triggerActions()

                testObserver.assertNotComplete()
                testObserver.assertNoErrors()
                testObserver.assertValues(GOOD_VALUE, GOOD_VALUE)
            }
        }

        context("the mapper is single source") {
            it("should resubscribe on range cycle") {
                val testScheduler = TestScheduler()
                val testObserver = TestObserver<Int>()

                val goodData = Single.just(GOOD_VALUE)
                val badData = Single.just(BAD_VALUE).delay(0, TimeUnit.SECONDS, testScheduler)

                var subscriptionCount = 0

                testEvents()
                        .switchMapSingleWithinRange(Lifecycle.Event.ON_START to Lifecycle.Event.ON_STOP) {
                            when (++subscriptionCount) {
                                1 -> goodData
                                else -> badData
                            }
                        }
                        .subscribe(testObserver)

                testScheduler.triggerActions()

                testObserver.assertNotComplete()
                testObserver.assertError(CancellationException::class.java)
                testObserver.assertValues(GOOD_VALUE)
            }
        }

        context("the mapper is maybe source") {
            it("should resubscribe on range cycle") {
                val testScheduler = TestScheduler()
                val testObserver = TestObserver<Int>()

                val goodData = Maybe.just(GOOD_VALUE)
                val badData = Maybe.just(BAD_VALUE).delay(0, TimeUnit.SECONDS, testScheduler)

                var subscriptionCount = 0

                testEvents()
                        .switchMapMaybeWithinRange(Lifecycle.Event.ON_START to Lifecycle.Event.ON_STOP) {
                            when (++subscriptionCount) {
                                1 -> goodData
                                else -> badData
                            }
                        }
                        .subscribe(testObserver)

                testScheduler.triggerActions()

                testObserver.assertNotComplete()
                testObserver.assertNoErrors()
                testObserver.assertValues(GOOD_VALUE)
            }
        }

        context("the mapper is completable source") {
            it("should resubscribe on range cycle") {
                val testScheduler = TestScheduler()
                val testObserver = TestObserver<Unit>()

                val innerValues = mutableListOf<Int>()
                var subscriptionCount = 0

                testEvents()
                        .switchMapCompletableWithinRange(Lifecycle.Event.ON_START to Lifecycle.Event.ON_STOP) {
                            when (++subscriptionCount) {
                                1 -> Completable.complete()
                                        .doOnComplete { innerValues += GOOD_VALUE }
                                else -> Completable.complete().delay(0, TimeUnit.SECONDS, testScheduler)
                                        .doOnComplete { innerValues += BAD_VALUE }
                            }

                        }
                        .subscribe(testObserver)

                testScheduler.triggerActions()

                testObserver.assertNotComplete()
                testObserver.assertNoErrors()
                innerValues shouldEqual listOf(GOOD_VALUE)
            }
        }
    }
})
