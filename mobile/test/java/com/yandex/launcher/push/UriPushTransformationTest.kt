package com.yandex.launcher.push

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.yandex.launcher.common.util.UriUtils
import com.yandex.launcher.app.TestApplication
import com.yandex.metrica.push.core.model.PushMessage
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull.notNullValue
import org.json.JSONObject
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_KEY = "test_key"
private const val TEST_VALUE = "test_value"

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [26],
    manifest = Config.NONE,
    packageName = "com.yandex.launcher",
    application = TestApplication::class
)
class UriPushTransformationTest {

    private val testJsonPattern = JSONObject("{\n" +
            "  \"a\": \"reminders.calendar\",\n" +
            "  \"b\": false,\n" +
            "  \"c\": '{\"push_action\":\"%s\",\"push_id\":\"reminders.calendar\",\"push_uri\":\"%s\"}',\n" +
            "  \"d\": {\n" +
            "    \"a\": 1356478945,\n" +
            "    \"e\": \"Алиса: напоминание\",\n" +
            "    \"g\": \"поесть\",\n" +
            "    \"w\": \"%s\",\n" +
            "    \"y\": \"some-url\",\n" +
            "    \"j\": 7\n" +
            "  }\n" +
            "}\n").toString()
    private val testUri = "dialog://blablabla"
    private val originUriJson = testJsonPattern.format("uri", testUri, testUri)
    private val resultJson = testJsonPattern.format("uri", UriUtils.getOpenLinkUri(testUri), UriUtils.getOpenLinkUri(testUri))
    private val notUriPushMessageJson = testJsonPattern.format("not_uri", testUri, testUri)
    private lateinit var uriPushHandler: UriPushHandler

    @Before
    fun setUp() {
        uriPushHandler = UriPushHandler(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `message has push_action uri, it's uri push`() {
        MatcherAssert.assertThat(UriPushHandler.isUriPush(createTestPushMessage(createTestBundle(originUriJson))), `is`(true))
    }

    @Test
    fun `message has push_action not uri, it's not uri push`() {
        MatcherAssert.assertThat(UriPushHandler.isUriPush(createTestPushMessage(createTestBundle(notUriPushMessageJson))), `is`(false))
    }

    @Test
    fun `handle push message, uri replaced with launcher uri`() {
        val result = uriPushHandler.transform(createTestPushMessage(createTestBundle(originUriJson)))

        MatcherAssert.assertThat(processResultString(result.bundle.getString(PUSH_DATA_KEY)!!), `is`(resultJson))
    }

    @Test
    fun `handle push message, all other keys presented in result json`() {
        val testBundle = createTestBundle(originUriJson)
        Assume.assumeThat(testBundle.get(TEST_KEY), notNullValue())
        val result = uriPushHandler.transform(createTestPushMessage(testBundle))

        MatcherAssert.assertThat(result.bundle.get(TEST_KEY), `is`(testBundle.get(TEST_KEY)))
    }

    private fun createTestBundle(json: String): Bundle {
        val result = Bundle()
        result.putString(TEST_KEY, TEST_VALUE)
        result.putString(PUSH_DATA_KEY, json)
        return result
    }

    private fun createTestPushMessage(bundle: Bundle): PushMessage {
        return PushMessage(ApplicationProvider.getApplicationContext(), bundle)
    }

    private fun processResultString(result: String): String {
        return result.replace("\\/", "/").replace("\\/", "/").replace("\\/", "/")
    }
}
