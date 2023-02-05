package com.yandex.sync.lib

import com.yandex.sync.lib.response.UserInfoResponse
import org.junit.Assert
import org.junit.Test
import org.simpleframework.xml.core.Persister

class UserInfoResponseTest {
    @Test
    fun `deserialization test`() {
        val userInfo = Persister().read(UserInfoResponse::class.java, MockResponses.USERINFO, false)

        Assert.assertNotNull(userInfo)
        Assert.assertEquals("/principals/users/thevery%40yandex-team.ru/", userInfo.responseHref)
        Assert.assertEquals("/calendars/thevery%40yandex-team.ru/", userInfo.calendarHref)
        Assert.assertEquals("mailto:thevery@yandex-team.ru", userInfo.emailHref)
        Assert.assertEquals("HTTP/1.1 200 OK", userInfo.status)
    }
}
