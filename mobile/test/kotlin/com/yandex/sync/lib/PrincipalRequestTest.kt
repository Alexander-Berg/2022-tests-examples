package com.yandex.sync.lib

import com.yandex.sync.lib.request.PrincipalRequest
import com.yandex.sync.lib.request.Prop
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister
import org.xmlunit.matchers.CompareMatcher
import java.io.ByteArrayOutputStream


class PrincipalRequestTest {
    val principal = """
        <d:propfind xmlns:d="DAV:">
            <d:prop>
                <d:current-user-principal />
            </d:prop>
        </d:propfind>
        """

    @Test
    fun `serialization test`() {
        val request = PrincipalRequest()

        val stream = ByteArrayOutputStream()
        Persister().write(request, stream)

        Assert.assertThat(principal, CompareMatcher.isIdenticalTo(stream.toString()).ignoreWhitespace())
    }
}
