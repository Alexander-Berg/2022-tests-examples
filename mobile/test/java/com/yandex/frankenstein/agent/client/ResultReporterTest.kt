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
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutorService

private const val COMMAND_ID = "commandId"
private const val TIMEOUT = 5

class ResultReporterTest {

    @Mock private lateinit var result: JSONObject
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
        server.enqueue(MockResponse())
    }

    private val baseUrl = server.url("/path/")
    private val parameters = JSONObject().put("baseUrl", baseUrl.toString()).put("timeout", TIMEOUT)
    private val resultReporter = ResultReporter(client, parameters)

    @Test
    fun testUrl() {
        resultReporter.report(COMMAND_ID, result)

        val url = server.takeRequest().requestUrl
        assertThat(url.scheme()).isEqualTo(baseUrl.scheme())
        assertThat(url.host()).isEqualTo(baseUrl.host())
        assertThat(url.port()).isEqualTo(baseUrl.port())
        assertThat(url.encodedPath()).isEqualTo(baseUrl.encodedPath())
        assertThat(url.queryParameter("commandId")).isEqualTo(COMMAND_ID)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
