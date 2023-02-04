package ru.auto.data.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.should
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nats
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

/**
 * @author themishkun on 17/10/2018.
 */
class MathExtTests : StringSpec() {
    init {
        "(a whatPercentageOf b) * b should be equal to a" {
            checkAll(Arb.pair(Arb.nats(), Arb.nats())) { (a, b) ->
                val percentage = a whatPercentageOf b
                (b * percentage / 100) should (a.toDouble() plusOrMinus 0.0001)
            }
        }

        "ensureInRangeInclusive" {
            checkAll(Arb.int()) { num ->
                val min = -500
                val max = 100
                num.ensureInRangeInclusive(min, max) should object : Matcher<Int> {
                    override fun test(value: Int) = MatcherResult(
                        passed = value in min..max,
                        failureMessage = "$value should be >= $min and <= $max",
                        negatedFailureMessage = "$value should be > $max and < $min"
                    )
                }
            }
        }
    }
}
