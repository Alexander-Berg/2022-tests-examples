package com.yandex.sync.lib.entity

import com.yandex.sync.lib.MockResponses
import com.yandex.sync.lib.response.CalendarsResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.simpleframework.xml.core.Persister

class CalendarEntityTest {

    @Test
    fun `Check correct exception millis and other`() {
        val calendarsResponse =
                Persister().read(CalendarsResponse::class.java, MockResponses.CALENDARS, false)
        val calendars = calendarsResponse.calendars?.filter { it.calendarComp == "VEVENT" }!!

        val calendarEntity = CalendarEntity.fromCalendarsInnerResponse(calendars.first(), "thevery@yandex-team.ru")

        assertEquals("/calendars/thevery%40yandex-team.ru/events-12618/", calendarEntity.href)
        assertEquals("Ильдар Каримов", calendarEntity.name)
        assertEquals("Ильдар Каримов", calendarEntity.displayName)
        assertEquals("#49c0a8ff", calendarEntity.color)
        assertEquals("thevery@yandex-team.ru", calendarEntity.owner)
        assertEquals("1518307816957", calendarEntity.ctag)
        assertEquals(false, calendarEntity.canWrite)
        assertEquals(true, calendarEntity.canRead)
        assertEquals("data:,1518307816957", calendarEntity.syncToken)
        assertEquals(calendarEntity.href, calendarEntity.syncId)

    }
}