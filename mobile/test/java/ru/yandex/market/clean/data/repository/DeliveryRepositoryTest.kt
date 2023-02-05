package ru.yandex.market.clean.data.repository

import com.annimon.stream.Optional
import com.annimon.stream.OptionalLong
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.dto.frontApiRegionDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.regionsDeliveryFapiDtoTestInstance
import ru.yandex.market.clean.data.fapi.source.region.RegionFapiClient
import ru.yandex.market.clean.data.fapi.source.regions.delivery.RegionsDeliveryFapiClient
import ru.yandex.market.clean.data.fapi.source.regions.suggestion.RegionSuggestionsFapiClient
import ru.yandex.market.clean.data.mapper.DeliveryAvailabilityMapper
import ru.yandex.market.clean.data.mapper.DeliveryLocalityMapper
import ru.yandex.market.clean.data.mapper.DeliveryLocalitySuggestionMapper
import ru.yandex.market.clean.data.store.DeliveryLocalDataStore
import ru.yandex.market.clean.data.store.DeliveryLocalityCache
import ru.yandex.market.clean.domain.model.deliveryLocalitySuggestionTestInstance
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.data.region.RegionDtoV2
import ru.yandex.market.data.regions.SelectedRegionRepository
import ru.yandex.market.domain.models.region.DeliveryLocality
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.safe.Safe
import ru.yandex.market.utils.Observables
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.utils.asOptional
import ru.yandex.market.utils.toOptional
import ru.yandex.market.optional.Optional as MarketOptional

class DeliveryRepositoryTest {

    private val deliveryLocalityMapper = mock<DeliveryLocalityMapper> {
        on { map(any<RegionDtoV2>()) } doReturn deliveryLocalityTestInstance().asExceptional()
    }

    private val preferencesDataStore = mock<PreferencesDataStore> {
        on { saveSelectedDeliveryLocality(any()) } doReturn Completable.complete()
    }

    private val workerScheduler = WorkerScheduler(Schedulers.trampoline())
    private val networkingScheduler = NetworkingScheduler(Schedulers.trampoline())

    private val regionToAvailableRegionMapper = mock<DeliveryAvailabilityMapper>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()

    private val deliveryLocalityCache = mock<DeliveryLocalityCache> {
        on { containsId(any()) } doReturn false
        on { get(any()) } doReturn null
    }

    private val commonLocalities = mock<CommonDeliveryLocalityRepository> {
        on { localities } doReturn listOf(deliveryLocalityTestInstance())
    }

    private val suggestionsMapper = mock<DeliveryLocalitySuggestionMapper> {
        on { map(any()) } doReturn deliveryLocalitySuggestionTestInstance().asExceptional()
    }

    private val regionsRepository = mock<RegionsRepository>()

    private val selectedRegionRepository = mock<SelectedRegionRepository> {
        on { setSelectedRegionIdCompletable(any()) } doReturn Completable.complete()
    }

    private val deliveryLocalDataStore = mock<DeliveryLocalDataStore>()
    private val regionsDeliveryFapiClient = mock<RegionsDeliveryFapiClient> {
        on { getRegionDelivery(any()) } doReturn Single.just(regionsDeliveryFapiDtoTestInstance())
    }
    private val regionSuggestionsFapiClient = mock<RegionSuggestionsFapiClient>()
    private val regionFapiClient = mock<RegionFapiClient>()

    private val repository = DeliveryRepository(
        deliveryLocalityMapper,
        preferencesDataStore,
        workerScheduler,
        networkingScheduler,
        regionToAvailableRegionMapper,
        analyticsService,
        deliveryLocalityCache,
        commonLocalities,
        suggestionsMapper,
        regionsRepository,
        selectedRegionRepository,
        deliveryLocalDataStore,
        lazy { regionsDeliveryFapiClient },
        lazy { regionSuggestionsFapiClient },
        lazy { regionFapiClient },
    )

    @Test
    fun `Current delivery locality stream tries to get locality from preferences first`() {
        val first = deliveryLocalityTestInstance()
        whenever(preferencesDataStore.selectedDeliveryLocalityStream)
            .thenReturn(Observables.stream(first.asOptional()))

        repository.currentOrDefaultDeliveryLocalityStream
            .test()
            .assertNoErrors()
            .assertValue(first)
            .assertNotComplete()
    }

