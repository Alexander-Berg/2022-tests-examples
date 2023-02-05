@file:Suppress("MagicNumber")
package com.yandex.frankenstein.agent.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.assertj.core.api.SoftAssertions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.ParameterizedRobolectricTestRunner
import java.util.ArrayList
import java.util.Collections

private class TestParcelable(
    val transport: String,
    val pushId: Int
) : Parcelable {

    constructor(inParcel: Parcel): this(
        transport = inParcel.readString() ?: "",
        pushId = inParcel.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(transport)
        dest.writeInt(pushId)
    }
}

private val nullAction: String? = null
private val nullType: String? = null
private val nullCategories: Set<String>? = null
private val nullExtras: Bundle? = null
private val nullDataString: String? = null
private val nullScheme: String? = null
private const val EXPECTED_JSON_STRING_WITH_NULL_VALUES = "{}"

private const val EMPTY_ACTION = ""
private const val EMPTY_TYPE = ""
private val emptyCategories = Collections.EMPTY_SET
private val emptyExtras = Bundle()
private const val EMPTY_DATA_STRING = ""
private const val EMPTY_SCHEME = ""
private const val EXPECTED_JSON_STRING_WITH_EMPTY_VALUES =
    "{\"action\"=\"\",\"categories\"=[],\"data\"=\"\",\"data_string\"=\"\",\"extras\"={}, " +
        "\"scheme\"=\"\", \"type\"=\"\"}"

private const val VALID_ACTION = "com.frankenstein.action1"
private const val VALID_TYPE = "test type"
private val validCategories = setOf<String>("category1", "category2")
private val validExtras = Bundle().apply {
    putString("extra string", "extra value 1")
    putInt("extra int", 123)
    putParcelable("extra parcelable", TestParcelable("string 1", 42))
    putStringArrayList("string list", ArrayList(listOf("str1", "str2")))
    putIntegerArrayList("integer list", ArrayList(listOf(1, 2, 3)))
    putParcelableArrayList("parcelable list", ArrayList(listOf(
        TestParcelable("string 1", 42),
        TestParcelable("string 2", 43),
        TestParcelable("string 3", 44)
    )))
}
private const val VALID_DATA_STRING = "frankenstein://test.data"
private const val VALID_SCHEME = "input scheme"
private const val EXPECTED_JSON_STRING_FOR_VALID_VALUES = "{" +
    "\"action\":\"com.frankenstein.action1\"," +
    "\"data\":\"frankenstein://test.data\"," +
    "\"type\":\"test type\"," +
    "\"categories\":[\"category1\",\"category2\"]," +
    "\"extras\":{" +
        "\"extra int\"=123," +
        "\"extra parcelable\"={\"transport\":\"string 1\",\"pushId\":42}," +
        "\"extra string\"=\"extra value 1\"," +
        "\"integer list\"=[1,2,3]," +
        "\"parcelable list\"=[" +
            "{\"transport\":\"string 1\",\"pushId\":42}," +
            "{\"transport\":\"string 2\",\"pushId\":43}," +
            "{\"transport\":\"string 3\",\"pushId\":44}]," +
        "\"string list\"=[\"str1\",\"str2\"]" +
    "}," +
    "\"data_string\":\"frankenstein://test.data\"," +
    "\"scheme\":\"input scheme\"}"

private const val ACTION = "action"
private const val DATA = "data"
private const val TYPE = "type"
private const val CATEGORIES = "categories"
private const val EXTRAS = "extras"
private const val DATA_STRING = "data_string"
private const val SCHEME = "scheme"

@RunWith(ParameterizedRobolectricTestRunner::class)
class BroadcastObserverIntentToJsonTest @Suppress("UnusedPrivateMember") constructor(
    private val inputAction: String?,
    private val inputType: String?,
    private val inputCategories: Set<String>?,
    private val inputExtras: Bundle?,
    private val inputDataString: String?,
    private val inputScheme: String?,
    private val expectedJsonString: String?,
    private val description: String
) {

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "[#{index}] {7}")
        @JvmStatic
        fun data(): Collection<Array<Any?>> = listOf(
            arrayOf<Any?>(
                nullAction, nullType, nullCategories, nullExtras, nullDataString,
                nullScheme, EXPECTED_JSON_STRING_WITH_NULL_VALUES, "Intent without values"
            ),
            arrayOf<Any?>(
                EMPTY_ACTION, EMPTY_TYPE, emptyCategories, emptyExtras, EMPTY_DATA_STRING,
                EMPTY_SCHEME, EXPECTED_JSON_STRING_WITH_EMPTY_VALUES, "Intent with empty values"
            ),
            arrayOf<Any?>(
                VALID_ACTION, VALID_TYPE, validCategories, validExtras, VALID_DATA_STRING,
                VALID_SCHEME, EXPECTED_JSON_STRING_FOR_VALID_VALUES, "Intent with valid values"
            )
        )
    }

    private lateinit var expectedJson: JSONObject

    @Mock
    private lateinit var inputIntent: Intent

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(inputIntent.action).thenReturn(inputAction)
        `when`(inputIntent.data).thenReturn(
            when (inputDataString) {
                null -> null
                "" -> Uri.EMPTY
                else -> Uri.parse(inputDataString)
            }
        )
        `when`(inputIntent.type).thenReturn(inputType)
        `when`(inputIntent.categories).thenReturn(inputCategories)
        `when`(inputIntent.extras).thenReturn(inputExtras)
        `when`(inputIntent.dataString).thenReturn(inputDataString)
        `when`(inputIntent.scheme).thenReturn(inputScheme)

        expectedJson = JSONObject(expectedJsonString)
    }

    @Test
    fun testIntentToResult() {
        val inputJson = inputIntent.toResult()

        val softAssertions = SoftAssertions()

        softAssertions.assertThat(inputJson.optString(ACTION, null))
            .`as`(ACTION)
            .isEqualTo(expectedJson.optString(ACTION, null))

        softAssertions.assertThat(inputJson.optString(DATA, null))
            .`as`(DATA)
            .isEqualTo(expectedJson.optString(DATA, null))

        softAssertions.assertThat(inputJson.optString(TYPE, null))
            .`as`(TYPE)
            .isEqualTo(expectedJson.optString(TYPE, null))

        softAssertions.assertThat(inputJson.optJSONArray(CATEGORIES).toStringList())
            .`as`(CATEGORIES)
            .containsExactlyInAnyOrderElementsOf(inputJson.optJSONArray(CATEGORIES).toStringList())

        softAssertions.assertThat(inputJson.optJSONObject(EXTRAS).toJsonElement())
            .`as`(EXTRAS)
            .isEqualTo(expectedJson.optJSONObject(EXTRAS).toJsonElement())

        softAssertions.assertThat(inputJson.optString(DATA_STRING, null))
            .`as`(DATA_STRING)
            .isEqualTo(expectedJson.optString(DATA_STRING, null))

        softAssertions.assertThat(inputJson.optString(SCHEME, null))
            .`as`(SCHEME)
            .isEqualTo(expectedJson.optString(SCHEME, null))

        softAssertions.assertAll()
    }

    private fun JSONObject?.toJsonElement(): JsonElement? =
        this?.let { JsonParser.parseString(it.toString()) }

    private fun JSONArray?.toStringList(): List<String?> =
        this?.let { array ->
            (0 until array.length()).map { idx -> array.getString(idx) }
        } ?: emptyList()
}
