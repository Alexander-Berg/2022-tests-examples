package ru.auto.ara.util

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.pair


/**
 * @author themishkun on 08/04/2018.
 */

infix fun <T> Iterable<T>?.shouldBeSameSetAs(other: Iterable<T>?) {
    this?.toSet() shouldBe other?.toSet()
}

infix fun Int.plusOrMinus(tolerance: Int): Matcher<Int> = this.let { matching ->
    object : Matcher<Int> {
        override fun test(value: Int): MatcherResult {
            val diff = Math.abs(matching - value)
            return MatcherResult(
                passed = diff <= tolerance,
                failureMessage = "$value should be equal to $matching",
                negatedFailureMessage = "$value should not be equal to $matching"
            )
        }
    }
}

fun <T> Collection<T>.toNonEmptyList(): List<T>? = if (size > 0) toList() else null

fun <T, R> List<SerializablePair<T, R>>.toListOfPairs(): List<Pair<T, R>> = map { it.first to it.second }

fun orderedPairOfChosenOrNulls(fromRange: Int, toRange: Int): Gen<Pair<Int?, Int?>> =
        Arb.pair(Arb.int(fromRange, toRange).orNull(),
                Arb.int(fromRange, toRange).orNull())
                .filter { (first, second) ->
                    first == null || first < (second ?: Int.MAX_VALUE)
                }

fun orderedPairOfChosenOrNulls(fromRange: Long, toRange: Long): Gen<Pair<Long?, Long?>> =
        Arb.pair(Arb.long(fromRange, toRange).orNull(),
                Arb.long(fromRange, toRange).orNull())
                .filter { (first, second) ->
                    first == null || first < (second ?: Long.MAX_VALUE)
                }

fun orderedPairOfNatsOrNulls() = orderedPairOfChosenOrNulls(1, Int.MAX_VALUE)
