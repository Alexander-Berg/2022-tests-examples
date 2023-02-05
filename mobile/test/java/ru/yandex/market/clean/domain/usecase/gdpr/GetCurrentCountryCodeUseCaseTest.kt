package ru.yandex.market.clean.domain.usecase.gdpr

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.clean.data.repository.RegionsRepository
import ru.yandex.market.clean.data.repository.LocationRepository
import ru.yandex.market.domain.models.region.CountryCode
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.optional.Optional

class GetCurrentCountryCodeUseCaseTest {

    private val regionsRepository = mock<RegionsRepository>()

    private val locationRepository = mock<LocationRepository>()

    private val useCase = GetCurrentCountryCodeUseCase(regionsRepository, locationRepository)

    @Test
    fun `Gets current country code from regions repository`() {
        whenever(regionsRepository.currentCountryCode)
            .thenReturn(Single.just(Optional.of(CountryCode.RU)))

        useCase.getCurrentCountryCode()
            .test()
            .assertResult(CountryCode.RU)
    }

    @Test
    fun `Gets current country code from location repository when regions repository returns error`() {
        whenever(regionsRepository.currentCountryCode).thenReturn(Single.error(RuntimeException()))
        val latitude = 10.0
        val longitude = 20.0
        val coordinates = GeoCoordinates(latitude, longitude)
        whenever(locationRepository.approximateLocation).thenReturn(Single.just(coordinates))
        whenever(regionsRepository.getCountryCode(latitude, longitude))
            .thenReturn(Single.just(CountryCode.RU))

        useCase.getCurrentCountryCode()
            .test()
            .assertResult(CountryCode.RU)
    }

    @Test
    fun `Gets current country code from location repository when regions repository returns empty`() {
        whenever(regionsRepository.currentCountryCode).thenReturn(Single.just(Optional.empty()))
        val latitude = 10.0
        val longitude = 20.0
        val coordinates = GeoCoordinates(latitude, longitude)
        whenever(locationRepository.approximateLocation).thenReturn(Single.just(coordinates))
        whenever(regionsRepository.getCountryCode(latitude, longitude))
            .thenReturn(Single.just(CountryCode.RU))

        useCase.getCurrentCountryCode()
            .test()
            .assertResult(CountryCode.RU)
    }
}