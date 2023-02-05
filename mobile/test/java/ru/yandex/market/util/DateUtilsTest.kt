package ru.yandex.market.util

import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.utils.createDate
import ru.yandex.market.utils.isSameDay
import java.util.Date

@RunWith(Enclosed::class)
class DateUtilsTest {

    @RunWith(Parameterized::class)
    class IsSameDayTest(
        private val dateOne: Date,
        private val dateTwo: Date,
        private val expectedResult: Boolean
    ) {
        @Test
        fun `Method works as expected`() {
            val result = dateOne.isSameDay(dateTwo)
            Assert.assertThat(result, Matchers.equalTo(expectedResult))
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: Is same days {0} and {1}? {2}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(
                    createDate(2018, 1, 16),
                    createDate(2018, 12, 16),
                    false
                ),
                arrayOf(
                    createDate(2018, 6, 1),
                    createDate(2018, 6, 30),
                    false
                ),
                arrayOf(
                    createDate(2017, 6, 16),
                    createDate(2018, 6, 16),
                    false
                ),
                arrayOf(
                    createDate(2018, 6, 16, 23, 59, 59),
                    createDate(2018, 6, 16),
                    true
                ),
                arrayOf(
                    createDate(2018, 6, 16, 23, 59, 59),
                    createDate(2018, 6, 16, 11, 59, 59),
                    true
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 16),
                    true
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 17),
                    false
                ),
                arrayOf(
                    createDate(2018, 6, 16, 23, 59, 59),
                    createDate(2018, 6, 17),
                    false
                ),
                arrayOf(
                    createDate(2018, 6, 17),
                    createDate(2018, 6, 16),
                    false
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 6, 23),
                    false
                ),
                arrayOf(
                    createDate(2018, 6, 16),
                    createDate(2018, 7, 16),
                    false
                )
            )
        }
    }

}