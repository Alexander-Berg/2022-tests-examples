package com.yandex.sync.lib

import com.yandex.sync.lib.request.CalendarHomeRequest
import com.yandex.sync.lib.request.PrincipalRequest
import com.yandex.sync.lib.request.Prop
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister
import org.xmlunit.matchers.CompareMatcher
import java.io.ByteArrayOutputStream


class CalendarHomeRequestTest {
    val principal = """
        <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
            <d:prop>
                <c:calendar-home-set />
                <calserv:email-address-set xmlns:calserv="http://calendarserver.org/ns/"/>
            </d:prop>
        </d:propfind>
        """

    @Test
    fun `serialization test`() {
        val request = CalendarHomeRequest()

        val stream = ByteArrayOutputStream()
        Persister().write(request, stream)

        Assert.assertThat(principal, CompareMatcher.isIdenticalTo(stream.toString()).ignoreWhitespace())
    }
}
