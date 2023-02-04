package ru.auto.data.util

import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.nats
import io.kotest.property.checkAll
import ru.auto.data.util.property.LazyRetryDelegate
import ru.auto.data.util.property.LazyRetryDelegate.Companion.MAX_RETRY_COUNT
import kotlin.test.assertFailsWith

/**
 * @author dumchev on 1/24/19.
 */
class LazyRetryDelegateTest : DescribeSpec({

    describe("LazyRetryDelegate help functions") {

        fun builderByAttemptsCount(count: Int): Function0<Int> = object : Function0<Int> {
            private var attemptCount = 0
            override fun invoke(): Int = when (attemptCount++) {
                count -> Math.random().toInt()
                else -> error("Throwing exception for test purpose")
            }
        }

        context("random attempts") {
            it("check works when there are more attempts than exceptions") {
                checkAll(Arb.nats()) { randNum ->
                    val exceptionCount = randNum % MAX_RETRY_COUNT
                    val onFail: Function2<Exception, Int, Unit> = mock()
                    val result: Int by LazyRetryDelegate(
                        builder = builderByAttemptsCount(exceptionCount),
                        onFail = onFail,
                        retryCount = exceptionCount
                    )

                    identity(result)
                    val attemptCaptor = argumentCaptor<Int>()
                    verify(onFail, times(exceptionCount)).invoke(argumentCaptor<Exception>().capture(), attemptCaptor.capture())

                    val expectedAttempts = IntRange(0, exceptionCount - 1).toList()
                    val realAttempts = attemptCaptor.allValues
                    check(realAttempts == expectedAttempts) {
                        "with $exceptionCount exception, has attempts $realAttempts, but expected $expectedAttempts"
                    }
                }
            }

            it("check everything works when no exception thrown") {
                checkAll(Arb.nats()) { randNum ->
                    val result: Int by LazyRetryDelegate({ randNum })
                    identity(result)
                }
            }
        }

        it("check have exception when retries less than attempts") {
            assertFailsWith<IllegalStateException> {
                val exceptionCount = 3
                val result: Int by LazyRetryDelegate(
                    builder = builderByAttemptsCount(exceptionCount),
                    onFail = { e, i -> println("retry num $i, e: $e") },
                    retryCount = exceptionCount - 1
                )
                identity(result)
            }
        }
    }
})

