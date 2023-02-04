package ru.auto.ara.search

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.search.GeoRepository.Companion.GEO_ASSET_PATH
import ru.auto.ara.search.GeoRepository.Companion.GEO_DICTIONARY_VERSION_KEY
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.model.network.scala.geo.NWGeoListing
import ru.auto.data.model.network.scala.geo.NWGeoResponse
import ru.auto.data.model.network.scala.geo.NWRegion
import ru.auto.data.model.network.scala.geo.converter.RegionConverter
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.prefs.IPrefsDelegate
import ru.auto.data.prefs.MemoryPrefsDelegate
import ru.auto.data.repository.ItemsRepository
import ru.auto.data.repository.MemoryItemsRepo
import ru.auto.data.storage.assets.AssetStorage
import ru.auto.testextension.testWithSubscriber
import rx.Single.error
import rx.Single.just
import rx.observers.TestSubscriber
import java.io.IOException

/**
 * @author dumchev on 03.04.2018.
 */
@RunWith(AllureRunner::class)
 @Suppress("RemoveRedundantBackticks")
class GeoRepositoryTest {

    private val prefsDelegate: IPrefsDelegate = MemoryPrefsDelegate()
    private val assetStorage: AssetStorage = mock()
    private val api: ScalaApi = mock()
    private val radiusStorage: ItemsRepository<Int> = MemoryItemsRepo()
    private val chosenGeoStorage: ItemsRepository<SuggestGeoItem> = MemoryItemsRepo()
    private val allGeoStorage: ItemsRepository<SuggestGeoItem> = MemoryItemsRepo()

    private lateinit var geoRepo: GeoRepository

    @Before
    fun setUp() {
        whenever(assetStorage.readJsonAsset(GEO_ASSET_PATH, NWGeoResponse::class.java))
                .thenReturn(createGeoResponse(items = oldGeoItems, version = OLD_VERSION))

        whenever(api.getRegions())
                .thenReturn(just(createGeoResponse(items = newGeoItems, version = NEW_VERSION)))

        geoRepo = GeoRepository(api, assetStorage, prefsDelegate, radiusStorage,
                chosenGeoStorage = chosenGeoStorage, allGeoCacheStorage = allGeoStorage)

        prefsDelegate.saveString(GEO_DICTIONARY_VERSION_KEY, OLD_VERSION)
        allGeoStorage.save(emptyList()).toBlocking()
    }

    @Test
    fun `updateCache() should updated version and allGeoStorage`() {
        geoRepo.updateCache(NEW_VERSION).await()
        check(prefsDelegate.getString(GEO_DICTIONARY_VERSION_KEY) == NEW_VERSION) {
            "version doesn't updated, though we had old version in prefs and got new version from api"
        }
        testWithSubscriber(allGeoStorage.get()) { testSubscriber ->
            testSubscriber.assertValue(newGeoItems.toSuggestGeoItems())
        }
    }


    @Test
    fun `if error occurs during updateCache(), do not update version`() {
        val allGeoStorage: ItemsRepository<SuggestGeoItem> = mock()
        geoRepo = GeoRepository(api, assetStorage, prefsDelegate, radiusStorage,
                chosenGeoStorage = chosenGeoStorage, allGeoCacheStorage = allGeoStorage)

        val checkHasOldVersionAfterInvocation: () -> Unit = {
            val subscriber = TestSubscriber<Boolean>()
            geoRepo.updateCache(NEW_VERSION).toSingle { false }.subscribe(subscriber)
            subscriber.assertError(IOException::class.java)
            check(prefsDelegate.getString(GEO_DICTIONARY_VERSION_KEY) != NEW_VERSION) {
                "version shouldn't be updated, as we had some errors during updateCache()"
            }
        }

        whenever(allGeoStorage.save(any())).thenReturn(error(IOException("can't save :(")))
        checkHasOldVersionAfterInvocation()
    }

    @Test
    fun `getAllRegions() go to assets only when we have no cache`() {
        allGeoStorage.save(oldGeoItems.toSuggestGeoItems()).toBlocking()
        testWithSubscriber(geoRepo.getAllRegions()) {
            verify(assetStorage, times(0)).readJsonAsset(GEO_ASSET_PATH, NWGeoResponse::class.java)
        }

        allGeoStorage.save(emptyList()).toBlocking()
        testWithSubscriber(geoRepo.getAllRegions()) {
            verify(assetStorage, times(1)).readJsonAsset(GEO_ASSET_PATH, NWGeoResponse::class.java)
        }
    }

    @Test
    fun `saveSelectedRegions() getSelectedRegions() getFirstSelected()`() {
        val newItems = newGeoItems.toSuggestGeoItems()
        geoRepo.saveSelectedRegions(newItems).await()
        testWithSubscriber(geoRepo.getSelectedRegions()) { it.assertValue(newItems) }
        testWithSubscriber(geoRepo.getFirstSelected()) { it.assertValue(newItems.first()) }
    }

    @Test
    fun getGeoSuggest() {
        whenever(api.geoSuggest(false, null, null, null))
                .thenReturn(just(createNWGeoListing(newGeoItems, NEW_VERSION)))
        testWithSubscriber(geoRepo.getGeoSuggest(isOnlyCities = false)) {
            it.assertValue(newGeoItems.toSuggestGeoItems())
        }
    }

    companion object {
        private const val OLD_VERSION: String = "7"
        private const val NEW_VERSION: String = "9"

        private val oldGeoItems: List<NWRegion> = listOf(NWRegion(id = "44", name = "Ереван"))
        private val newGeoItems: List<NWRegion> = listOf(NWRegion(id = "4", name = "London"))


        private fun List<NWRegion>.toSuggestGeoItems(): List<SuggestGeoItem> =
                this.map { RegionConverter.convert(it) }

        private fun createGeoResponse(items: List<NWRegion>, version: String): NWGeoResponse =
                NWGeoResponse(listing = createNWGeoListing(items, version))

        private fun createNWGeoListing(items: List<NWRegion>, version: String): NWGeoListing =
                NWGeoListing(version = version, regions = items)

    }
}
