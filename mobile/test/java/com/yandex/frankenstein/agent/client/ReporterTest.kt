package com.yandex.frankenstein.agent.client

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations
import java.net.HttpURLConnection
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private const val TIMEOUT = 5L
private const val RESPONSE = "response"

class ReporterTest {

    @Mock private lateinit var completion: (String) -> Unit
    @Mock private lateinit var executorService: ExecutorService

    init {
        MockitoAnnotations.initMocks(this)

        `when`(executorService.execute(any())).then { it.getArgument<Runnable>(0).run() }
    }

    private var actualTimeout by Delegates.notNull<Int>()
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            actualTimeout = chain.readTimeoutMillis()
            chain.proceed(chain.request())
        }
        .dispatcher(Dispatcher(executorService))
        .build()

    private val server = MockWebServer().also { it.start() }
    private val reporter = Reporter(client, TIMEOUT)
    private val url = server.url("/path/")
    private val result = JSONObject().put("key", "value")

    @Test
    fun testTimeout() {
        server.enqueue(MockResponse())
        reporter.report(url, result, completion)

        val expectedTimeout = TimeUnit.SECONDS.toMillis(TIMEOUT)
        assertThat(actualTimeout).isEqualTo(expectedTimeout)
    }

    @Test
    fun testResult() {
        server.enqueue(MockResponse().setBody(RESPONSE))
        reporter.report(url, result, completion)

        val request = server.takeRequest()
        val actualResult = JSONObject(request.body.readUtf8())
        assertThat(actualResult).isEqualToComparingFieldByField(result)
    }

    @Test
    fun testReport() {
        server.enqueue(MockResponse().setBody(RESPONSE))
        reporter.report(url, result, completion)

        verify(completion).invoke(RESPONSE)
    }

    @Test
    fun testReportWithCode299() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_MULT_CHOICE - 1).setBody(RESPONSE))
        reporter.report(url, result, completion)

        verify(completion).invoke(RESPONSE)
    }

    @Test
    fun testReportWithError404() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND).setBody(RESPONSE))
        reporter.report(url, result, completion)

        verifyZeroInteractions(completion)
    }

    @Test
    fun testRequestTestCaseWithConnectException() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        reporter.report(url, result, completion)

        verifyZeroInteractions(completion)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
