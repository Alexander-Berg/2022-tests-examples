package com.yandex.frankenstein.agent.client

import android.app.Activity
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

private const val COMMAND_ID = "commandId"
private const val LISTENER_ID = "listenerId"
private const val CALLBACK = "callback"

class CommandInputTest {

    @Mock private lateinit var activity: Activity
    @Mock private lateinit var client: OkHttpClient
    @Mock private lateinit var testObjectStorage: TestObjectStorage
    @Mock private lateinit var arguments: JSONObject
    @Mock private lateinit var resultReporter: ResultReporter
    @Mock private lateinit var callbackReporter: CallbackReporter
    @Mock private lateinit var result: JSONObject
    @Mock private lateinit var completion: (JSONObject) -> Unit

    init {
        MockitoAnnotations.initMocks(this)
    }

    private val input = CommandInput(activity, client, testObjectStorage,
            COMMAND_ID, arguments, resultReporter, callbackReporter)

    @Test
    fun testReportResult() {
        input.reportResult(result)

        verify(resultReporter).report(COMMAND_ID, result)
    }

    @Test
    fun testReportResultWithoutResult() {
        input.reportResult()

        verify(resultReporter).report(eq(COMMAND_ID), refEq(JSONObject()))
    }

    @Test
    fun testReportCallback() {
        input.reportCallback(LISTENER_ID, CALLBACK, result, completion)

        verify(callbackReporter).report(LISTENER_ID, CALLBACK, result, completion)
    }

    @Test
    fun testReportCallbackWithoutListenerId() {
        input.reportCallback(callback = CALLBACK, result = result, completion = completion)

        verify(callbackReporter).report(COMMAND_ID, CALLBACK, result, completion)
    }

    @Test
    fun testReportCallbackWithoutResult() {
        input.reportCallback(LISTENER_ID, CALLBACK, completion = completion)

        verify(callbackReporter).report(eq(LISTENER_ID), eq(CALLBACK), refEq(JSONObject()), eq(completion))
    }
}
