package ru.auto.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.Entity
import ru.auto.data.model.dictionary.BODY_TYPE
import ru.auto.data.model.dictionary.Dictionary
import ru.auto.data.model.network.scala.catalog.dictionary.NWDictionary
import ru.auto.data.model.network.scala.catalog.dictionary.NWDictionaryInfo
import ru.auto.data.model.network.scala.offer.NWEntity
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.DictionaryResponse
import ru.auto.data.prefs.MemoryPrefsDelegate
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.data.storage.assets.AssetStorage
import rx.Single
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AllureRunner::class) class DictionaryRepositoryTest {

    private val memoryPrefsDelegate = MemoryPrefsDelegate()
    private val memoryReactivePrefsDelegate = MemoryReactivePrefsDelegate(memoryPrefsDelegate)

    private val api: ScalaApi = mock()

    private val jsonRepo = JsonItemsRepo(
            DictionaryRepository.DICTIONARY_KEY, memoryReactivePrefsDelegate, TYPE
    )
    private val assetStorage: AssetStorage = mock()
    private val repository = DictionaryRepository(api, jsonRepo, assetStorage)

    @Before
    fun setUp() {
        whenever(api.getDictionary(any(), any(), any())).thenReturn(Single.just(DICTIONARY_RESPONSE))
        whenever(assetStorage.readJsonAsset<DictionaryResponse>(any(), any())).thenReturn(DictionaryResponse(LOCAL_NWDICTIONARY))
    }

    @Test fun `get dictionary from asset`() {
        val dictionary = repository.getDictionariesForCategory(CATEGORY).toBlocking().value()[BODY_TYPE]!!
        LOCAL_DICTIONARY.assertEqualsTo(dictionary)
    }

    private fun Dictionary.assertEqualsTo(actual: Dictionary) {
        assertEquals(version, actual.version)
        values?.entries?.forEachIndexed { index, entry ->
             val actualEntry = actual.values?.entries?.toList()?.get(index)?.value
            assertEquals(entry.value.id, actualEntry?.id)
            assertEquals(entry.value.label, actualEntry?.label)
        }
    }

    @Test fun `save dictionary to prefs`() {
        val dictionary = repository.getDictionariesForCategory(CATEGORY).toBlocking().value()[BODY_TYPE]
        val json = memoryPrefsDelegate.getString(DictionaryRepository.DICTIONARY_KEY)
        val list = Gson().fromJson<Any>(json, TYPE.type) as ArrayList<NWDictionaryInfo>
        assertEquals(LOCAL_DICTIONARY.version, list[0].dictionary.version)
    }

    @Test fun `updating dictionary from network`() {
        val dictionary = repository.getDictionariesForCategory(CATEGORY).toBlocking().value()[BODY_TYPE]!!
        repository.updateDictionary(CATEGORY, BODY_TYPE).test().assertCompleted()
        val updated = repository.getDictionariesForCategory(CATEGORY).toBlocking().value()[BODY_TYPE]!!
        assertTrue { updated.version!! > dictionary.version!! }
        assertEquals("2", updated.values!!["1"]!!.label)
    }

    private companion object {
        const val CATEGORY = CAR
        val TYPE = object : TypeToken<ArrayList<NWDictionaryInfo>>() {}

        val OLD_VALUES = mapOf("1" to Entity("1", "1"))
        val OLD_NETWORK_VALUES = listOf(NWEntity("1", "1"))
        val VALUES = listOf(NWEntity("1", "2"))

        val LOCAL_NWDICTIONARY = NWDictionary("1.0", OLD_NETWORK_VALUES)
        val LOCAL_DICTIONARY = Dictionary("1.0", OLD_VALUES)
        val NETWORK_DICTIONARY = NWDictionary("1.1", VALUES)

        val DICTIONARY_RESPONSE = DictionaryResponse(NETWORK_DICTIONARY)
    }
}
