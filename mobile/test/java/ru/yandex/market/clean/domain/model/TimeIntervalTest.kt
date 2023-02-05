package ru.yandex.market.clean.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.common.LocalTime

@RunWith(Enclosed::class)
class TimeIntervalTest {

    class OtherCases {

        @Test(expected = IllegalStateException::class)
        fun `Throws exception when start time is greater than end time`() {
            val startTime = LocalTime.noon()
            val endTime = LocalTime.midnight()
            TimeInterval.create(startTime, endTime)
        }

        @Test(expected = IllegalStateException::class)
        fun `Throws exception when start time is equal to end time`() {
            val startTime = LocalTime.midnight()
            TimeInterval.create(startTime, startTime)
        }
    }

    @RunWith(Parameterized::class)
    class IsWholeDayTest(
        private val input: TimeInterval,
        private val expectedResult: Boolean
    ) {

        @Test
        fun `isWholeDay() returns true only when start time is midnight and end time is day end`() {
            assertThat(input.isWholeDay).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0}.isWholeDay == {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(TimeInterval.create(LocalTime.midnight(), LocalTime.dayEnd()), true),
                arrayOf(TimeInterval.create(LocalTime.noon(), LocalTime.dayEnd()), false),
                arrayOf(TimeInterval.create(LocalTime.midnight(), LocalTime.noon()), false),
                arrayOf(
                    TimeInterval.create(
                        LocalTime.create(12, 0),
                        LocalTime.create(20, 0)
                    ),
                    false
                ),
                arrayOf(
                    TimeInterval.create(
                        LocalTime.create(0, 0, 1),
                        LocalTime.dayEnd()
                    ),
                    false
                ),
                arrayOf(
                    TimeInterval.create(
                        LocalTime.midnight(),
                        LocalTime.create(23, 59, 58)
                    ),
                    false
                )
            )
        }
    }
}
