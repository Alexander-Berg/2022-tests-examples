package com.yandex.frankenstein.agent.client

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.calls
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations
import java.net.HttpURLConnection
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private const val COMMAND_ID = "commandId"

private const val RETRY_INTERVAL = 4.2
private const val TIMEOUT = 5L
private const val MILLIS_IN_SECOND = 1000

class CommandsProviderTest {

    @Mock private lateinit var sleep: (Long) -> Unit
    @Mock private lateinit var processCommands: (List<Command>) -> Unit
    @Mock private lateinit var executorService: ExecutorService

    @Captor private lateinit var commandsCaptor: ArgumentCaptor<List<Command>>

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
    private val baseUrl = server.url("/path/")

    private val commandJson = JSONObject().put("id", COMMAND_ID).put("class", "CommandClass")
        .put("name", "commandName").put("arguments", JSONObject().put("key", "value"))

    private val commandsJson = JSONObject().put("commands", JSONArray().put(commandJson))

    private val parameters = JSONObject().put("baseUrl", baseUrl.toString())
            .put("retryInterval", RETRY_INTERVAL).put("timeout", TIMEOUT)

    private val commandsProvider = CommandsProvider(client, parameters, sleep)

    @Test
    fun testTimeout() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        commandsProvider.request(processCommands)

        val expectedTimeout = TimeUnit.SECONDS.toMillis(TIMEOUT)
        assertThat(actualTimeout).isEqualTo(expectedTimeout)
    }

    @Test
    fun testSuccessfulRequest() {
        server.enqueue(MockResponse().setBody(commandsJson.toString()))
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        commandsProvider.request(processCommands)

        verifyDelays(0, 0)
        assertRequests(null, COMMAND_ID)

        verify(processCommands).invoke(capture(commandsCaptor))
        assertThat(commandsCaptor.value)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(Command(commandJson))
    }

    @Test
    fun testSuccessfulRequestWithoutCommands() {
        val emptyCommandsJson = JSONObject().put("commands", JSONArray())
        server.enqueue(MockResponse().setBody(emptyCommandsJson.toString()))
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        commandsProvider.request(processCommands)

        verifyDelays(0, 0)
        assertRequests(null, null)

        verify(processCommands).invoke(capture(commandsCaptor))
        assertThat(commandsCaptor.value).isEmpty()
    }

    @Test
    fun testRequestWithError404() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        commandsProvider.request(processCommands)

        verifyDelays(0)
        assertRequests(null)
        verifyZeroInteractions(processCommands)
    }

    @Test
    fun testRequestWithError504() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_GATEWAY_TIMEOUT))
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        commandsProvider.request(processCommands)

        verifyDelays(0, Math.round(RETRY_INTERVAL * MILLIS_IN_SECOND))
        assertRequests(null, null)
        verifyZeroInteractions(processCommands)
    }

    @Test
    fun testRequestWithSocketTimeoutException() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND))
        commandsProvider.request(processCommands)

        verifyDelays(0, Math.round(RETRY_INTERVAL * MILLIS_IN_SECOND))
        assertRequests(null, null)
        verifyZeroInteractions(processCommands)
    }

    @Test
    fun testRequestWithConnectException() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        commandsProvider.request(processCommands)

        verifyDelays(0)
        assertRequests(null)
        verifyZeroInteractions(processCommands)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun verifyDelays(vararg delays: Long) {
        val inOrder = inOrder(sleep)
        for (delay in delays) {
            inOrder.verify(sleep, calls(1)).invoke(delay)
        }
        inOrder.verifyNoMoreInteractions()
    }

    private fun assertRequests(vararg lastCommandsIds: String?) {
        assertThat(server.requestCount).isEqualTo(lastCommandsIds.size)

        for (lastCommandId in lastCommandsIds) {
            val url = server.takeRequest().requestUrl

            assertThat(url.scheme()).isEqualTo(baseUrl.scheme())
            assertThat(url.host()).isEqualTo(baseUrl.host())
            assertThat(url.port()).isEqualTo(baseUrl.port())
            assertThat(url.encodedPath()).isEqualTo(baseUrl.encodedPath())
            assertThat(url.queryParameter("lastCommandId")).isEqualTo(lastCommandId)
        }
    }
}
