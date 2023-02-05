package ru.yandex.market.util.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.extensions.decodeHtmlCharacters
import ru.yandex.market.extensions.formatAsPriceString
import ru.yandex.market.extensions.toCamelCase
import ru.yandex.market.utils.Characters
import java.math.BigDecimal

@RunWith(Enclosed::class)
class StringExtensionsTest {

    @Test
    fun `isNullOrBlank return true for null`() {
        val input: CharSequence? = null
        assertThat(input.isNullOrBlank()).isTrue
    }

    @Test
    fun `isNullOrBlank return true for blank string`() {
        assertThat("   ".isNullOrBlank()).isTrue
    }

    @Test
    fun `isNullOrBlank return true for empty string`() {
        assertThat("".isNullOrBlank()).isTrue
    }

    @Test
    fun `isNullOrBlank return false for none-blank string`() {
        assertThat("definitely-not-blank".isNullOrBlank()).isFalse
    }

    @Test
    fun `decodeHtmlCharacters return correct string for html-encoded input`() {
        val nbsp = Characters.NON_BREAKING_SPACE
        val dash = Characters.EM_DASH
        
        val originalString =
            "Эти игрушки необычные: у&nbsp;каждой&nbsp;&mdash; своя история, которую расскажет Алиса. " +
                    "Попросите её&nbsp;об&nbsp;этом, поднесите игрушку к&nbsp;Яндекс.Станции&nbsp;&mdash; и&nbsp;начнётся приключение"

        val expectedResultString =
            "Эти игрушки необычные: у${nbsp}каждой${nbsp}${dash} своя история, которую расскажет Алиса. " +
                    "Попросите её${nbsp}об${nbsp}этом, поднесите игрушку к${nbsp}Яндекс.Станции${nbsp}${dash} и${nbsp}начнётся приключение"

        assertThat(originalString.decodeHtmlCharacters() == expectedResultString)
    }


    @RunWith(Parameterized::class)
    class TestCamelCase(
        private val input: String,
        private val expected: String
    ) {

        @Test
        fun `Returns camel case for correct input`() {
            assertThat(input.toCamelCase()).isEqualTo(expected)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                return listOf(
                    arrayOf("Abc", "Abc"),
                    arrayOf("", ""),
                    arrayOf("a", "A"),
                    arrayOf("ABC", "Abc"),
                    arrayOf("ABC DEF", "Abc Def"),
                    arrayOf("ABC\u001EDEF", "Abc\u001EDef"),
                    arrayOf("Abc def", "Abc Def"),
                    arrayOf("AbC", "Abc"),
                    arrayOf("ABC  DEF", "Abc  Def"),
                    arrayOf("ABC DEF GHI", "Abc Def Ghi"),
                    arrayOf("ABC | def", "Abc | Def")
                )
            }
        }
    }

    @RunWith(Parameterized::class)
    class TestFormatAsPriceString(
        private val input: BigDecimal?,
        private val expected: String
    ) {

        @Test
        fun `Returns separate Thousands for correct input`() {
            assertThat(input.formatAsPriceString()).isEqualTo(expected)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                return listOf(
                    arrayOf(null, ""),
                    arrayOf(BigDecimal(1), "1"),
                    arrayOf(BigDecimal(12), "12"),
                    arrayOf(BigDecimal(123), "123"),
                    arrayOf(BigDecimal(1234), "1 234"),
                    arrayOf(BigDecimal(12345), "12 345"),
                    arrayOf(BigDecimal(123456), "123 456"),
                    arrayOf(BigDecimal(1234567), "1 234 567"),
                    arrayOf(BigDecimal(12345678), "12 345 678"),
                    arrayOf(BigDecimal(123456789), "123 456 789"),
                    arrayOf(BigDecimal(1234567890), "1 234 567 890"),
                    arrayOf(BigDecimal(12345678901), "12 345 678 901"),
                    arrayOf(BigDecimal(1234567890100), "1 234 567 890 100"),
                )
            }
        }
    }
}