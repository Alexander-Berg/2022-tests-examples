package ru.yandex.market.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class AlphaUtilsTest {

    @RunWith(Parameterized::class)
    class AsFloatTest(private val input: Int, private val expected: Float) {

        @Test
        fun `Correctly map integer alpha to float alpha`() {
            val mapped = AlphaUtils.toFloat(input)
            assertThat(mapped).isEqualTo(expected)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(0, 0f),
                arrayOf(255, 1f),
                arrayOf(51, 0.2f),
                arrayOf(102, 0.4f)
            )
        }
    }
}