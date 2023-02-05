package com.yandex.frankenstein.agent.network

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLSocketFactory

private const val CONNECT_TIMEOUT = 4
private const val READ_TIMEOUT = 2
private const val RESPONSE_CODE = 42
private const val CONTENT_LENGTH = 101

private const val KEY = "key"
private const val VALUE = "value"

@Suppress("TooManyFunctions")
class HttpsUrlConnectionDelegationTest {

    private val url = URL("http://www.example.com/docs/resource1.html")

    @Mock private lateinit var delegate: HttpURLConnection
    @Mock private lateinit var inputStream: InputStream
    @Mock private lateinit var outputStream: OutputStream
    @Mock private lateinit var headerFields: Map<String, List<String>>
    @Mock private lateinit var sslSocketFactory: SSLSocketFactory
    @Mock private lateinit var content: Any

    init {
        MockitoAnnotations.initMocks(this)

        `when`(delegate.url).thenReturn(url)
    }

    private val httpsUrlConnectionDelegation = HttpsUrlConnectionDelegation(delegate)

    @Test
    fun getUrl() {
        assertThat(httpsUrlConnectionDelegation.url).isEqualTo(url)
    }

    @Test
    fun testDisconnect() {
        httpsUrlConnectionDelegation.disconnect()

        verify(delegate).disconnect()
    }

    @Test
    fun testUsingProxy() {
        `when`(delegate.usingProxy()).thenReturn(true)

        assertThat(httpsUrlConnectionDelegation.usingProxy()).isTrue()
    }

    @Test
    fun testNotUsingProxy() {
        `when`(delegate.usingProxy()).thenReturn(false)

        assertThat(httpsUrlConnectionDelegation.usingProxy()).isFalse()
    }

    @Test
    fun testConnect() {
        httpsUrlConnectionDelegation.connect()

        verify(delegate).connect()
    }

    @Test
    fun testSetConnectTimeout() {
        httpsUrlConnectionDelegation.connectTimeout = CONNECT_TIMEOUT

        verify(delegate).connectTimeout = CONNECT_TIMEOUT
    }

    @Test
    fun testSetReadTimeout() {
        httpsUrlConnectionDelegation.readTimeout = READ_TIMEOUT

        verify(delegate).readTimeout = READ_TIMEOUT
    }

    @Test
    fun testSetDoInput() {
        httpsUrlConnectionDelegation.doInput = true

        verify(delegate).doInput = true
    }

    @Test
    fun testUnsetDoInput() {
        httpsUrlConnectionDelegation.doInput = false

        verify(delegate).doInput = false
    }

    @Test
    fun testSetRequestProperty() {
        httpsUrlConnectionDelegation.setRequestProperty(KEY, VALUE)

        verify(delegate).setRequestProperty(KEY, VALUE)
    }

    @Test
    fun testAddRequestProperty() {
        httpsUrlConnectionDelegation.addRequestProperty(KEY, VALUE)

        verify(delegate).addRequestProperty(KEY, VALUE)
    }

    @Test
    fun testSetDoOutput() {
        httpsUrlConnectionDelegation.doOutput = true

        verify(delegate).doOutput = true
    }

    @Test
    fun testUnsetDoOutput() {
        httpsUrlConnectionDelegation.doOutput = false

        verify(delegate).doOutput = false
    }

    @Test
    fun testSetUseCaches() {
        httpsUrlConnectionDelegation.useCaches = true

        verify(delegate).useCaches = true
    }

    @Test
    fun testUnsetUseCaches() {
        httpsUrlConnectionDelegation.useCaches = false

        verify(delegate).useCaches = false
    }

    @Test
    fun testSetInstanceFollowRedirects() {
        httpsUrlConnectionDelegation.instanceFollowRedirects = true

        verify(delegate).instanceFollowRedirects = true
    }

    @Test
    fun testUnsetInstanceFollowRedirects() {
        httpsUrlConnectionDelegation.instanceFollowRedirects = false

        verify(delegate).instanceFollowRedirects = false
    }

    @Test
    fun testGetOutputStream() {
        `when`(delegate.outputStream).thenReturn(outputStream)

        assertThat(httpsUrlConnectionDelegation.outputStream).isEqualTo(outputStream)
    }

    @Test
    fun testGetResponseCode() {
        `when`(delegate.responseCode).thenReturn(RESPONSE_CODE)

        assertThat(httpsUrlConnectionDelegation.responseCode).isEqualTo(RESPONSE_CODE)
    }

    @Test
    fun testGetResponseMessage() {
        `when`(delegate.responseMessage).thenReturn(VALUE)

        assertThat(httpsUrlConnectionDelegation.responseMessage).isEqualTo(VALUE)
    }

    @Test
    fun testGetHeaderFields() {
        `when`(delegate.headerFields).thenReturn(headerFields)

        assertThat(httpsUrlConnectionDelegation.headerFields).isEqualTo(headerFields)
    }

    @Test
    fun testGetHeaderField() {
        `when`(delegate.getHeaderField(KEY)).thenReturn(VALUE)

        assertThat(httpsUrlConnectionDelegation.getHeaderField(KEY)).isEqualTo(VALUE)
    }

    @Test
    fun testGetContentLength() {
        `when`(delegate.contentLength).thenReturn(CONTENT_LENGTH)

        assertThat(httpsUrlConnectionDelegation.contentLength).isEqualTo(CONTENT_LENGTH)
    }

    @Test
    fun testGetContentEncoding() {
        `when`(delegate.contentEncoding).thenReturn(VALUE)

        assertThat(httpsUrlConnectionDelegation.contentEncoding).isEqualTo(VALUE)
    }

    @Test
    fun testGetContentType() {
        `when`(delegate.contentType).thenReturn(VALUE)

        assertThat(httpsUrlConnectionDelegation.contentType).isEqualTo(VALUE)
    }

    @Test
    fun testGetContent() {
        `when`(delegate.content).thenReturn(content)

        assertThat(httpsUrlConnectionDelegation.content).isEqualTo(content)
    }

    @Test
    fun testGetInputStream() {
        `when`(delegate.inputStream).thenReturn(inputStream)

        assertThat(httpsUrlConnectionDelegation.inputStream).isEqualTo(inputStream)
    }

    @Test
    fun testGetErrorStream() {
        `when`(delegate.errorStream).thenReturn(inputStream)

        assertThat(httpsUrlConnectionDelegation.errorStream).isEqualTo(inputStream)
    }

    @Test
    fun testGetCipherSuite() {
        assertThat(httpsUrlConnectionDelegation.cipherSuite).isNull()
    }

    @Test
    fun testGetLocalCertificates() {
        assertThat(httpsUrlConnectionDelegation.localCertificates).isNull()
    }

    @Test
    fun testGetServerCertificates() {
        assertThat(httpsUrlConnectionDelegation.serverCertificates).isNull()
    }

    @Test
    fun testSetSSLSocketFactory() {
        httpsUrlConnectionDelegation.sslSocketFactory = sslSocketFactory

        verify(delegate).url
        verifyNoMoreInteractions(delegate)
    }
}
