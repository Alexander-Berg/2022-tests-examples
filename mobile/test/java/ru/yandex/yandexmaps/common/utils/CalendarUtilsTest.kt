package ru.yandex.yandexmaps.common.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class CalendarUtilsTest {

    @Test
    fun newYear() {
        val monday30Dec2019 = date(year = 2019, month = Calendar.DECEMBER, dayOfMonth = 30)
        val sunday5Jan2020 = date(year = 2020, month = Calendar.JANUARY, dayOfMonth = 5)
        val monday6Jan2020 = date(year = 2020, month = Calendar.JANUARY, dayOfMonth = 6)

        assertTrue(CalendarUtils.isInSameWeek(monday30Dec2019, sunday5Jan2020))
        assertFalse(CalendarUtils.isInSameWeek(monday30Dec2019, monday6Jan2020))
        assertFalse(CalendarUtils.isInSameWeek(sunday5Jan2020, monday6Jan2020))
    }

    @Test
    fun someDatesInTheMiddleOfYear() {
        val saturday2Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 2)
        val sunday3Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 3)
        val monday4Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 4)
        val tuesday5Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 5)
        val wednesday6Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 6)
        val thursday7Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 7)
        val friday8Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 8)
        val saturday9Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 9)
        val sunday10Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 10)
        val monday11Feb2019 = date(year = 2019, month = Calendar.FEBRUARY, dayOfMonth = 11)

        assertTrue(CalendarUtils.isInSameWeek(saturday2Feb2019, sunday3Feb2019))
        assertFalse(CalendarUtils.isInSameWeek(saturday2Feb2019, monday4Feb2019))
        assertFalse(CalendarUtils.isInSameWeek(sunday3Feb2019, monday4Feb2019))

        val week = listOf(monday4Feb2019, tuesday5Feb2019, wednesday6Feb2019, thursday7Feb2019, friday8Feb2019, saturday9Feb2019, sunday10Feb2019)
        week.forEachIndexed { index1, day1 ->
            week.forEachIndexed { index2, day2 ->
                assert(CalendarUtils.isInSameWeek(day1, day2)) { "Not in same week $day1($index1) and $day2($index2)" }
            }

            assert(!CalendarUtils.isInSameWeek(day1, monday11Feb2019)) { "In same week $day1($index1) and monday11Feb2019" }
        }
    }

    private fun date(year: Int, month: Int, dayOfMonth: Int) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
    }.timeInMillis
}
