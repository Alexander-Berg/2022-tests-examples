package ru.yandex.metro.utils.rx.extension

import com.jakewharton.rx.replayingShare
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import ru.yandex.metro.CallableSpek

class ReplayingShareSpec : CallableSpek(Observable<*>::replayingShare, {
    context("infinite upstream") {
        lateinit var testObservable: Observable<Int>
        beforeEachTest {
            var counter = 0
            testObservable = Observable.create<Int> { emitter ->
                emitter.onNext(counter++)
            }.replayingShare()
        }

        context("single subscriber") {
            lateinit var tester: TestObserver<Int>

            beforeEachTest {
                tester = TestObserver()
                testObservable.subscribe(tester)
            }

            afterEachTest {
                tester.dispose()
            }

            it("should emit 1 value total") {
                tester.assertValuesOnly(0)
            }
        }

        context("multiple subscribers") {
            lateinit var testers: Array<TestObserver<Int>>

            afterEachTest {
                testers.forEach(TestObserver<Int>::dispose)
            }

            context("just subscribe") {
                beforeEachTest {
                    testers = Array(3) { TestObserver<Int>() }
                    testers.forEach(testObservable::subscribe)
                }

                it("should emit cached value for all") {
                    testers[0].assertValuesOnly(0)
                    testers[1].assertValuesOnly(0)
                    testers[2].assertValuesOnly(0)
                }
            }

            context("each subscribe and dispose at once") {
                beforeEachTest {
                    testers = Array(3) { TestObserver<Int>() }
                    testers.forEach { tester ->
                        testObservable.subscribe(tester)
                        tester.dispose()
                    }
                }

                it("should emit current and resubscribe to upstream for new value") {
                    testers[0].assertValuesOnly(0)
                    testers[1].assertValuesOnly(0, 1)
                    testers[2].assertValuesOnly(1, 2)
                }
            }
        }
    }

    context("finite upstream") {
        lateinit var testObservable: Observable<Int>
        beforeEachTest {
            var counter = 0
            testObservable = Observable.fromCallable { counter++ }.replayingShare()
        }

        context("single subscriber") {
            lateinit var tester: TestObserver<Int>

            beforeEachTest {
                tester = TestObserver()
                testObservable.subscribe(tester)
            }

            afterEachTest {
                tester.dispose()
            }

            it("should emit 1 value total") {
                tester.assertValues(0)
            }
        }

        context("multiple subscribers") {
            lateinit var testers: Array<TestObserver<Int>>

            beforeEachTest {
                testers = Array(3) { TestObserver<Int>() }
                testers.forEach(testObservable::subscribe)
            }

            afterEachTest {
                testers.forEach(TestObserver<Int>::dispose)
            }


            it("should emit new value for each subscriber") {
                testers[0].assertValues(0)
                testers[1].assertValues(1)
                testers[2].assertValues(2)
            }
        }
    }
})
