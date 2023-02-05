package ru.yandex.market.clean.data.repository

import com.annimon.stream.OptionalLong
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.data.mapper.CountryCodeMapper
import ru.yandex.market.clean.data.fapi.FrontApiDataSource
import ru.yandex.market.clean.data.fapi.dto.frontApiRegionDtoTestInstance
import ru.yandex.market.clean.data.fapi.source.region.RegionFapiClient
import ru.yandex.market.clean.data.mapper.userpreset.address.StreetSuggestsMapper
import ru.yandex.market.clean.data.model.dto.address.SuggestStreetDto
import ru.yandex.market.clean.data.store.AutoDetectedRegionDataStore
import ru.yandex.market.clean.domain.model.AutoDetectedRegion
import ru.yandex.market.clean.domain.model.addresses.StreetSuggest
import ru.yandex.market.clean.domain.model.addresses.StreetSuggests
import ru.yandex.market.clean.domain.model.deliveryAvailabilityTestInstance
import ru.yandex.market.common.featureconfigs.managers.FapiUserRegionToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.data.region.RegionDtoV2
import ru.yandex.market.data.regions.SelectedRegionRepository
import ru.yandex.market.domain.models.region.CountryCode
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.models.region.RegionType
import ru.yandex.market.net.http.HttpClient
import ru.yandex.market.test.extensions.asOptional
import ru.yandex.market.optional.Optional as MarketOptional

class RegionsRepositoryTest {

    private val networkingScheduler = NetworkingScheduler(Schedulers.trampoline())

    private val countryCodeMapper = mock<CountryCodeMapper>()

    private val frontApiDataSource = mock<FrontApiDataSource>()

    private val suggestsMapper = mock<StreetSuggestsMapper>()

    private val amazingHttpClient = mock<HttpClient>()

    private val autoDetectedRegionDataStore = mock<AutoDetectedRegionDataStore>()

    private val featureConfigsProvider = mock<FeatureConfigsProvider>()

    private val selectedRegionsRepository = mock<SelectedRegionRepository>()

    private val regionFapiClient = mock<RegionFapiClient>()

    private val fapiUserRegionToggleManager = mock<FapiUserRegionToggleManager>()

    private val featureToggle = mock<FeatureToggle>()

    private val repository = RegionsRepository(
        selectedRegionsRepository,
        networkingScheduler,
        countryCodeMapper,
        suggestsMapper,
        Lazy { frontApiDataSource },
        amazingHttpClient,
        autoDetectedRegionDataStore,
        featureConfigsProvider,
        Lazy { regionFapiClient },
    )

    @Test
    fun `Maps geo region id to country code`() {
        val countryId = 42L
        val country = RegionDtoV2.testBuilder()
            .id(countryId)
            .type(RegionType.COUNTRY)
            .build()
        val region = RegionDtoV2.testBuilder()
            .country(country)
            .type(RegionType.CITY)
            .build()
        whenever(regionFapiClient.resolveRegionById(any()))
            .thenReturn(Single.just(MarketOptional.of(frontApiRegionDtoTestInstance())))
        whenever(countryCodeMapper.mapByGeoId(any())).thenReturn(CountryCode.RU)

        repository.getCountryCode(1)
            .test()
            .assertResult(CountryCode.RU)
        verify(countryCodeMapper).mapByGeoId(countryId)
    }

    @Test
    fun `Returns unknown country code when failed to get county id from region`() {
        whenever(regionFapiClient.resolveRegionById(any()))
            .thenReturn(Single.just(MarketOptional.of(frontApiRegionDtoTestInstance())))

        repository.getCountryCode(1)
            .test()
            .assertResult(CountryCode.UNKNOWN)
    }

