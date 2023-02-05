package ru.yandex.market.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class MathTest {

    @RunWith(Parameterized::class)
    class ToIntPercentTest(private val value: Float, private val expectedResult: Int) {

        @Test
        fun `Check result match expectations`() {
            assertThat(value.toIntPercent()).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} to int percent is {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(-3f, -300),
                arrayOf(-0.12f, -12),
                arrayOf(0f, 0),
                arrayOf(0.5f, 50),
                arrayOf(0.501f, 50),
                arrayOf(0.505f, 51),
                arrayOf(0.506f, 51),
                arrayOf(1f, 100),
                arrayOf(5f, 500)
            )
        }
    }

    @RunWith(Parameterized::class)
    class ToFloatPercentTest(private val value: Int, private val expectedResult: Float) {

        @Test
        fun `Check result match expectations`() {
            assertThat(value.toFloatPercent()).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} to float percent is {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(-100, -1f),
                arrayOf(-10, -0.1f),
                arrayOf(-1, -0.01f),
                arrayOf(0, 0.0f),
                arrayOf(50, 0.5f),
                arrayOf(100, 1.0f),
                arrayOf(500, 5f)
            )
        }
    }
}