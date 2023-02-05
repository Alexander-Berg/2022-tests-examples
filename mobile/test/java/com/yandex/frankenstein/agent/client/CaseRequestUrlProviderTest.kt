package com.yandex.frankenstein.agent.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.Base64

private const val ARGUMENTS_KEY = "FRANKENSTEIN_TEST_ARG"
private const val CASE_REQUEST_URL = "http://42.42.100.100:4242/path?query"
private const val DEFAULT_CASE_REQUEST_URL = "http://10.0.2.2:20100/_command/case"

class CaseRequestUrlProviderTest {

    private val bundle = mock(Bundle::class.java)
    private val intent = mock(Intent::class.java)
    private val activity = mock(Activity::class.java)

    init {
        val argumentsJson = JSONObject().put("caseRequestURL", CASE_REQUEST_URL)
        val encodedArguments = encodeJson(argumentsJson)
        `when`(bundle.getString(ARGUMENTS_KEY)).thenReturn(encodedArguments)
        `when`(intent.extras).thenReturn(bundle)
        `when`(activity.intent).thenReturn(intent)
    }

    @Test
    fun testGetCaseRequestUrl() {
        val actualCaseRequestUrl = getCaseRequestUrl(activity, ::decodeBase64)

        assertThat(actualCaseRequestUrl).isEqualTo(CASE_REQUEST_URL)
    }

    @Test
    fun testGetCaseRequestUrlWithInvalidJson() {
        val encodedArguments = encodeJson(JSONObject())
        `when`(bundle.getString(ARGUMENTS_KEY)).thenReturn(encodedArguments)
        val actualCaseRequestUrl = getCaseRequestUrl(activity, ::decodeBase64)

        assertThat(actualCaseRequestUrl).isEqualTo(DEFAULT_CASE_REQUEST_URL)
    }

    @Test
    fun testGetCaseRequestUrlWithInvalidExtras() {
        `when`(bundle.getString(ARGUMENTS_KEY)).thenReturn(null)
        val actualCaseRequestUrl = getCaseRequestUrl(activity, ::decodeBase64)

        assertThat(actualCaseRequestUrl).isEqualTo(DEFAULT_CASE_REQUEST_URL)
    }

    @Test
    fun testGetCaseRequestUrlWithInvalidIntent() {
        `when`(intent.extras).thenReturn(null)
        val actualCaseRequestUrl = getCaseRequestUrl(activity, ::decodeBase64)

        assertThat(actualCaseRequestUrl).isEqualTo(DEFAULT_CASE_REQUEST_URL)
    }

    @Test
    fun testGetCaseRequestUrlWithInvalidActivity() {
        `when`(activity.intent).thenReturn(null)
        val actualCaseRequestUrl = getCaseRequestUrl(activity, ::decodeBase64)

        assertThat(actualCaseRequestUrl).isEqualTo(DEFAULT_CASE_REQUEST_URL)
    }

    private fun decodeBase64(source: String) = Base64.getDecoder().decode(source)

    private fun encodeJson(arguments: JSONObject) =
            Base64.getEncoder().encodeToString(arguments.toString().toByteArray())
}
