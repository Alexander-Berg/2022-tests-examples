@file:Suppress("FunctionMaxLength")

package com.yandex.frankenstein.agent.client

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.IntentFilter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.iterable.Extractor
import org.assertj.core.groups.Tuple
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.ParameterizedRobolectricTestRunner

private const val ID = 42
private const val EMPTY_JSON = "{\"intent_filter\":{},\"id\":$ID}"
private const val JSON_WITH_EMPTY_BLOCKS =
    "{\"intent_filter\":{\"actions\":[],\"categories\":[],\"data_authorities\":[],\"data_paths\":[]," +
        "\"data_schemes\":[],\"data_types\":[]},\"id\":$ID}"

private const val JSON_WITH_SINGLE_ITEMS =
    "{\"intent_filter\":{\"actions\":[\"com.frankenstein.action1\"]," +
        "\"categories\":[\"com.frankenstein.category1\"]," +
        "\"data_authorities\":[{\"host\":\"frankenstein1.com\",\"port\":\"8000\"}]," +
        "\"data_paths\":[{\"path\":\"frankenstein.path.1\",\"type\":0}]," +
        "\"data_schemes\":[\"frankenstein.scheme.1\"],\"data_types\":[\"vnd.com.yandex.frankenstein/type1\"]}," +
        "\"id\":$ID}"

private const val JSON_WITH_MULTIPLE_ITEMS =
    "{\"intent_filter\":{\"actions\":[\"com.frankenstein.action1\",\"com.frankenstein.actoins2\"]," +
        "\"categories\":[\"com.frankenstein.category1\",\"com.frankenstein.category2\"]," +
        "\"data_authorities\":[{\"host\":\"frankenstein1.com\",\"port\":\"8000\"}," +
        "{\"host\":\"frankenstein2.com\",\"port\":\"8001\"}]," +
        "\"data_paths\":[{\"path\":\"frankenstein.path.1\"," +
        "\"type\":0},{\"path\":\"frankenstein.path.2\",\"type\":0}]," +
        "\"data_schemes\":[\"frankenstein.scheme.1\",\"frankenstein.scheme.2\"]," +
        "\"data_types\":[\"vnd.com.yandex.frankenstein/type1\",\"vnd.com.yandex.frankenstein/type2\"]},\"id\":$ID}"

private val emptyActions = emptyList<String>()
private val emptyCategories = null
private val emptyAuthorities = null
private val emptyDataPaths = null
private val emptyDataSchemes = null
private val emptyDataTypes = null

private val singleActions = listOf("com.frankenstein.action1")
private val singleCategory = listOf("com.frankenstein.category1")

private const val AUTHORITIES_PORT = 8000
private const val AUTHORITIES_ANOTHER_PORT = 8001
private const val DATA_PATH_TYPE = 0
private val singleAuthorities = listOf("frankenstein1.com" to AUTHORITIES_PORT)
private val singleDataPath = listOf("frankenstein.path.1" to DATA_PATH_TYPE)
private val singleScheme = listOf("frankenstein.scheme.1")
private val singleDataType = listOf("vnd.com.yandex.frankenstein/type1")

private val multipleActions = listOf("com.frankenstein.action1", "com.frankenstein.actoins2")
private val multipleCategories = listOf("com.frankenstein.category1", "com.frankenstein.category2")
private val multipleAuthorities =
    listOf("frankenstein1.com" to AUTHORITIES_PORT, "frankenstein2.com" to AUTHORITIES_ANOTHER_PORT)
private val multipleDataPaths =
    listOf("frankenstein.path.1" to DATA_PATH_TYPE, "frankenstein.path.2" to DATA_PATH_TYPE)
private val multipleSchemes = listOf("frankenstein.scheme.1", "frankenstein.scheme.2")
private val multipleDataTypes = listOf("vnd.com.yandex.frankenstein/type1", "vnd.com.yandex.frankenstein/type2")

