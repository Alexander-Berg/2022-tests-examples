package com.yandex.sync.lib

import com.yandex.sync.lib.response.CalendarEventMetasResponse
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister

class CalendarEventMetasResponseTest {
    @Test
    fun `deserialization test`() {
        val calendarsResponse =
            Persister().read(CalendarEventMetasResponse::class.java, MockResponses.CALENDAR, false)
        val events = calendarsResponse.events!!

        Assert.assertNotNull(events)
        Assert.assertEquals(5, events.size)
        with(events[0]) {
            Assert.assertEquals("/calendars/ttqul%40yandex.ru/events-5632612/thhnxh8myandex.ru.ics", responseHref?.trim())
            Assert.assertEquals("1517258265195", etag)
            Assert.assertEquals("HTTP/1.1 200 OK", status)
        }
    }
}
