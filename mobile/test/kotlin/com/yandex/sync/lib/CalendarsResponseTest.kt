package com.yandex.sync.lib

import com.yandex.sync.lib.response.CalendarsResponse
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister

class CalendarsResponseTest {
    @Test
    fun `deserialization test`() {
        val calendarsResponse =
            Persister().read(CalendarsResponse::class.java, MockResponses.CALENDARS, false)
        val calendars = calendarsResponse.calendars!!

        Assert.assertNotNull(calendars)
        Assert.assertEquals(7, calendars.size)
        Assert.assertEquals(3, calendars.filter { it.calendarComp == "VEVENT" }.size)
        with(calendars[3]) {
            Assert.assertEquals("/calendars/thevery%40yandex-team.ru/events-12618/", responseHref)
            Assert.assertEquals("#49c0a8ff", color)
            Assert.assertEquals("Ильдар Каримов", displayname)
            Assert.assertEquals("/principals/users/thevery%40yandex-team.ru/", ownerHref)
            Assert.assertEquals("VEVENT", calendarComp)
            Assert.assertEquals("1518307816957", ctag)
            Assert.assertEquals(true, isReadable())
            Assert.assertEquals(false, isWritable())
            Assert.assertEquals("HTTP/1.1 200 OK", status)
        }

        with(calendars[4]) {
            Assert.assertEquals("/calendars/thevery%40yandex-team.ru/events-12873/", responseHref)
            Assert.assertEquals(true, isReadable())
            Assert.assertEquals(false, isWritable())
        }

        with(calendars[5]) {
            Assert.assertEquals("/calendars/thevery%40yandex-team.ru/todos-3946/", responseHref)
            Assert.assertEquals(false, isReadable())
            Assert.assertEquals(false, isWritable())
        }
    }
}