@RunWith(ParameterizedRobolectricTestRunner::class)
class BroadcastObserverAwaitBroadcastReceiveTest @Suppress("UnusedPrivateMember") constructor(
    val inputJsonString: String?,
    val expectedActions: List<String?>?,
    val expectedCategories: List<String?>?,
    val expectedAuthorities: List<Pair<String?, Int?>?>?,
    val expectedDataPaths: List<Pair<String?, Int?>?>?,
    val expectedDataSchemes: List<String?>?,
    val expectedDataTypes: List<String?>?,
    val description: String
) {

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "[#{index}] {7}")
        @JvmStatic
        fun data(): Collection<Array<Any?>> = listOf(
            arrayOf<Any?>(
                EMPTY_JSON, emptyActions, emptyCategories, emptyAuthorities,
                emptyDataPaths, emptyDataSchemes, emptyDataTypes, "for empty intent filter"
            ),
            arrayOf<Any?>(
                JSON_WITH_EMPTY_BLOCKS, emptyActions, emptyCategories, emptyAuthorities,
                emptyDataPaths, emptyDataSchemes, emptyDataTypes, "for intent filter with empty parameters"
            ),
            arrayOf<Any?>(
                JSON_WITH_SINGLE_ITEMS, singleActions, singleCategory, singleAuthorities,
                singleDataPath, singleScheme, singleDataType, "for intent filter with single parameters"
            ),
            arrayOf<Any?>(
                JSON_WITH_MULTIPLE_ITEMS, multipleActions, multipleCategories, multipleAuthorities,
                multipleDataPaths, multipleSchemes, multipleDataTypes, "for intent with multiple parameters"
            )
        )
    }

    private lateinit var inputJson: JSONObject

    @Mock
    private lateinit var commandInput: CommandInput
    @Mock
    private lateinit var application: Application
    @Mock
    private lateinit var activity: Activity

    @Captor
    private lateinit var intentFilterCaptor: ArgumentCaptor<IntentFilter>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        inputJson = JSONObject(inputJsonString)

        `when`(activity.application).thenReturn(application)
        `when`(commandInput.activity).thenReturn(activity)
        `when`(commandInput.arguments).thenReturn(inputJson)

        awaitBroadcastReceive(commandInput)
    }

    @Test
    fun testAwaitBroadcastReceiveReportResult() {
        verify(commandInput).reportResult(any())
    }

    @Test
    fun testAwaitBroadcastReceiverRegisterBroadcastReceiverWithExpectedAction() {
        assertThat(interceptIntentFilter().actionsIterator().asSequence().toList())
            .containsOnlyElementsOf(expectedActions)
    }

    @Test
    fun testAwaitBroadcastReceiverRegisterBroadcastReceiverWithExpectedCategory() {
        if (expectedCategories == null) {
            assertThat(interceptIntentFilter().categoriesIterator()).isNull()
        } else {
            assertThat(interceptIntentFilter().categoriesIterator().asSequence().toList())
                .containsOnlyElementsOf(expectedCategories)
        }
    }

    @Test
    fun testAwaitBroadcastReceiverRegisterBroadcastReceiverWithExpectedAuthorities() {
        if (expectedAuthorities == null) {
            assertThat(interceptIntentFilter().authoritiesIterator()?.asSequence()?.toList()).isNullOrEmpty()
        } else {
            assertThat(interceptIntentFilter().authoritiesIterator().asSequence().toList())
                .extracting(Extractor<IntentFilter.AuthorityEntry, Tuple> { tuple(it.host, it.port) })
                .containsOnlyElementsOf(expectedAuthorities.map { tuple(it?.first, it?.second) })
        }
    }

    @Test
    fun testAwaitBroadcastReceiverRegisterBroadcastReceiverWithExpectedDataPaths() {
        val intentFilter = interceptIntentFilter()
        if (expectedDataPaths == null) {
            assertThat(intentFilter.countDataPaths()).isZero()
        } else {
            val actualValues = ArrayList<Tuple>()
            repeat(intentFilter.countDataPaths()) {
                actualValues.add(tuple(intentFilter.getDataPath(it).path, intentFilter.getDataPath(it).type))
            }
            assertThat(actualValues).containsOnlyElementsOf(expectedDataPaths.map { tuple(it?.first, it?.second) })
        }
    }

    @Test
    fun testAwaitBroadcastReceiverRegisterBroadcastReceiverWithExpectedDataSchemes() {
        val intentFilter = interceptIntentFilter()
        if (expectedDataSchemes == null) {
            assertThat(intentFilter.countDataSchemes()).isZero()
        } else {
            val actualValues = ArrayList<String>()
            repeat(intentFilter.countDataSchemes()) { actualValues.add(intentFilter.getDataScheme(it)) }
            assertThat(actualValues).containsOnlyElementsOf(expectedDataSchemes)
        }
    }

    @Test
    fun testAwaitBroadcastReceiverRegisterBroadcastReceiverWithExpectedDataTypes() {
        val intentFilter = interceptIntentFilter()
        if (expectedDataTypes == null) {
            assertThat(intentFilter.countDataTypes()).isZero()
        } else {
            val actualValues = ArrayList<String>()
            repeat(intentFilter.countDataTypes()) { actualValues.add(intentFilter.getDataType(it)) }
            assertThat(actualValues).containsOnlyElementsOf(expectedDataTypes)
        }
    }

    private fun interceptIntentFilter(): IntentFilter {
        verify(application)
            .registerReceiver(ArgumentMatchers.any(BroadcastReceiver::class.java), intentFilterCaptor.capture())
        return intentFilterCaptor.value
    }
}
