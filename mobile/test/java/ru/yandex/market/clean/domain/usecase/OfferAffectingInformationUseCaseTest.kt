package ru.yandex.market.clean.domain.usecase

import io.reactivex.Observable
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.offer.OfferAffectingInformationRepository
import ru.yandex.market.clean.domain.model.OfferAffectingInformation
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.clean.domain.model.secretSaleTestInstance
import ru.yandex.market.clean.domain.usecase.health.facades.OfferAffectingInformationHealthFacade
import ru.yandex.market.clean.domain.usecase.secretsale.GetSecretSaleInfoUseCase
import ru.yandex.market.domain.auth.model.authTokenTestInstance
import ru.yandex.market.domain.auth.usecase.GetAuthTokenStreamUseCase
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.domain.user.usecase.ObserveCurrentUserProfileUseCase
import ru.yandex.market.optional.Optional
import ru.yandex.market.utils.asOptional

class OfferAffectingInformationUseCaseTest {

    private val getAuthTokenStreamUseCase = mock<GetAuthTokenStreamUseCase>()
    private val getCurrentRegionUseCase = mock<GetCurrentRegionUseCase>()
    private val getSecretSaleInfoUseCase = mock<GetSecretSaleInfoUseCase>()
    private val observeCurrentUserProfileUseCase = mock<ObserveCurrentUserProfileUseCase>()
    private val offerAffectingInformationRepository = mock<OfferAffectingInformationRepository>()
    private val offerAffectingInformationHealthFacade = mock<OfferAffectingInformationHealthFacade>()

    private val useCase = OfferAffectingInformationUseCase(
        getAuthTokenStreamUseCase,
        getCurrentRegionUseCase,
        getSecretSaleInfoUseCase,
        observeCurrentUserProfileUseCase,
        offerAffectingInformationRepository,
        offerAffectingInformationHealthFacade,
    )

    @Test
    fun `on getOfferAffectingInformationStream try to get cached stream`() {
        val cachedStream = Observable.empty<OfferAffectingInformation>()
        whenever(offerAffectingInformationRepository.getStream()).thenReturn(cachedStream)

        useCase.getOfferAffectingInformationStream()
            .test()
            .assertNoErrors()

        verify(getAuthTokenStreamUseCase, never()).getAuthTokenStream()
        verify(getCurrentRegionUseCase, never()).getCurrentDeliveryLocalityStream()
        verify(getSecretSaleInfoUseCase, never()).getSecretSaleStream()
        verify(observeCurrentUserProfileUseCase, never()).execute()
    }

    @Test
    fun `on getOfferAffectingInformationStream return new stream if no cache and put it in repository`() {
        whenever(offerAffectingInformationRepository.getStream()).thenReturn(null)

        whenever(getAuthTokenStreamUseCase.getAuthTokenStream())
            .thenReturn(Observable.just(authTokenTestInstance().asOptional()))
        whenever(getCurrentRegionUseCase.getCurrentDeliveryLocalityStream())
            .thenReturn(Observable.just(deliveryLocalityTestInstance()))
        whenever(getSecretSaleInfoUseCase.getSecretSaleStream())
            .thenReturn(Observable.just(Optional.ofNullable(secretSaleTestInstance())))
        whenever(observeCurrentUserProfileUseCase.execute())
            .thenReturn(Observable.just(Optional.ofNullable(userProfileTestInstance())))

        useCase.getOfferAffectingInformationStream()
            .test()
            .assertNoErrors()
            .assertValueCount(1)

        verify(offerAffectingInformationRepository, times(1)).putStream(any())
    }

    @Test
    fun `on getOfferAffectingInformationStream return new stream with replayingShare`() {
        whenever(offerAffectingInformationRepository.getStream()).thenReturn(null)

        whenever(getAuthTokenStreamUseCase.getAuthTokenStream())
            .thenReturn(Observable.just(authTokenTestInstance().asOptional()))
        whenever(getCurrentRegionUseCase.getCurrentDeliveryLocalityStream())
            .thenReturn(Observable.just(deliveryLocalityTestInstance()))
        whenever(getSecretSaleInfoUseCase.getSecretSaleStream())
            .thenReturn(Observable.just(Optional.ofNullable(secretSaleTestInstance())))
        whenever(observeCurrentUserProfileUseCase.execute())
            .thenReturn(Observable.just(Optional.ofNullable(userProfileTestInstance())))

        val offerAffectingInformationStream = useCase.getOfferAffectingInformationStream()

        val subscription = offerAffectingInformationStream
            .test()
            .assertNoErrors()
            .assertValueCount(1)

        subscription.dispose()

        offerAffectingInformationStream
            .test()
            .assertNoErrors()
            .assertValueCount(1)
    }
}