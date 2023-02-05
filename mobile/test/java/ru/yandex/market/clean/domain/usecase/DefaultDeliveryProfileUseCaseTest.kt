package ru.yandex.market.clean.domain.usecase

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.DeliveryRepository
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.data.passport.Address
import ru.yandex.market.data.passport.Profile
import ru.yandex.market.domain.models.region.Country
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.domain.user.repository.UserProfileRepository
import ru.yandex.market.optional.Optional
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.util.toAddress
import ru.yandex.market.utils.Observables

@RunWith(MockitoJUnitRunner::class)
class DefaultDeliveryProfileUseCaseTest {

    private val deliveryRepository = mock<DeliveryRepository>()
    private val userProfileRepository = mock<UserProfileRepository>()
    private val useCase = DefaultDeliveryProfileUseCase(deliveryRepository, userProfileRepository)

    @Test
    fun `When user profile is present, use its data to fill recipient fields in profile`() {
        val deliveryLocality = deliveryLocalityTestInstance()
        whenever(deliveryRepository.currentOrDefaultDeliveryLocality)
            .thenReturn(deliveryLocality.asSingle())
        val phone = "+7 495 123-45-67"
        val email = "example@yandex.ru"
        val firstName = "Станислав"
        val lastName = "Максимов"
        val userProfile = userProfileTestInstance(
            phones = listOf(phone),
            email = email,
            firstName = firstName,
            lastName = lastName
        )
        whenever(userProfileRepository.getCachedCurrentUserProfileStream())
            .thenReturn(Observables.stream(Optional.ofNullable(userProfile)))

        val expectedProfile = Profile.builder()
            .address(deliveryLocality.toAddress())
            .fullName("$firstName $lastName")
            .email(email)
            .phone(phone)
            .build()
        useCase.getDefaultDeliveryProfile()
            .test()
            .assertResult(expectedProfile)
    }

    @Test
    fun `Don't fill recipient fields when user profile is absent`() {
        val deliveryLocality = deliveryLocalityTestInstance()
        whenever(deliveryRepository.currentOrDefaultDeliveryLocality)
            .thenReturn(deliveryLocality.asSingle())
        whenever(userProfileRepository.getCachedCurrentUserProfileStream())
            .thenReturn(Observables.stream(Optional.empty()))

        val expectedProfile = Profile.builder()
            .address(deliveryLocality.toAddress())
            .build()
        useCase.getDefaultDeliveryProfile()
            .test()
            .assertResult(expectedProfile)
    }

    @Test
    fun `Fill address fields from default delivery locality`() {
        val regionId = 213L
        val cityName = "Москва"
        val countryName = "Россия"
        val deliveryLocality = deliveryLocalityTestInstance(
            regionId = regionId,
            name = cityName,
            country = Country(name = countryName, code = "")
        )
        whenever(deliveryRepository.currentOrDefaultDeliveryLocality)
            .thenReturn(deliveryLocality.asSingle())
        whenever(userProfileRepository.getCachedCurrentUserProfileStream())
            .thenReturn(Observables.stream(Optional.empty()))

        val expectedAddress = Address.builder()
            .regionId(regionId)
            .city(cityName)
            .country(countryName)
            .build()
        val expectedProfile = Profile.builder()
            .address(expectedAddress)
            .build()
        useCase.getDefaultDeliveryProfile()
            .test()
            .assertResult(expectedProfile)
    }
}