    @Test
    fun `If failed to get current locality from preferences, try to get region id and find it in cache`() {
        whenever(preferencesDataStore.selectedDeliveryLocalityStream).thenReturn(Observable.error(RuntimeException()))
        val regionId = 42L
        whenever(selectedRegionRepository.getSelectedRegionIdStream()).thenReturn(
            Observables.stream(OptionalLong.of(regionId))
        )
        whenever(deliveryLocalityCache.containsId(regionId)).thenReturn(true)
        val locality = deliveryLocalityTestInstance()
        whenever(deliveryLocalityCache.get(regionId)).thenReturn(locality)

        repository.currentOrDefaultDeliveryLocalityStream
            .test()
            .assertNoErrors()
            .assertValue(locality)
            .assertNotComplete()
    }

    @Test
    fun `If failed to get current locality and region id from preferences, return default locality`() {
        whenever(preferencesDataStore.selectedDeliveryLocalityStream).thenReturn(Observable.error(RuntimeException()))
        whenever(selectedRegionRepository.getSelectedRegionIdStream()).thenReturn(
            Observables.stream(OptionalLong.empty())
        )
        whenever(regionFapiClient.resolveRegionWithCountry(any())).thenReturn(Single.error(Exception()))
        val locality = deliveryLocalityTestInstance()
        whenever(commonLocalities.moscow).thenReturn(locality)

        repository.currentOrDefaultDeliveryLocalityStream
            .test()
            .assertNoErrors()
            .assertValue(locality)
            .assertNotComplete()
    }

    @Test
    fun `Switch to region id stream when current locality is empty in preferences`() {
        whenever(preferencesDataStore.selectedDeliveryLocalityStream).thenReturn(Observables.stream(Optional.empty()))
        whenever(regionFapiClient.resolveRegionWithCountry(any())).thenReturn(
            Single.just(MarketOptional.of(frontApiRegionDtoTestInstance()) to frontApiRegionDtoTestInstance())
        )
        whenever(deliveryLocalityMapper.mapRegionCountry(any())).thenReturn(
            Safe.value(deliveryLocalityTestInstance())
        )
        whenever(selectedRegionRepository.getSelectedRegionIdStream())
            .thenReturn(Observables.stream(1L, 2L).map { OptionalLong.of(it) })

        repository.currentOrDefaultDeliveryLocalityStream
            .test()
            .assertNoErrors()
            .assertValueCount(2)
            .assertNotComplete()
    }

    @Test
    fun `Switch back from region id stream to current locality`() {
        val localityStream = BehaviorSubject.createDefault<Optional<DeliveryLocality>>(Optional.empty())
        whenever(preferencesDataStore.selectedDeliveryLocalityStream).thenReturn(localityStream)
        whenever(regionFapiClient.resolveRegionWithCountry(any())).thenReturn(
            Single.just(MarketOptional.of(frontApiRegionDtoTestInstance()) to frontApiRegionDtoTestInstance())
        )
        whenever(selectedRegionRepository.getSelectedRegionIdStream())
            .thenReturn(Observables.stream(1L).map { OptionalLong.of(it) })

        val mappedLocality = deliveryLocalityTestInstance()
        whenever(deliveryLocalityMapper.mapRegionCountry(any())).thenReturn(Safe.value(mappedLocality))

        val observer = repository.currentOrDefaultDeliveryLocalityStream.test()
        val preferencesLocality = deliveryLocalityTestInstance(regionId = 1090)
        localityStream.onNext(preferencesLocality.asOptional())

        observer.assertNoErrors()
            .assertValues(mappedLocality, preferencesLocality)
            .assertNotComplete()
    }

    @Test
    fun `Save current locality to preferences after switch to id`() {
        whenever(preferencesDataStore.selectedDeliveryLocalityStream).thenReturn(Observables.stream(Optional.empty()))
        whenever(selectedRegionRepository.getSelectedRegionIdStream()).thenReturn(Observables.stream(1L.toOptional()))
        whenever(regionFapiClient.resolveRegionWithCountry(any())).thenReturn(
            Single.just(MarketOptional.of(frontApiRegionDtoTestInstance()) to frontApiRegionDtoTestInstance())
        )
        val locality = deliveryLocalityTestInstance()
        whenever(deliveryLocalityMapper.mapRegionCountry(any())).thenReturn(Safe.value(locality))

        repository.currentOrDefaultDeliveryLocalityStream.test()
            .assertNoErrors()
            .assertNotComplete()

        verify(selectedRegionRepository).setSelectedRegionIdCompletable(locality.regionId)
        verify(preferencesDataStore).saveSelectedDeliveryLocality(locality)
    }
}