package ru.yandex.market.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.extensions.daysBetween
import ru.yandex.market.extensions.daysBetweenIgnoreTime
import ru.yandex.market.extensions.withTimeAtStartOfDay
import ru.yandex.market.utils.createDate
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

@RunWith(Enclosed::class)
class DateTimeUtilsTest {

    @RunWith(Parameterized::class)
    class DaysBetweenDatesIgnoreTimeTest(
        private val dateOne: Date,
        private val dateTwo: Date,
        private val expectedResult: Long
    ) {
        @Test
        fun `Method works as expected`() {
            val result = dateOne daysBetweenIgnoreTime dateTwo

            assertThat(result).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {2} days between {0} and {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 16),
                    0
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 17),
                    1
                ),
                arrayOf(
                    createDate(2018, 6, 16, 23, 59, 59),
                    createDate(2018, 6, 17),
                    1
                ),
                arrayOf(
                    createDate(2018, 6, 17),
                    createDate(2018, 6, 16),
                    1
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 23),
                    7
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 7, 16),
                    31
                )
            )
        }
    }

    @RunWith(Parameterized::class)
    class DaysBetweenDatesTest(
        private val dateOne: Date,
        private val dateTwo: Date,
        private val expectedResult: Long
    ) {
        @Test
        fun `Method works as expected`() {
            val result = dateOne daysBetween dateTwo

            assertThat(result).isEqualTo(expectedResult)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {2} days between {0} and {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 16),
                    0
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 17),
                    1
                ),
                arrayOf(
                    createDate(2018, 6, 16, 23, 59, 59),
                    createDate(2018, 6, 17),
                    0
                ),
                arrayOf(
                    createDate(2018, 6, 17),
                    createDate(2018, 6, 16),
                    1
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 23),
                    7
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 7, 16),
                    31
                )
            )
        }
    }

    class SimpleTests {

        @Test
        fun `withTimeAtStartOfDay resets date time to midnight`() {
            var date = createDate(2018, 6, 16, 12, 30, 42, 42)
            date = date.withTimeAtStartOfDay
            val calendar = GregorianCalendar.getInstance().apply { time = date }

            assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
            assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(0)
            assertThat(calendar.get(Calendar.SECOND)).isEqualTo(0)
            assertThat(calendar.get(Calendar.MILLISECOND)).isEqualTo(0)
        }
    }
}