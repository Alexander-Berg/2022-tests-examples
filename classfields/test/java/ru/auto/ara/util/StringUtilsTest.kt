package ru.auto.ara.util

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.core_ui.util.StringUtils
import kotlin.test.assertEquals

/**
 * @author airfreshener on 21.01.2019.
 */
@Suppress("TestFunctionName")
class StringUtilsTest {

    @RunWith(AllureParametrizedRunner::class)
    class FilterNumbersTests(
        private val input: String,
        private val expected: String
    ) {

        @Test
        fun FilterNumbersTestValue() {
            assertEquals(StringUtils.filterNumbers(input), expected)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data(): Array<Array<*>> = arrayOf(
                arrayOf("1,234,567", "1234567"), // remove commas
                arrayOf("1 234 567", "1234567"), // remove spaces
                arrayOf("", "") // empty string
            )
        }
    }

    @RunWith(AllureParametrizedRunner::class)
    class FormatNumberStringTests(
        private val input: String,
        private val expected: String
    ) {

        @Test
        fun FormatNumberStringTestValue() {
            assertEquals(StringUtils.formatNumberString(input), expected)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data(): Array<Array<*>> = arrayOf(
                arrayOf("1234567.891", "1 234 567.89"), // simple format
                arrayOf("1,23", "1.23"), // replace comma to dot
                arrayOf("1234567.00", "1 234 567"), // float to int
                arrayOf("0", "0"), // format zero
                arrayOf("", "") // format empty string
            )
        }

    }

}
