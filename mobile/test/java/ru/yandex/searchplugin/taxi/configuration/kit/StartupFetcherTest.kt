/*
 * This file is a part of the Yandex Search for Android project.
 *
 * (C) Copyright 2019. Yandex, LLC. All rights reserved.
 *
 * Author: Alexander Skvortsov <askvortsov@yandex-team.ru>
 */

package ru.yandex.searchplugin.taxi.configuration.kit

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.Headers
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.ExecutorService

@RunWith(JUnit4::class)
class StartupFetcherTest {
    private var prefsUserId: String? = null
    private val installId = "install_id"
    private val deviceId = "device_id"
    private val gcmToken = "gcm_token_string"
    private val locale = "test_locale"
    private val userAgent = "test_user_agent"
    private val persistence = mock<Persistence> {
        on { userId } doAnswer { prefsUserId }
    }

    private val call = mock<Call<StartupResponseJson>>()
    private val startupApi = mock<StartupApi> {
        on { startupResponse(anyOrNull(), anyOrNull(), any(), any(), any()) } doReturn call
    }
    private val executorService = mock<ExecutorService> {
        on { execute(any()) } doAnswer { (it.arguments[0] as Runnable).run() }
    }
    private var deferredUiCalls = mutableListOf<() -> Unit>()

    private val me = StartupFetcher(
        { startupApi },
        executorService,
        persistence,
        object : InstallIdProvider {
            override fun getInstallId(): String? = installId
            override fun getDeviceId(): String? = deviceId
        },
        { gcmToken },
        { userAgent },
        { locale },
        { deferredUiCalls.add(it) }
    )

    @Test
    fun successfulResponse() {
        val json = StartupResponseJson()
        when (val response = getResponse(Response.success(json, HEADERS_WITH_USER_ID))) {
            is StartupFetcher.Result.Success ->
                assertSame(json, response.value)
            else ->
                fail()
        }
    }

    @Test
    fun passesNonNullUserId() {
        prefsUserId = "test_id"
        getResponse(Response.success(null))
        verify(startupApi).startupResponse(eq("Bearer $OAUTH_TOKEN"), same(prefsUserId), same(locale), same(userAgent), any())
    }

    @Test
    fun passesNullUserId() {
        getResponse(Response.success(null))
        verify(startupApi).startupResponse(eq("Bearer $OAUTH_TOKEN"), isNull(), same(locale), same(userAgent), any())
    }

    @Test
    fun passesNullOauthToken() {
        getResponse(Response.success(null), null)
        verify(startupApi).startupResponse(isNull(), isNull(), same(locale), same(userAgent), any())
    }

    @Test
    fun persistsUserId() {
        whenever(call.execute()).thenReturn(Response.success(null as StartupResponseJson?, HEADERS_WITH_USER_ID))
        me.fetchStartupResponse(OAUTH_TOKEN) { _, _, _ -> }
        verify(persistence).userId = SERVER_USER_ID
    }

    @Test
    fun passesGcmTokenAndInstallId() {
        var body: StartupRequestBody? = null
        whenever(startupApi.startupResponse(any(), isNull(), any(), any(), any())).thenAnswer {
            assertNull(body)
            body = it.arguments[4] as StartupRequestBody
            call
        }
        me.fetchStartupResponse(OAUTH_TOKEN) { _, _, _ -> }
        assertEquals(gcmToken, body?.gcmToken)
        assertEquals(installId, body?.installId)
        assertEquals(deviceId, body?.deviceId)
    }

    @Test
    fun unknownError() {
        when (getResponse(Response.error(403, ResponseBody.create(null, "")))) {
            is StartupFetcher.Result.Error -> return
            else -> fail()
        }
    }

    @Test
    fun whenRequestIsCanceledCancelsCall() {
        val token = me.fetchStartupResponse(OAUTH_TOKEN) { _, _, _ -> }
        token?.cancel()
        verify(call).cancel()
    }

    @Test
    fun whenRequestIsCanceledDoesNotExecuteCall() {
        whenever(call.isCanceled).thenReturn(true)
        me.fetchStartupResponse(OAUTH_TOKEN) { _, _, _ -> }
        verify(call, never()).execute()
    }

    @Test
    fun whenRequestIsCanceledDuringExecutionDoesNotCallCompletion() {
        whenever(call.execute()).thenReturn(Response.success(null))
        me.fetchStartupResponse(OAUTH_TOKEN) { _, _, _ -> fail() }
        whenever(call.isCanceled).thenReturn(true)
        performUiCalls()
    }

    @Test
    fun humanReadableError() {
        val errorCode = "test_error_code"
        val errorText = "test_error_text"
        val responseBody = """
        { "code": "$errorCode", "message": "$errorText" }
        """
        val httpResponse: Response<StartupResponseJson> =
            Response.error(401, ResponseBody.create(null, responseBody))
        when (val result = getResponse(httpResponse)) {
            is StartupFetcher.Result.Error ->
                assertEquals(StartupErrorResponseJson(errorCode, errorText), result.response?.body)
            else ->
                fail()
        }
    }

    @Test
    fun networkError() {
        val exception = IOException("test")
        whenever(call.execute()).doThrow(exception)
        me.fetchStartupResponse(OAUTH_TOKEN) { _, _, result ->
            assertEquals(StartupFetcher.Result.Error(error = exception), result)
        }
        performUiCalls()
    }

    private fun getResponse(
        httpResponse: Response<StartupResponseJson>,
        oauthToken: String? = OAUTH_TOKEN
    ): StartupFetcher.Result? {
        whenever(call.execute()).doReturn(httpResponse)

        var receivedResponse: StartupFetcher.Result? = null
        var requestToken: StartupFetcher.RequestToken? = null
        requestToken = me.fetchStartupResponse(oauthToken) { receivedRequestToken, _, result ->
            receivedResponse = result
            assertSame(requestToken, receivedRequestToken)
        }
        performUiCalls()

        assertTrue("Completion must be called", receivedResponse != null)
        return receivedResponse
    }

    private fun performUiCalls(expectedCount: Int = 1) {
        assertEquals(expectedCount, deferredUiCalls.count())
        deferredUiCalls.forEach { it() }
        deferredUiCalls.clear()
    }
}

private const val SERVER_USER_ID = "server_id"
private const val OAUTH_TOKEN = "test_oauth_token"
private val HEADERS_WITH_USER_ID = Headers.Builder()
    .add(StartupApi.TAXI_USER_ID_HEADER, SERVER_USER_ID)
    .build()
