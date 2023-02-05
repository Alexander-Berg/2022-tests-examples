package com.edadeal.android.model

import com.edadeal.android.util.StringUtils
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TimeTest {

    @BeforeTest
    fun setup() {
        changeGlobalTimezone(TimeZone.getTimeZone("Europe/Moscow"))
    }

    @Test
    fun testCompareDateStrings() {
        assertEquals(0, Time.compareDateStrings("2016-12-31T01:00:00+03:00", "2016-12-31"))
        assertEquals(1, Time.compareDateStrings("2016-12-31T01:00:00+03:00", "2016-12-30"))
        assertEquals(-1, Time.compareDateStrings("2016-12-31T01:00:00+03:00", "2017-01-01"))
        assertEquals(-1, Time.compareDateStrings("2016-01-01", "2016-10-01"))
        assertEquals(0, Time.compareDateStrings("2016-06-01", "2016-06-01"))
        assertEquals(1, Time.compareDateStrings("2016-06-09", "2016-06-08"))
        assertEquals(1, Time.compareDateStrings("2017-06-09", "2016-12-09"))
    }

    @Test
    fun testCompareCalendars() {
        val cal1 = assertNotNull(Time.parseYYYYMMDD("2016-12-30"))
        val cal2 = assertNotNull(Time.parseYYYYMMDD("2016-12-31"))
        val cal3 = assertNotNull(Time.parseYYYYMMDD("2017-01-01"))
        assertEquals(-1, Time.compareCalendarIgnoringTime(cal1, cal2))
        assertEquals(-1, Time.compareCalendarIgnoringTime(cal2, cal3))
        assertEquals(1, Time.compareCalendarIgnoringTime(cal3, cal2))
        assertEquals(1, Time.compareCalendarIgnoringTime(cal2, cal1))
        assertEquals(0, Time.compareCalendarIgnoringTime(cal1, cal1))
        assertEquals(0, Time.compareCalendarIgnoringTime(cal2, cal2))
        assertEquals(0, Time.compareCalendarIgnoringTime(cal3, cal3))
    }

    @Test
    fun testConversions() {
        val nbsp = StringUtils.STR_NBSP
        listOf("2016-12-31", "2016-12-31T01:00:00+03:00", "2016-12-31T01:23:59+03:00").forEach {
            val cal = assertNotNull(Time.parseYYYYMMDD(it))
            assertEquals(Time.calendarToHumanDMMMM(cal), "31${nbsp}декабря")
            assertEquals(Time.calendarToHumanDMMM(cal), "31${nbsp}дек")
        }
    }

    @Test
    fun `format is correct when uses UTC time zone`() {
        val timeZone = TimeZone.getTimeZone("UTC")
        changeGlobalTimezone(timeZone)
        val calendar = Calendar.getInstance()
        calendar.set(1990, 11, 31, 23, 59, 59)

        val res = Time().getRfc3339Time(calendar.timeInMillis)
        assertEquals("1990-12-31T23:59:59Z", res)
    }

    @Test
    fun `parse string from rfc3339 format is correct with zero timezone`() {
        val currentTime = "1970-01-01T00:00:01Z"

        val mills = Time().getMillisFromRfc3339(currentTime)
        assertEquals(mills, 1000L)
    }

    @Test
    fun `parse string from rfc3339 format is correct with +1 timezone`() {
        val currentTime = "1970-01-01T01:00:0+01:00"

        val mills = Time().getMillisFromRfc3339(currentTime)
        assertEquals(mills, 0)
    }

    @Test
    fun `parse string from rfc3339 format is correct with -1 timezone`() {
        val currentTime = "1970-01-01T00:00:0-01:00"

        val mills = Time().getMillisFromRfc3339(currentTime)
        assertEquals(mills, Time.hoursToMillis(1))
    }

    @Test
    fun `two rfc3339 strings comparing is correct`() {
        val time1 = "1972-03-02T11:01:12+02:00"
        val time2 = "1972-03-02T11:01:12+01:00"

        val mills1 = Time().getMillisFromRfc3339(time1)
        val mills2 = Time().getMillisFromRfc3339(time2)

        val res = mills2 - mills1

        assertEquals(res, Time.hoursToMillis(1))
    }

    @Test
    fun `format is correct when uses 8 hours behind UTC time zone`() {
        val timeZone = TimeZone.getTimeZone("America/New_York")
        changeGlobalTimezone(timeZone)
        val calendar = Calendar.getInstance()
        calendar.set(1990, 11, 31, 23, 59, 59)

        val res = Time().getRfc3339Time(calendar.timeInMillis)
        assertEquals("1990-12-31T23:59:59-05:00", res)
    }

    @Test
    fun `format is correct when uses 3 hours after UTC time zone`() {
        val timeZone = TimeZone.getTimeZone("Europe/Moscow")
        changeGlobalTimezone(timeZone)
        val calendar = Calendar.getInstance()
        calendar.set(1990, 11, 31, 23, 59, 59)

        val res = Time().getRfc3339Time(calendar.timeInMillis)
        assertEquals("1990-12-31T23:59:59+03:00", res)
    }

    @Test
    fun `time returns real timezone after settings changes with one time instance`() {
        val time = Time()
        val timeZone = TimeZone.getTimeZone("Europe/Moscow")
        changeGlobalTimezone(timeZone)
        val calendar = Calendar.getInstance()
        calendar.set(1990, 11, 31, 23, 59, 59)

        val resOne = time.getRfc3339Time(calendar.timeInMillis)

        val timeZoneOther = TimeZone.getTimeZone("America/New_York")
        changeGlobalTimezone(timeZoneOther)
        val calendarOther = Calendar.getInstance()
        calendarOther.set(1990, 11, 31, 23, 59, 59)

        val resTwo = time.getRfc3339Time(calendarOther.timeInMillis)

        assertEquals("1990-12-31T23:59:59+03:00", resOne)
        assertEquals("1990-12-31T23:59:59-05:00", resTwo)
    }

    @Test
    fun `timestamp value is correct when uses UTC time zone`() {
        val time = Time()
        val expected = 1638392400000L

        val timestamp1 = time.getTimestampFromRfc3339("2021-12-02T00:00:00+03:00")
        val timestamp2 = time.getTimestampFromRfc3339("2021-12-01T21:00:00+00:00")
        assertEquals(expected, timestamp1)
        assertEquals(expected, timestamp2)
    }

    private fun changeGlobalTimezone(timeZone: TimeZone) = TimeZone.setDefault(timeZone)
}
