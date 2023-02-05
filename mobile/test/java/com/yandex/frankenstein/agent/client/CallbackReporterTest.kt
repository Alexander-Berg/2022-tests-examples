package com.yandex.frankenstein.agent.client

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutorService

private const val LISTENER_ID = "listenerId"
private const val CALLBACK = "callbackName"

private const val TIMEOUT = 5

class CallbackReporterTest {

    private val response = JSONObject().put("key", "value")

    @Mock private lateinit var result: JSONObject
    @Mock private lateinit var completion: (JSONObject) -> Unit
    @Mock private lateinit var executorService: ExecutorService

    init {
        MockitoAnnotations.initMocks(this)

        `when`(executorService.execute(any())).then { it.getArgument<Runnable>(0).run() }
    }

    private val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher(executorService))
        .build()

    private val server = MockWebServer().also { server ->
        server.start()
        server.enqueue(MockResponse().setBody(response.toString()))
    }

    private val baseUrl = server.url("/path/")
    private val parameters = JSONObject().put("baseUrl", baseUrl.toString()).put("timeout", TIMEOUT)
    private val callbackReporter = CallbackReporter(client, parameters)

    @Test
    fun testUrl() {
        callbackReporter.report(LISTENER_ID, CALLBACK, result, completion)

        val url = server.takeRequest().requestUrl
        assertThat(url.scheme()).isEqualTo(baseUrl.scheme())
        assertThat(url.host()).isEqualTo(baseUrl.host())
        assertThat(url.port()).isEqualTo(baseUrl.port())
        assertThat(url.encodedPath()).isEqualTo(baseUrl.encodedPath())
        assertThat(url.queryParameter("listenerId")).isEqualTo(LISTENER_ID)
        assertThat(url.queryParameter("callback")).isEqualTo(CALLBACK)
    }

    @Test
    fun testCompletion() {
        callbackReporter.report(LISTENER_ID, CALLBACK, result, completion)

        verify(completion).invoke(refEq(response))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
