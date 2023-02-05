package com.yandex.sync.lib

import com.yandex.sync.lib.request.SyncCalendarRequest
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister
import org.xmlunit.matchers.CompareMatcher
import java.io.ByteArrayOutputStream


class SyncCalendarRequestTest {
    val principal = """
        <d:sync-collection xmlns:d="DAV:">
          <d:sync-token>abs</d:sync-token>
          <d:sync-level>1</d:sync-level>
          <d:prop>
            <d:getetag/>
            <calendar-data xmlns="urn:ietf:params:xml:ns:caldav"/>
          </d:prop>
        </d:sync-collection>
        """

    @Test
    fun `serialization test`() {
        val request = SyncCalendarRequest(
                syncToken = "abs"
        )

        val stream = ByteArrayOutputStream()
        Persister().write(request, stream)

        Assert.assertThat(principal, CompareMatcher.isIdenticalTo(stream.toString()).ignoreWhitespace())
    }
}
