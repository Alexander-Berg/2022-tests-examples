package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.WeekDay

@RunWith(Enclosed::class)
class WeekDayMapperTest {

    @RunWith(Parameterized::class)
    class ErrorCases(private val input: String?) {

        private val mapper = WeekDayMapper()

        @Test
        fun `Returns error for incorrect input`() {
            val result = mapper.map(input)
            assertThat(result).matches { it.exception != null }
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\"")
            @JvmStatic
            fun parameters(): Iterable<String?> {
                return listOf(null, "", "0", "8", "abc", "   ")
            }
        }
    }

    @RunWith(Parameterized::class)
    class CommonCases(
        private val input: String,
        private val expected: WeekDay
    ) {

        private val mapper = WeekDayMapper()

        @Test
        fun `Returns week day for correct input`() {
            val result = mapper.map(input)
            assertThat(result).extracting { it.get() }.isEqualTo(expected)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                return listOf(
                    arrayOf("1", WeekDay.MONDAY),
                    arrayOf("2", WeekDay.TUESDAY),
                    arrayOf("3", WeekDay.WEDNESDAY),
                    arrayOf("4", WeekDay.THURSDAY),
                    arrayOf("5", WeekDay.FRIDAY),
                    arrayOf("6", WeekDay.SATURDAY),
                    arrayOf("7", WeekDay.SUNDAY)
                )
            }
        }
    }

    @RunWith(Parameterized::class)
    class CommonCasesWithShift(
        private val dayIndex: Int,
        private val weekdayStartIndex: Int,
        private val expected: WeekDay
    ) {

        private val mapper = WeekDayMapper()

        @Test
        fun `Returns week day for correct input with shift`() {
            val result = mapper.map(dayIndex, weekdayStartIndex)
            assertThat(result).extracting { it.get() }.isEqualTo(expected)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\", \"{1}\" -> {2}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                return listOf(
                    arrayOf(0, 0, WeekDay.MONDAY),
                    arrayOf(1, 0, WeekDay.TUESDAY),
                    arrayOf(3, 1, WeekDay.WEDNESDAY),
                    arrayOf(4, 1, WeekDay.THURSDAY),
                    arrayOf(9, 5, WeekDay.FRIDAY),
                    arrayOf(10, 5, WeekDay.SATURDAY),
                    arrayOf(11, 5, WeekDay.SUNDAY)
                )
            }
        }
    }

    class OtherCases {

        private val mapper = WeekDayMapper()

        @Test
        fun `Ignores spaces in input`() {
            val result = mapper.map(" 1 ")
            assertThat(result).extracting { it.get() }.isEqualTo(WeekDay.MONDAY)
        }
    }
}