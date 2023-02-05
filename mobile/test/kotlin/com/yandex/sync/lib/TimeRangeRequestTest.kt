package com.yandex.sync.lib

import com.yandex.sync.lib.request.Filter
import com.yandex.sync.lib.request.TimeRange
import com.yandex.sync.lib.request.FilterRequest
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister
import org.xmlunit.matchers.CompareMatcher
import java.io.ByteArrayOutputStream

class TimeRangeRequestTest {
    val principal = """
        <c:calendar-query xmlns:d="DAV:"
                  xmlns:c="urn:ietf:params:xml:ns:caldav">
            <c:filter>
                <c:comp-filter name="VCALENDAR">
                    <c:comp-filter name="VEVENT">
                        <c:time-range start="20060104T000000Z" end="20060105T000000Z"/>
                    </c:comp-filter>
                </c:comp-filter>
            </c:filter>
            <d:prop>
                <d:getetag/>
                <c:calendar-data xmlns:c="urn:ietf:params:xml:ns:caldav"/>
            </d:prop>
        </c:calendar-query>
        """

    @Test
    fun `serialization test`() {
        val request = FilterRequest(
                filter = Filter(
                        filter = Filter(
                                name = "VCALENDAR",
                                filter = Filter(
                                        name = "VEVENT",
                                        timeRange = TimeRange("20060105T000000Z", "20060104T000000Z")
                                )
                        )
                )
        )

        val stream = ByteArrayOutputStream()
        Persister().write(request, stream)

        Assert.assertThat(principal, CompareMatcher.isIdenticalTo(stream.toString()).ignoreWhitespace())
    }
}
