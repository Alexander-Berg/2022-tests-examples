package ru.auto.testextension

import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list

/**
 * @author themishkun on 18/10/2018.
 */
fun <T : Any> nonEmptyList(generator: Arb<T>): Arb<List<T>> = Arb.list(generator).filter { it.isNotEmpty() }

