package com.yandex.frankenstein.utils.authorization

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.Base64

class SshRsaAuthorizationTest {

    @Test
    fun testGetHeaders() {
        val username = "username"
        val timestamp = "timestamp"
        val serializedRequest = "serializedRequest"

        val rsaSigner = mock(RsaSigner::class.java)
        `when`(rsaSigner.sign(any() ?: ByteArray(0))).thenReturn(serializedRequest.toByteArray())

        val headers = SshRsaAuthorization.getHeaders(rsaSigner, username, "timestamp", serializedRequest)

        assertThat(headers).containsExactly(
            entry("X-Ya-Rsa-Timestamp", timestamp),
            entry("X-Ya-Rsa-Login", username),
            entry("X-Ya-Rsa-Signature", Base64.getUrlEncoder().encodeToString(serializedRequest.toByteArray())),
        )
    }
}
