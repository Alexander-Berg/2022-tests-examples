package ru.auto.data.stub

import com.google.gson.reflect.TypeToken
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.data.offer.Entity
import ru.auto.data.model.network.scala.catalog.dictionary.NWDictionary
import ru.auto.data.model.network.scala.catalog.dictionary.NWDictionaryInfo
import ru.auto.data.model.network.scala.offer.NWEntity
import ru.auto.data.model.network.scala.offer.converter.EntityConverter
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.DictionaryResponse
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.data.repository.DictionaryRepository
import ru.auto.data.repository.IDictionaryRepository
import ru.auto.data.repository.JsonItemsRepo
import ru.auto.data.storage.assets.AssetStorage
import rx.Single

/**
 * @author dumchev on 2/7/19.
 */
class DictionaryRepositoryStub(
    old_values: Map<String, Entity> = mapOf("1" to Entity("1", "1")),
    values: List<NWEntity> = listOf(NWEntity("1", "2")),
    localDictionary: NWDictionary = NWDictionary("1.0", old_values.map { EntityConverter.toNetwork(it.value) }),
    networkDictionary: NWDictionary = NWDictionary("1.1", values)
) : IDictionaryRepository by buildDelegateRepository(localDictionary, networkDictionary) {

    companion object {
        private val typeToken = object : TypeToken<ArrayList<NWDictionaryInfo>>() {}
        private val jsonRepo = JsonItemsRepo(DictionaryRepository.DICTIONARY_KEY, MemoryReactivePrefsDelegate(), typeToken)
        private val api: ScalaApi = mock()
        private val assetStorage: AssetStorage = mock()

        private fun buildDelegateRepository(
            localDictionary: NWDictionary,
            networkDictionary: NWDictionary
        ): DictionaryRepository {
            val dictionaryResponse = DictionaryResponse(networkDictionary)

            whenever(api.getDictionary(any(), any(), any())).thenReturn(Single.just(dictionaryResponse))
            whenever(assetStorage.readJsonAsset<DictionaryResponse>(any(), any()))
                .thenReturn(DictionaryResponse(localDictionary))

            return DictionaryRepository(api, jsonRepo, assetStorage)
        }
    }
}