    @Test
    fun `Get current country code from current region when present`() {
        val regionId = 42L
        whenever(selectedRegionsRepository.getSelectedRegionId()).thenReturn(regionId.asOptional)

        whenever(regionFapiClient.resolveRegionById(any()))
            .thenReturn(Single.just(MarketOptional.of(frontApiRegionDtoTestInstance())))
        val countryCode = CountryCode.RU
        whenever(countryCodeMapper.mapByGeoId(regionId)).thenReturn(countryCode)

        repository.currentCountryCode
            .test()
            .assertResult(MarketOptional.of(countryCode))
    }

    @Test
    fun `Return empty country code when current region is not present`() {
        whenever(selectedRegionsRepository.getSelectedRegionId()).thenReturn(OptionalLong.empty())

        repository.currentCountryCode
            .test()
            .assertResult(MarketOptional.empty())
    }

    @Test
    fun `Maps gps coordinate to country code`() {
        val countryId = 42L
        val country = RegionDtoV2.testBuilder().type(RegionType.COUNTRY).id(countryId).build()
        val region = RegionDtoV2.testBuilder().country(country).build()
        whenever(regionFapiClient.getRegionByLocality(any())).thenReturn(Single.just(region))
        whenever(countryCodeMapper.mapByGeoId(any())).thenReturn(CountryCode.RU)

        repository.getCountryCode(1.0, 1.0)
            .test()
            .assertResult(CountryCode.RU)
        verify(countryCodeMapper).mapByGeoId(countryId)
    }

    @Test
    fun `Returns unknown country code when failed to get county code from gps coordinate`() {
        val region = RegionDtoV2.testBuilder().build()
        whenever(regionFapiClient.getRegionByLocality(any())).thenReturn(Single.just(region))

        repository.getCountryCode(1.0, 1.0)
            .test()
            .assertResult(CountryCode.UNKNOWN)
    }

    @Test
    fun `Get street suggestions for region`() {
        val fullName = "fullName"
        val shortName = "shortName"
        val suggest = StreetSuggests(mutableListOf(StreetSuggest(fullName, shortName)))
        val suggestDto = SuggestStreetDto(
            mutableListOf(SuggestStreetDto.Result(SuggestStreetDto.Title(fullName), shortName))
        )
        whenever(amazingHttpClient.findAddresses(any(), any())).thenReturn(suggestDto)
        whenever(suggestsMapper.map(suggestDto)).thenReturn(suggest)

        repository.getAddressSuggestions(1, "")
            .test()
            .assertResult(suggest)
    }

    @Test
    fun `Get auto detected region`() {
        val autoDetectedRegion = AutoDetectedRegion(
            currentRegionId = CommonDeliveryLocalityRepository.REGION_ID_MOSCOW,
            isAutoDetected = true
        )

        whenever(autoDetectedRegionDataStore.autoDetectedRegionId).thenReturn(null)
        whenever(frontApiDataSource.getCurrentUserRegion()).thenReturn(null)
        whenever(featureConfigsProvider.fapiUserRegionToggleManager).thenReturn(fapiUserRegionToggleManager)
        whenever(fapiUserRegionToggleManager.get()).thenReturn(featureToggle)
        whenever(featureToggle.isEnabled).thenReturn(false)

        repository.getAutoDetectedRegion()
            .test()
            .assertResult(autoDetectedRegion)
    }

    @Test
    fun `Get saved auto detected region by coordinates`() {
        val deliveryAvailability = deliveryAvailabilityTestInstance()
        whenever(autoDetectedRegionDataStore.autoDetectedRegionByCoordinates).thenReturn(deliveryAvailability)

        repository.getAutoDetectedRegionByCoordinates()
            .test()
            .assertResult(MarketOptional.of(deliveryAvailability))
    }

    @Test
    fun `Get region by coordinates`() {
        val region = RegionDtoV2.testBuilder().build()
        whenever(regionFapiClient.getRegionByLocality(any())).thenReturn(Single.just(region))

        repository.getRegionByCoordinates(GeoCoordinates(1.0, 1.0))
            .test()
            .assertResult(region)
    }
}