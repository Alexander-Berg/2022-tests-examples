package com.yandex.sync.lib

import com.yandex.sync.lib.response.PrincipalResponse
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister

class PrincipalXmlTest {
    @Test
    fun `deserialization test`() {
        val principal = Persister().read(PrincipalResponse::class.java, MockResponses.PRINCIPAL, false)

        Assert.assertNotNull(principal)
        Assert.assertEquals("/", principal.responseHref)
        Assert.assertEquals("/principals/users/thevery%40yandex-team.ru/", principal.principalHref)
        Assert.assertEquals("HTTP/1.1 200 OK", principal.status)
    }
}
