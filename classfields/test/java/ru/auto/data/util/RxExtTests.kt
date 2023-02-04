package ru.auto.data.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import ru.auto.testextension.nonEmptyList
import rx.Observable

/**
 * @author themishkun on 18/10/2018.
 */
class RxExtTests : FreeSpec({
    "firstToSingle util" - {
        "should get the first element emitted in Observable as Single" {
            checkAll(nonEmptyList(arbitrary { Any() })) { listOfElements: List<Any> ->
                val tester = Observable.from(listOfElements).firstToSingle().test()

                tester.assertCompleted()
                tester.assertValue(listOfElements.first())
            }
        }
        "should throw `NoSuchElementException` if there are no elements emitted" {
            val tester = Observable.empty<Any>().firstToSingle().test()

            tester.assertError(NoSuchElementException::class.java)
        }
    }
})
