package com.yandex.frankenstein.agent.client

import android.app.Activity
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.net.HttpURLConnection
import java.util.concurrent.ExecutorService

private const val BASE_URL_KEY = "baseUrl"
private const val TIMEOUT_KEY = "timeout"
private const val CLAZZ = "CommandClass"
private const val NAME = "commandName"

private const val RETRY_INTERVAL = 4.2
private const val COMMAND_TIMEOUT = 5
private const val RESULT_TIMEOUT = 6
private const val CALLBACK_TIMEOUT = 7

class TestCaseRunnerTest {

    @Mock private lateinit var activity: Activity
    @Mock private lateinit var commandImplementations: CommandImplementations
    @Mock private lateinit var testObjectStorage: TestObjectStorage
    @Mock private lateinit var block: (CommandInput) -> Unit
    @Mock private lateinit var executorService: ExecutorService

    @Captor private lateinit var inputCaptor: ArgumentCaptor<CommandInput>

    init {
        MockitoAnnotations.initMocks(this)

        `when`(executorService.execute(any())).then { it.getArgument<Runnable>(0).run() }
    }

    private val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher(executorService))
        .build()

    private val arguments = JSONObject().put("key", "value")

    private val server = MockWebServer().also { server ->
        server.start()

        val commandJson = JSONObject().put("id", "commandId")
            .put("class", CLAZZ).put("name", NAME).put("arguments", arguments)

        val commandsJson = JSONObject().put("commands", JSONArray().put(commandJson))
        server.enqueue(MockResponse().setBody(commandsJson.toString()))

        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
    }

    private val environment = JSONObject()
        .put("commandRequest", JSONObject().put("retryInterval", RETRY_INTERVAL)
            .put(BASE_URL_KEY, server.url("/path/").toString()).put(TIMEOUT_KEY, COMMAND_TIMEOUT))
        .put("commandResult", JSONObject()
            .put(BASE_URL_KEY, "http://42.42.100.100:4242/result").put(TIMEOUT_KEY, RESULT_TIMEOUT))
        .put("commandCallback", JSONObject()
            .put(BASE_URL_KEY, "http://42.42.100.100:4242/callback").put(TIMEOUT_KEY, CALLBACK_TIMEOUT))

    @Test
    fun testRunTestCase() {
        `when`(commandImplementations.get(CLAZZ, NAME)).thenReturn(block)
        runTestCase(activity, environment, commandImplementations, client, testObjectStorage)

        verify(block).invoke(capture(inputCaptor))
        val input = inputCaptor.value

        assertThat(input.activity).isEqualTo(activity)
        assertThat(input.client).isEqualTo(client)
        assertThat(input.testObjectStorage).isEqualTo(testObjectStorage)
        assertThat(input.arguments).isEqualToComparingFieldByField(arguments)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
