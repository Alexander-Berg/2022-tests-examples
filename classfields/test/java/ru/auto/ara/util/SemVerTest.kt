package ru.auto.ara.util

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AllureRunner::class) class SemVerTest {

    companion object {
        val LOWER = { first: SemVer, second: SemVer ->
            first < second
        }
        val LOWER_EQUALS = { first: SemVer, second: SemVer ->
            first <= second
        }
        val GREATER = { first: SemVer, second: SemVer ->
            first > second
        }
        val GREATER_EQUALS = { first: SemVer, second: SemVer ->
            first >= second
        }
        val EQUALS = { first: SemVer, second: SemVer ->
            first == second
        }
    }

    class Args(
        private val left: SemVer,
        private val right: SemVer,
        private val tester: (SemVer, SemVer) -> Boolean
    ) {
        constructor(left: String, right: String, tester: (SemVer, SemVer) -> Boolean) : this(
            SemVer.parse(left),
            SemVer.parse(right),
            tester
        )

        fun test(): Boolean = tester.invoke(left, right)
    }

    @Test
    fun testSemVer() {
        listOf(
            Args("1", "2", LOWER),
            Args("1", "2", LOWER_EQUALS),
            Args("1", "1", LOWER_EQUALS),
            Args("1", "1", EQUALS),
            Args("2", "2", GREATER_EQUALS),
            Args("2", "1", GREATER_EQUALS),
            Args("2", "1", GREATER),

            Args("1", "1.1", LOWER),
            Args("1", "1.1", LOWER_EQUALS),
            Args("1.0", "1.0", LOWER_EQUALS),
            Args("1.0", "1.0", EQUALS),
            Args("2.0", "2.0", GREATER_EQUALS),
            Args("1.1", "1", GREATER_EQUALS),
            Args("1.1", "1", GREATER),

            Args("1.0.0-alpha", "1.0.0-beta", LOWER),
            Args("1.0.0-alpha", "1.0.0-beta", LOWER_EQUALS),
            Args("1.0.0-beta", "1.0.0-beta", LOWER_EQUALS),
            Args("1.0.0", "1.0.0", EQUALS),
            Args("2.0.1+15", "2.0.1+15", GREATER_EQUALS),
            Args("1.0.0+15", "1.0.0+14", GREATER_EQUALS),
            Args("1.1.0-alpha1", "1.1.0-alpha0", GREATER)
        ).forEach { args -> assertThat(args.test()) }
    }
}
