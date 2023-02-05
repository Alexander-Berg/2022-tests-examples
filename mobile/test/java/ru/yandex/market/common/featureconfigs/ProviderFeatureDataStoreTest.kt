package ru.yandex.market.common.featureconfigs

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.featureconfigs.bootstrapper.FeatureConfigDataBootstrapper
import ru.yandex.market.common.featureconfigs.datastore.FapiRemoteConfigDataStore
import ru.yandex.market.common.featureconfigs.datastore.ProductionRemoteConfigDataStore
import ru.yandex.market.common.featureconfigs.provider.ConfigDataStoreProvider
import ru.yandex.market.common.featureconfigs.repository.FapiRemoteConfigRepository
import ru.yandex.market.common.schedulers.WorkerScheduler

class ProviderFeatureDataStoreTest {

    private val workerScheduler = WorkerScheduler(Schedulers.trampoline())
    private val productionRemoteConfigDataStore = mock<ProductionRemoteConfigDataStore>()
    private val fapiRemoteConfigDataStore = mock<FapiRemoteConfigDataStore>()
    private val fapiRemoteConfigRepository = mock<FapiRemoteConfigRepository>()
    private val dataStoreProvider = ConfigDataStoreProvider(
        productionRemoteConfigDataStore,
        lazy { fapiRemoteConfigDataStore }
    )
    private val bootstrapper = FeatureConfigDataBootstrapper(
        dataStoreProvider,
        workerScheduler,
    )

    @Before
    fun setUp() {
        whenever(productionRemoteConfigDataStore.setup()).thenReturn(Completable.complete())
        whenever(fapiRemoteConfigDataStore.setup()).thenReturn(Completable.complete())
        whenever(fapiRemoteConfigRepository.fetchConfig()).thenReturn(Completable.complete())
    }

    @Test
    fun `Provide firebase datastore if firebase success`() {
        whenever(productionRemoteConfigDataStore.fetch()).thenReturn(Completable.complete())
        bootstrapper.bootstrap()

        val datastore = dataStoreProvider.provideDataStore()
        Assertions.assertThat(datastore::class.java).isEqualTo(ProductionRemoteConfigDataStore::class.java)
    }

    @Test
    fun `Provide fapi datastore if firebase error`() {
        whenever(fapiRemoteConfigDataStore.fetch()).thenReturn(Completable.complete())
        whenever(productionRemoteConfigDataStore.fetch()).thenReturn(Completable.error(Throwable("Firebase error")))
        bootstrapper.bootstrap()

        val datastore = dataStoreProvider.provideDataStore()
        Assertions.assertThat(datastore::class.java).isEqualTo(FapiRemoteConfigDataStore::class.java)
    }

}