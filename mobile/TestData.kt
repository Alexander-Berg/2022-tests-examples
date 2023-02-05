package ru.yandex.market

import java.util.Date
import java.util.GregorianCalendar

object TestData {
    @Suppress("MagicNumber")
    private val FIXED_DATE_CALENDAR = GregorianCalendar(2018, 5, 22, 0, 0, 0)

    @JvmStatic
    val dateTime: Date
        get() = FIXED_DATE_CALENDAR.time
}