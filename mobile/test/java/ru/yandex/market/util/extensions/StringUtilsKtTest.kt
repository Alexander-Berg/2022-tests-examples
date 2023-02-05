package ru.yandex.market.util.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.extensions.isNumber

@RunWith(Enclosed::class)
class StringUtilsKtTest {

    @RunWith(Parameterized::class)
    class TestIsNumber(
        private val input: String,
        private val expected: Boolean
    ) {

        @Test
        fun `Returns is number for correct input`() {
            assertThat(input.isNumber()).isEqualTo(expected)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                return listOf(
                    arrayOf("14", true),
                    arrayOf("14.5", true),
                    arrayOf("+14.5", true),
                    arrayOf("-14.5", true),
                    arrayOf("0", true),
                    arrayOf("+0", true),
                    arrayOf("-0", true),
                    arrayOf("a", false),
                    arrayOf("-a", false)
                )
            }
        }
    }
}