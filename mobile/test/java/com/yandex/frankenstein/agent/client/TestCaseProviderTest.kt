package com.yandex.frankenstein.agent.client

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
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

class TestCaseProviderTest {

    @Mock private lateinit var block: (JSONObject) -> Unit
    @Mock private lateinit var executorService: ExecutorService

    init {
        MockitoAnnotations.initMocks(this)

        `when`(executorService.execute(any())).then { it.getArgument<Runnable>(0).run() }
    }

    private val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher(executorService))
        .build()

    private val server = MockWebServer().also { it.start() }
    private val url = server.url("/path/").toString()
    private val testCase = JSONObject().put("key", "value")

    @Test
    fun testRequestTestCase() {
        server.enqueue(MockResponse().setBody(testCase.toString()))
        requestTestCase(client, url, block)

        verify(block).invoke(refEq(testCase))
    }

    @Test
    fun testRequestTestCaseWithError404() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        requestTestCase(client, url, block)

        verifyZeroInteractions(block)
    }

    @Test
    fun testRequestTestCaseWithConnectException() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        requestTestCase(client, url, block)

        verifyZeroInteractions(block)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
