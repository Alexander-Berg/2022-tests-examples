package com.edadeal.android.model.entity

import com.edadeal.protobuf.content.v3.mobile.Shop
import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekdayTest {

    @Test
    fun testCalendarConversion() {
        val calendars = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        ).map { Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, it) } }

        assertEquals(Weekday.values().toList(), calendars.map { Weekday.fromCalendar(it) })
    }

    @Test
    fun testOpenHoursWeekdayConversion() {
        val openHoursWeekdays = Shop.OpenHours.Weekday.values()

        assertEquals(Weekday.values().toList(), openHoursWeekdays.map { Weekday.fromProto(it) })
    }
}
