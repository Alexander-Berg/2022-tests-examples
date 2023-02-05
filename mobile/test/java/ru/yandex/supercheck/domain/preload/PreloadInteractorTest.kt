package ru.yandex.supercheck.domain.preload

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.domain.config.ConfigInteractor
import ru.yandex.supercheck.domain.error.AvailabilityError
import ru.yandex.supercheck.domain.preload.TestData.SCAN_AND_GO_SHOPS
import ru.yandex.supercheck.domain.preload.TestData.SERVER_RESPONSE_ERROR
import ru.yandex.supercheck.domain.scanandgo.shoplist.ScanAndGoShopInteractor
import ru.yandex.supercheck.domain.shops.ShopsInteractor
import ru.yandex.supercheck.model.domain.category.CategoryDomain
import ru.yandex.supercheck.model.domain.config.LocationAwareConfig
import ru.yandex.supercheck.model.domain.scanandgo.ScanAndGoShopsResult
import ru.yandex.supercheck.model.domain.shops.ShopsWithCurrentAddressAndState

@RunWith(MockitoJUnitRunner::class)
class PreloadInteractorTest {

    private val configInteractor = mock<ConfigInteractor>()
    private val shopsInteractor = mock<ShopsInteractor>()
    private val scanAndGoShopInteractor = mock<ScanAndGoShopInteractor>()

    private val preloadInteractor by lazy {
        PreloadInteractor(
            configInteractor,
            shopsInteractor,
            scanAndGoShopInteractor
        )
    }

    @Before
    fun setUp() {
        mockConfig(isScanAndGoAvailable = true, isProductCatalogAvailable = true)
        mockShops(Single.just(TestData.SHOPS))
        mockScanAndGoOutlets(Observable.just(SCAN_AND_GO_SHOPS))
    }

    @Test
    fun allFeaturesAreAvailable() {
        preload().assertComplete()
    }

    @Test
    fun allFeaturesAreAvailableButShopsLoadWithError() {
        mockShops(Single.error(SERVER_RESPONSE_ERROR))

        preload().assertComplete()
    }

    @Test
    fun allFeaturesAreAvailableButScanAndGoLoadWithError() {
        mockScanAndGoOutlets(Observable.error(SERVER_RESPONSE_ERROR))

        preload().assertComplete()
    }

    @Test
    fun allFeaturesAreAvailableButAllLoadWithError() {
        mockShops(Single.error(SERVER_RESPONSE_ERROR))
        mockScanAndGoOutlets(Observable.error(SERVER_RESPONSE_ERROR))

        preload().assertError(Throwable::class.java)
    }

    @Test
    fun onlyScanAndGoAvailable() {
        mockConfig(isScanAndGoAvailable = true, isProductCatalogAvailable = false)
        preload().assertComplete()
    }

    @Test
    fun onlyScanAndGoAvailableButLoadWithError() {
        mockConfig(isScanAndGoAvailable = true, isProductCatalogAvailable = false)
        mockScanAndGoOutlets(Observable.error(SERVER_RESPONSE_ERROR))
        preload().assertError(SERVER_RESPONSE_ERROR)
    }

    @Test
    fun onlyProductCatalogAvailable() {
        mockConfig(isScanAndGoAvailable = false, isProductCatalogAvailable = true)
        preload().assertComplete()
    }

    @Test
    fun onlyProductCatalogAvailableButLoadWithError() {
        mockConfig(isScanAndGoAvailable = false, isProductCatalogAvailable = true)
        mockShops(Single.error(SERVER_RESPONSE_ERROR))
        preload().assertError(SERVER_RESPONSE_ERROR)
    }

    @Test
    fun noneOfFeaturesAvailable() {
        mockConfig(isScanAndGoAvailable = false, isProductCatalogAvailable = false)
        preload().assertError(AvailabilityError.AllFunctionalScenariosUnavailable)
    }

    private fun mockConfig(isScanAndGoAvailable: Boolean, isProductCatalogAvailable: Boolean) {
        whenever(configInteractor.getConfigByUserLocation()).thenReturn(
            Single.just(
                LocationAwareConfig(
                    isScanAndGoAvailable = isScanAndGoAvailable,
                    isProductCatalogAvailable = isProductCatalogAvailable
                )
            )
        )
    }

    private fun mockShops(shops: Single<Pair<CategoryDomain, ShopsWithCurrentAddressAndState>>) {
        whenever(shopsInteractor.loadShops(false)).thenReturn(shops)
    }

    private fun mockScanAndGoOutlets(outlets: Observable<ScanAndGoShopsResult>) {
        whenever(scanAndGoShopInteractor.getShopsList()).thenReturn(outlets)
    }

    private fun preload(): TestObserver<Void> {
        return preloadInteractor.preloadData()
            .test()
    }

}