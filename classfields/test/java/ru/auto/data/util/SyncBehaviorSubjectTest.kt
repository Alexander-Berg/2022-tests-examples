package ru.auto.data.util

import io.qameta.allure.kotlin.junit4.AllureRunner
import ru.auto.testextension.testWithSubscriber
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * @author dumchev on 27.02.2018.
 */
@RunWith(AllureRunner::class)
class SyncBehaviorSubjectTest {

    private lateinit var subj: SyncBehaviorSubject<Int>

    @Before
    fun before() {
        subj = SyncBehaviorSubject.create()
    }

    @Test
    fun `onCompleted and hasCompleted`() {
        subj.onCompleted()
        check(subj.hasCompleted()) { "is not completed, while should be" }
    }

    @Test
    fun `onError, hasThrowable and throwable`() {
        val thr = IOException("testException")
        subj.onError(thr)
        check(subj.hasThrowable()) { "should've had a throwable" }
        check(subj.throwable == thr) { "wtf, unknown throwable in the subj" }
    }

    @Test
    fun `onNext, hasValue and value`() {
        val num = 1
        check(subj.hasValue().not()) { "subj should've had no value, where did it get one?" }
        subj.onNext(0)
        subj.onNext(num)
        check(subj.hasValue()) { "subj should've had a value" }
        check(subj.value == num) { "wtf, unknown value in subject. Where did it get one?" }
    }

    @Test
    fun `toObservable, toSingle`() {
        val num = 1
        subj.onNext(num)

        testWithSubscriber(subj) { sub -> sub.assertValue(num) }

        val single = subj.take(1).toSingle()
        testWithSubscriber(single) { sub -> sub.assertValue(num) }
    }


}
