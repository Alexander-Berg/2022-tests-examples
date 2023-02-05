package com.yandex.frankenstein.agent.network

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

private const val KNOWN_PROTOCOL = "https"
private const val UNKNOWN_PROTOCOL = "ftp"

private const val PORT = 42

class MockHostUrlStreamHandlerTest {

    @Mock private lateinit var updateUrl: (URL) -> URL
    @Mock private lateinit var delegate: (URL) -> URLConnection?
    @Mock private lateinit var urlConnection: URLConnection

    private val oldUrl = URL("http://42.42.42.42:100/path?query")

    init {
        MockitoAnnotations.initMocks(this)
    }

    private val defaultUrlStreamHandlers = mapOf(KNOWN_PROTOCOL to DefaultUrlStreamHandler(delegate))
    private val urlStreamHandler = MockHostUrlStreamHandler(defaultUrlStreamHandlers, updateUrl)

    @Test
    fun testOpenConnection() {
        mockDefaultUrlStreamHandler(KNOWN_PROTOCOL)
        val connection = UrlUtils.openConnection(urlStreamHandler, oldUrl)

        assertThat(connection).isEqualTo(urlConnection)
    }

    @Test
    fun testOpenConnectionWithUnknownProtocol() {
        mockDefaultUrlStreamHandler(UNKNOWN_PROTOCOL)
        val connection = UrlUtils.openConnection(urlStreamHandler, oldUrl)

        assertThat(connection).isNull()
    }

    private fun mockDefaultUrlStreamHandler(protocol: String) {
        val newUrl = URL(protocol, "100.100.100.100", PORT, "path?query")
        `when`(updateUrl(oldUrl)).thenReturn(newUrl)
        `when`(delegate(newUrl)).thenReturn(urlConnection)
    }

    class DefaultUrlStreamHandler(delegate: (URL) -> URLConnection?) : AbstractUrlStreamHandler(delegate)

    abstract class AbstractUrlStreamHandler(private val delegate: (URL) -> URLConnection?) : URLStreamHandler() {

        override fun openConnection(url: URL) = delegate(url)
    }
}
