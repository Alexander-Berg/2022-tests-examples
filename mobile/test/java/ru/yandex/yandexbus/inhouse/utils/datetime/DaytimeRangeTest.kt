package ru.yandex.yandexbus.inhouse.utils.datetime

import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.utils.DaytimeRange
import java.util.Calendar
import java.util.TimeZone

class DaytimeRangeTest {
    private lateinit var utcTimeZone: TimeZone
    private lateinit var moscowTimeZone: TimeZone

    private lateinit var midnightInMoscow: Calendar
    private lateinit var morningInMoscow: Calendar
    private lateinit var noonInMoscow: Calendar
    private lateinit var minuteAfterNoonInMoscow: Calendar
    private lateinit var eveningInMoscow: Calendar
    private lateinit var summerNoonInMoscow: Calendar

    private lateinit var noonOnUtc: Calendar

    @Before
    fun setUp() {
        utcTimeZone = TimeZone.getTimeZone("UTC")
        moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow")

        midnightInMoscow = createCalendar(moscowTimeZone, hours = 0, minutes =  0)
        morningInMoscow = createCalendar(moscowTimeZone, hours = 8, minutes = 0)
        noonInMoscow = createCalendar(moscowTimeZone, hours = 12, minutes = 0)
        minuteAfterNoonInMoscow = createCalendar(moscowTimeZone, hours = 12, minutes = 1)
        eveningInMoscow = createCalendar(moscowTimeZone, hours = 19, minutes = 0)

        summerNoonInMoscow = createCalendar(moscowTimeZone, hours = 12, minutes = 0)

        noonOnUtc = createCalendar(utcTimeZone, hours = 12, minutes = 0)
    }

    @Test
    fun `ranges are date independent`() {
        val daytimeRange = DaytimeRange(beginHour = 12, endHour = 13)
        assert(noonInMoscow in daytimeRange)
        assert(summerNoonInMoscow in daytimeRange)
    }

    @Test
    fun `ranges are timezone independent`() {
        val daytimeRange = DaytimeRange(beginHour = 12, endHour = 13)
        assert(noonInMoscow in daytimeRange)
        assert(noonOnUtc in daytimeRange)
    }

    @Test
    fun `begin point is in range for ranges which do not cross midnight`() {
        val daytimeRange = DaytimeRange(beginHour = 12, beginMinute = 1, endHour = 13)
        assert(noonInMoscow !in daytimeRange)
        assert(minuteAfterNoonInMoscow in daytimeRange)
    }

    @Test
    fun `begin point is in range for ranges which cross midnight`() {
        val daytimeRange = DaytimeRange(beginHour = 12, beginMinute = 1, endHour = 1)
        assert(noonInMoscow !in daytimeRange)
        assert(minuteAfterNoonInMoscow in daytimeRange)
    }

    @Test
    fun `end point is not in range for ranges which do not cross midnight`() {
        val daytimeRange = DaytimeRange(beginHour = 1, endHour = 12, endMinute = 1)
        assert(noonInMoscow in daytimeRange)
        assert(minuteAfterNoonInMoscow !in daytimeRange)
    }

    @Test
    fun `end point is not in range for ranges which cross midnight`() {
        val daytimeRange = DaytimeRange(beginHour = 23, endHour = 12, endMinute = 1)
        assert(noonInMoscow in daytimeRange)
        assert(minuteAfterNoonInMoscow !in daytimeRange)
    }


    @Test
    fun `contains works fine for intervals over midnight`() {
        val daytimeRange = DaytimeRange(beginHour = 19, endHour = 12)
        assert(morningInMoscow in daytimeRange)
        assert(noonInMoscow !in daytimeRange)
        assert(eveningInMoscow in daytimeRange)
        assert(midnightInMoscow in daytimeRange)
    }
}

private fun createCalendar(tz: TimeZone, hours: Int, minutes: Int, seconds: Int = 0): Calendar {
    val calendar = Calendar.getInstance(tz)
    calendar.set(2019, Calendar.JANUARY, 1, hours, minutes, seconds)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}
