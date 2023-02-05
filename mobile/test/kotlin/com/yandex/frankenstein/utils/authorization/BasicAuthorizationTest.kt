package com.yandex.frankenstein.utils.authorization

import com.yandex.frankenstein.utils.HttpHeaders
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test
import java.util.Base64

class BasicAuthorizationTest {

    @Test
    fun testGetHeaders() {
        val user = "user"
        val password = "password"
        val headers = BasicAuthorization.getHeaders(user, password)

        val encoded = Base64.getUrlEncoder().encodeToString("$user:$password".toByteArray())
        assertThat(headers).containsExactly(
            entry(HttpHeaders.AUTHORIZATION, "Basic $encoded")
        )
    }
}
