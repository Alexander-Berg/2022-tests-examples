package com.yandex.sync.lib

import com.yandex.sync.lib.request.CalendarsRequest
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister
import org.xmlunit.matchers.CompareMatcher
import java.io.ByteArrayOutputStream

class CalendarsRequestTest {
    val principal = """
        <d:propfind xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/" xmlns:c="urn:ietf:params:xml:ns:caldav" xmlns:a="http://apple.com/ns/ical/">
          <d:prop>
             <d:displayname />
             <cs:getctag />
             <d:sync-token />
             <d:owner />
             <c:supported-calendar-component-set />
             <d:current-user-privilege-set/>
             <a:calendar-color />
          </d:prop>
        </d:propfind>
        """

    @Test
    fun `serialization test`() {
        val request = CalendarsRequest()

        val stream = ByteArrayOutputStream()
        Persister().write(request, stream)

        Assert.assertThat(principal, CompareMatcher.isIdenticalTo(stream.toString()).ignoreWhitespace())
    }
}
