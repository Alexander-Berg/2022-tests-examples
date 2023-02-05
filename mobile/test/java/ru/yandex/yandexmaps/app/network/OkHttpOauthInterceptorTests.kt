package ru.yandex.yandexmaps.app.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import ru.yandex.yandexmaps.auth.service.rx.api.RxAuthService

class OkHttpOauthInterceptorTests {

    @get:Rule
    val server = MockWebServer()

    @Mock
    private lateinit var authService: RxAuthService

    private lateinit var mocksCloseable: AutoCloseable

    @Before
    fun setup() {
        mocksCloseable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun teardown() {
        mocksCloseable.close()
    }

    @Test
    fun `Does not fail if 401 and has invalidated token`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Not authorized"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("Good job"))

        `when`(authService.blockingGetToken()).thenReturn("oauth token")

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(Network.createOkHttpOauthInterceptor(authService))
            .build()

        val response = okHttpClient.newCall(Request.Builder().url(server.url("/")).build()).execute()

        Assert.assertEquals(200, response.code)
        Assert.assertEquals("Good job", response.body!!.string())

        Assert.assertEquals(2, server.requestCount)

        val request1 = server.takeRequest()
        Assert.assertEquals(server.url("/").toString(), request1.requestUrl.toString())

        val request2 = server.takeRequest()
        Assert.assertEquals(server.url("/").toString(), request2.requestUrl.toString())
    }

    @Test
    fun `Does not fail if 401 and has no invalidated token`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Not authorized"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("Good job"))

        `when`(authService.blockingGetToken()).thenReturn("oauth token", null)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(Network.createOkHttpOauthInterceptor(authService))
            .build()

        val response = okHttpClient.newCall(Request.Builder().url(server.url("/")).build()).execute()

        Assert.assertEquals(200, response.code)
        Assert.assertEquals("Good job", response.body!!.string())

        Assert.assertEquals(2, server.requestCount)

        val request1 = server.takeRequest()
        Assert.assertEquals(server.url("/").toString(), request1.requestUrl.toString())

        val request2 = server.takeRequest()
        Assert.assertEquals(server.url("/").toString(), request2.requestUrl.toString())
    }
}
