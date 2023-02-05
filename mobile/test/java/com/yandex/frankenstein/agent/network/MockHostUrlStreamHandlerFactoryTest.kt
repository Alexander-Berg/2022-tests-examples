package com.yandex.frankenstein.agent.network

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.net.JarURLConnection
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection
import kotlin.reflect.KClass

@RunWith(Parameterized::class)
class MockHostUrlStreamHandlerFactoryTest(
    @Suppress("unused") private val protocol: String,
    private val newUrl: URL,
    private val newUrlConnectionClass: KClass<URLConnection>
) {

    companion object {
        @Parameters(name = "{0}")
        @JvmStatic fun data() = listOf(
            arrayOf("http", URL("http://100.100.100.100:42/path?query"), HttpsURLConnection::class),
            arrayOf("https", URL("https://100.100.100.100:42/path?query"), HttpsURLConnection::class),
            arrayOf("jar", URL("jar:http://100.100.100.100:42/some.jar!/some.class"), JarURLConnection::class)
        )
    }

    private val oldUrl = URL("http://42.42.42.42:100/path?query")

    @Mock private lateinit var updateUrl: (URL) -> URL

    init {
        MockitoAnnotations.initMocks(this)
    }

    private val updateUrlAsAny: Any = updateUrl
    private val urlStreamHandlerFactory = MockHostUrlStreamHandlerFactory(updateUrlAsAny)
    private val urlStreamHandler = urlStreamHandlerFactory.createURLStreamHandler(oldUrl.protocol)

    @Test
    fun testOpenConnection() {
        `when`(updateUrl(oldUrl)).thenReturn(newUrl)
        val connection = UrlUtils.openConnection(urlStreamHandler as? MockHostUrlStreamHandler, oldUrl)

        assertThat(connection).isInstanceOf(newUrlConnectionClass.java)
        assertThat(connection.url).isEqualTo(newUrl)
    }
}
