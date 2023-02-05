package com.yandex.frankenstein.utils.authorization

import com.spotify.sshagentproxy.AgentProxy
import com.spotify.sshagentproxy.Identity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.nio.ByteBuffer

class RsaSignerTest {

    private val PREFIX = "ssh-rsa"
    private val TEST_DATA = "test_data"
    private val ENCRYPTED_TEST_DATA = "encrypted_test_data"

    @Test
    fun testSign() {
        val identity = mock(Identity::class.java)
        val agentProxy = mock(AgentProxy::class.java)
        `when`(agentProxy.list()).thenReturn(listOf(identity))
        `when`(agentProxy.sign(eq(identity), eq(TEST_DATA.toByteArray())))
            .thenReturn(ENCRYPTED_TEST_DATA.toByteArray())

        val rsaSigner = RsaSigner(agentProxy)
        val signed = rsaSigner.sign(TEST_DATA.toByteArray())
        val expected = getWithPrefix(ENCRYPTED_TEST_DATA.toByteArray())

        assertThat(signed).isEqualTo(expected)
    }

    @Test(expected = RuntimeException::class)
    fun testSignWithoutKeys() {
        val agentProxy = mock(AgentProxy::class.java)
        `when`(agentProxy.list()).thenReturn(listOf())

        val rsaSigner = RsaSigner(agentProxy)
        rsaSigner.sign(TEST_DATA.toByteArray())
    }

    private fun getWithPrefix(signed: ByteArray): ByteArray {
        val bytes = ByteBuffer.allocate(4 + PREFIX.length + 4 + signed.size)
        bytes.putInt(PREFIX.length)
        bytes.put(PREFIX.toByteArray())
        bytes.putInt(signed.size)
        bytes.put(signed)
        return bytes.array()
    }
}
