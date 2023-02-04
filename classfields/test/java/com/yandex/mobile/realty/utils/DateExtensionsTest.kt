package com.yandex.mobile.realty.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * @author andrey-bgm on 11/11/2020.
 */
class DateExtensionsTest {

    @Test
    fun yearAndMonthPassed() {
        assertEquals(2 to 11, date(2017, 12, 14).yearAndMonthPassed(date(2020, 11, 14)))
        assertEquals(0 to 2, date(2020, 9, 13).yearAndMonthPassed(date(2020, 11, 14)))
        assertEquals(0 to 1, date(2020, 9, 14).yearAndMonthPassed(date(2020, 11, 13)))
        assertEquals(0 to 0, date(2020, 10, 15).yearAndMonthPassed(date(2020, 11, 14)))
        assertEquals(0 to 0, date(2020, 10, 15).yearAndMonthPassed(date(2020, 10, 15)))
        assertEquals(0 to 0, date(2020, 10, 15).yearAndMonthPassed(date(2020, 10, 16)))
        assertEquals(0 to 0, date(2020, 9, 14).yearAndMonthPassed(date(2018, 11, 15)))
    }

    private fun date(year: Int, month: Int, day: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)

        return calendar.time
    }
}
