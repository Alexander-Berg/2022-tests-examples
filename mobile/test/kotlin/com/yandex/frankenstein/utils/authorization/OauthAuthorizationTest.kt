package com.yandex.frankenstein.utils.authorization

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test

class OauthAuthorizationTest {

    @Test
    fun testGetHeaders() {
        val token = "test_token"
        val headers = OauthAuthorization.getHeaders(token)

        assertThat(headers).containsExactly(entry("Authorization", "OAuth $token"))
    }
}
