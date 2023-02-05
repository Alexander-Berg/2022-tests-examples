package ru.yandex.market.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.utils.Duration
import ru.yandex.market.utils.millis
import ru.yandex.market.utils.minutes
import ru.yandex.market.utils.seconds
import java.util.concurrent.TimeUnit

@RunWith(Enclosed::class)
class DurationTest {

    @RunWith(Parameterized::class)
    class ConversionTest(
        private val input: Duration,
        private val conversionMethod: (Duration) -> Duration,
        private val expectedResult: Duration
    ) {

        @Test
        fun `Check conversion works as expected`() {
            assertThat(conversionMethod(input)).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} -> {2}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf<Any>(1.seconds, { d: Duration -> d.inMilliseconds }, 1000.millis),
                arrayOf<Any>(1.seconds, { d: Duration -> d.inSeconds }, 1.seconds),

                arrayOf<Any>(1.millis, { d: Duration -> d.inMilliseconds }, 1.millis),
                arrayOf<Any>(1.millis, { d: Duration -> d.inSeconds }, 0.001.seconds),

                arrayOf<Any>(1.minutes, { d: Duration -> d.inMilliseconds }, 60000.millis),
                arrayOf<Any>(1.minutes, { d: Duration -> d.inSeconds }, 60.seconds)
            )
        }
    }

    @RunWith(Parameterized::class)
    class JavaUnitTest(
        private val input: Duration,
        private val expectedResult: TimeUnit
    ) {

        @Test
        fun `Check returns correct java TimeUnit`() {
            assertThat(input.javaUnit).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(1.millis, TimeUnit.MILLISECONDS),
                arrayOf(1.seconds, TimeUnit.SECONDS),
                arrayOf(1.minutes, TimeUnit.MINUTES)
            )
        }
    }
}