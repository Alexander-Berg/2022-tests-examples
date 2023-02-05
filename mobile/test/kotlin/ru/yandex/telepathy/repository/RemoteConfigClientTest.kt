// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.telepathy.repository

import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import ru.yandex.telepathy.ConfigSource
import ru.yandex.telepathy.TelepathistAssignment
import ru.yandex.telepathy.exception.EmptyResponseException
import ru.yandex.telepathy.exception.HttpResponseCodeException
import ru.yandex.telepathy.exception.MalformedResponseException

class RemoteConfigClientTest {

    private lateinit var testResponse: Response

    private lateinit var remoteConfigClient: RemoteConfigClient

    private val assignment = TelepathistAssignment.compose()
        .loadFrom(ConfigSource.custom("https://ya.ru"))
        .sealInEnvelope()

    @Before
    fun runBeforeAnyTest() {
        remoteConfigClient = object : RemoteConfigClient() {
            override fun loadConfig(assignment: TelepathistAssignment) = testResponse
        }
    }

    @Test
    fun fetchConfig_whenLoadingJsonObject_shouldReturnMap() {
        testResponse = makeResponse(200, "{\"key\":\"value\"}")
        assertThat(remoteConfigClient.fetchConfig(assignment)).isEqualTo(mapOf("key" to "value"))
    }

    @Test
    fun fetchConfig_whenLoadingJsonArray_shouldReturnList() {
        testResponse = makeResponse(200, "[1, 2, 3]")
        assertThat(remoteConfigClient.fetchConfig(assignment)).isEqualTo(listOf(1.0, 2.0, 3.0))
    }

    @Test
    fun fetchConfig_whenLoadingJsonStringPrimitive_shouldReturnString() {
        testResponse = makeResponse(200, "\"test\"")
        assertThat(remoteConfigClient.fetchConfig(assignment)).isEqualTo("test")
    }

    @Test
    fun fetchConfig_whenLoadingJsonNumberPrimitive_shouldReturnDouble() {
        testResponse = makeResponse(200, "42")
        assertThat(remoteConfigClient.fetchConfig(assignment))
            .isEqualTo(42.0)
            .isExactlyInstanceOf(java.lang.Double::class.java)
    }

    @Test
    fun fetchConfig_whenLoadingJsonBooleanPrimitive_shouldReturnBoolean() {
        testResponse = makeResponse(200, "true")
        assertThat(remoteConfigClient.fetchConfig(assignment)).isEqualTo(true)
    }

    @Test
    fun fetchConfig_whenLoadingJsonNull_shouldReturnNull() {
        testResponse = makeResponse(200, "null")
        assertThat(remoteConfigClient.fetchConfig(assignment)).isEqualTo(null)
    }

    @Test
    fun fetchConfig_whenInvalidContentType_shouldReturnMap() {
        testResponse = makeResponse(200, "{\"key\":\"value\"}", contentType = "text/plain")
        assertThat(remoteConfigClient.fetchConfig(assignment)).isEqualTo(mapOf("key" to "value"))
    }

    @Test
    fun fetchConfig_whenRedirect_shouldThrowException() {
        testResponse = makeResponse(300, "{\"key\":\"value\"}")
        assertThatThrownBy { remoteConfigClient.fetchConfig(assignment) }
            .isExactlyInstanceOf(HttpResponseCodeException::class.java)
    }

    @Test
    fun fetchConfig_whenNotFound_shouldThrowException() {
        testResponse = makeResponse(404, "{\"key\":\"value\"}")
        assertThatThrownBy { remoteConfigClient.fetchConfig(assignment) }
            .isExactlyInstanceOf(HttpResponseCodeException::class.java)
    }

    @Test
    fun fetchConfig_whenServerError_shouldThrowException() {
        testResponse = makeResponse(512, "{\"key\":\"value\"}")
        assertThatThrownBy { remoteConfigClient.fetchConfig(assignment) }
            .isExactlyInstanceOf(HttpResponseCodeException::class.java)
    }

    @Test
    fun fetchConfig_whenMalformedJson_shouldThrowException() {
        testResponse = makeResponse(200, "{\"key\":\"value\"")
        assertThatThrownBy { remoteConfigClient.fetchConfig(assignment) }
            .isExactlyInstanceOf(MalformedResponseException::class.java)
    }

    @Test
    fun fetchConfig_whenEmptyBody_shouldThrowException() {
        testResponse = makeResponse(200, "")
        testResponse = testResponse.newBuilder().body(null).build()
        assertThatThrownBy { remoteConfigClient.fetchConfig(assignment) }
            .isExactlyInstanceOf(EmptyResponseException::class.java)
    }

    private fun makeResponse(code: Int, body: String, contentType: String = "application/json"): Response {
        return Response.Builder()
            .request(Request.Builder().url(assignment.configSource.url).build())
            .code(code)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .body(ResponseBody.create(MediaType.parse(contentType), body))
            .build()
    }
}