package ru.yandex.market.clean.domain.usecase

import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.data.mapper.DeliveryLocalityResolver
import ru.yandex.market.clean.domain.model.CartAffectingData
import ru.yandex.market.clean.domain.model.LoyaltyStatus
import ru.yandex.market.clean.domain.model.SecretSale
import ru.yandex.market.clean.domain.model.secretSaleTestInstance
import ru.yandex.market.clean.domain.usecase.bnpl.IsBnplEnabledUseCase
import ru.yandex.market.clean.domain.usecase.hyperlocal.ObserveHyperlocalRequestDataUseCase
import ru.yandex.market.clean.domain.usecase.secretsale.GetSecretSaleInfoUseCase
import ru.yandex.market.domain.auth.model.UserAccount
import ru.yandex.market.domain.auth.model.accountIdTestInstance
import ru.yandex.market.domain.auth.model.userAccountTestInstance
import ru.yandex.market.domain.auth.usecase.GetUserAccountStreamUseCase
import ru.yandex.market.domain.fintech.usecase.tinkoff.IsTinkoffInstallmentsEnabledUseCase
import ru.yandex.market.domain.hyperlocal.model.HyperlocalRequestData
import ru.yandex.market.domain.models.region.DeliveryLocality
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.domain.user.model.userProfileTestInstance
import ru.yandex.market.domain.user.usecase.ObserveCurrentUserProfileUseCase
import ru.yandex.market.optional.Optional
import ru.yandex.market.test.extensions.asStream
import ru.yandex.market.utils.Observables

class CartAffectingDataUseCaseTest {

    private val regionUseCase = mock<GetCurrentRegionUseCase>()
    private val secretSaleUseCase = mock<GetSecretSaleInfoUseCase>()
    private val isBnplEnabledUseCase = mock<IsBnplEnabledUseCase>()
    private val getUserAccountStreamUseCase = mock<GetUserAccountStreamUseCase>()
    private val getLoyaltyStatusUseCase = mock<GetLoyaltyStatusUseCase>()
    private val observeCurrentUserProfileUseCase = mock<ObserveCurrentUserProfileUseCase>()
    private val deliveryLocalityResolver = DeliveryLocalityResolver()
    private val observeHyperlocalRequestDataUseCase = mock<ObserveHyperlocalRequestDataUseCase>()
    private val isTinkoffInstallmentsEnabledUseCase = mock<IsTinkoffInstallmentsEnabledUseCase>()
    private val useCase = CartAffectingDataUseCase(
        regionUseCase,
        secretSaleUseCase,
        getUserAccountStreamUseCase,
        getLoyaltyStatusUseCase,
        observeCurrentUserProfileUseCase,
        deliveryLocalityResolver,
        observeHyperlocalRequestDataUseCase,
        isBnplEnabledUseCase,
        isTinkoffInstallmentsEnabledUseCase,
    )

    @Test
    fun `Cart affecting data stream combines latest delivery region and secret sale`() {
        val regionStream = PublishSubject.create<DeliveryLocality>()
        val secretSaleStream = PublishSubject.create<Optional<SecretSale>>()
        val accountStream = PublishSubject.create<Optional<UserAccount>>()
        whenever(regionUseCase.getCurrentDeliveryLocalityStream()) doReturn regionStream
        whenever(secretSaleUseCase.getSecretSaleStream()) doReturn secretSaleStream
        whenever(getUserAccountStreamUseCase.getUserAccountStream()) doReturn accountStream
        whenever(getLoyaltyStatusUseCase.getLoyaltyStatusStream()) doReturn Observables.stream(LoyaltyStatus.AVAILABLE)
        whenever(isBnplEnabledUseCase.execute()) doReturn Single.fromCallable { true }
        whenever(observeCurrentUserProfileUseCase.execute()) doReturn Observables.stream(
            Optional.of(userProfileTestInstance(hasYandexPlus = false))
        )
        whenever(observeHyperlocalRequestDataUseCase.execute()) doReturn Observables.stream(HyperlocalRequestData.EMPTY)
        whenever(isTinkoffInstallmentsEnabledUseCase.execute()) doReturn Single.fromCallable { true }

        val observer = useCase.getCartAffectingDataStream()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        regionStream.onNext(deliveryLocalityTestInstance(regionId = 1L))
        secretSaleStream.onNext(Optional.ofNullable(secretSaleTestInstance(id = "s1")))
        accountStream.onNext(Optional.ofNullable(userAccountTestInstance(id = accountIdTestInstance(id = 1L))))
        regionStream.onNext(deliveryLocalityTestInstance(regionId = 2L))
        secretSaleStream.onNext(Optional.ofNullable(secretSaleTestInstance(id = "s2")))
        accountStream.onNext(Optional.ofNullable(userAccountTestInstance(id = accountIdTestInstance(id = 2L))))

        observer.assertValues(
            CartAffectingData(
                deliveryLocality = deliveryLocalityTestInstance(regionId = 1L),
                personalPromoId = "s1",
                accountId = accountIdTestInstance(id = 1L),
                loyaltyStatus = LoyaltyStatus.AVAILABLE,
                hasYandexPlus = false,
                isBnplEnabled = true,
                isTinkoffInstallmentsEnabled = true,
            ),
            CartAffectingData(
                deliveryLocality = deliveryLocalityTestInstance(regionId = 2L),
                personalPromoId = "s1",
                accountId = accountIdTestInstance(id = 1L),
                loyaltyStatus = LoyaltyStatus.AVAILABLE,
                hasYandexPlus = false,
                isBnplEnabled = true,
                isTinkoffInstallmentsEnabled = true,
            ),
            CartAffectingData(
                deliveryLocality = deliveryLocalityTestInstance(regionId = 2L),
                personalPromoId = "s2",
                accountId = accountIdTestInstance(id = 1L),
                loyaltyStatus = LoyaltyStatus.AVAILABLE,
                hasYandexPlus = false,
                isBnplEnabled = true,
                isTinkoffInstallmentsEnabled = true,
            ),
            CartAffectingData(
                deliveryLocality = deliveryLocalityTestInstance(regionId = 2L),
                personalPromoId = "s2",
                accountId = accountIdTestInstance(id = 2L),
                loyaltyStatus = LoyaltyStatus.AVAILABLE,
                hasYandexPlus = false,
                isBnplEnabled = true,
                isTinkoffInstallmentsEnabled = true,
            )
        )
    }

    @Test
    fun `Cart affecting data stream distinct values`() {
        val regionStream = PublishSubject.create<DeliveryLocality>()
        val secretSaleStream = PublishSubject.create<Optional<SecretSale>>()
        val accountStream = PublishSubject.create<Optional<UserAccount>>()
        whenever(regionUseCase.getCurrentDeliveryLocalityStream()) doReturn regionStream
        whenever(secretSaleUseCase.getSecretSaleStream()) doReturn secretSaleStream
        whenever(getUserAccountStreamUseCase.getUserAccountStream()) doReturn accountStream
        whenever(isBnplEnabledUseCase.execute()) doReturn Single.fromCallable { true }
        whenever(getLoyaltyStatusUseCase.getLoyaltyStatusStream()) doReturn Observables.stream(LoyaltyStatus.AVAILABLE)
        whenever(observeCurrentUserProfileUseCase.execute()) doReturn Observables.stream(
            Optional.of(userProfileTestInstance(hasYandexPlus = false))
        )
        whenever(observeHyperlocalRequestDataUseCase.execute()) doReturn Observables.stream(HyperlocalRequestData.EMPTY)
        whenever(isTinkoffInstallmentsEnabledUseCase.execute()) doReturn Single.fromCallable { true }

        val observer = useCase.getCartAffectingDataStream()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        regionStream.onNext(deliveryLocalityTestInstance(regionId = 1L))
        secretSaleStream.onNext(Optional.ofNullable(secretSaleTestInstance(id = "s1")))
        accountStream.onNext(Optional.ofNullable(userAccountTestInstance(id = accountIdTestInstance(id = 1L))))
        regionStream.onNext(deliveryLocalityTestInstance(regionId = 1L))
        secretSaleStream.onNext(Optional.ofNullable(secretSaleTestInstance(id = "s1")))
        accountStream.onNext(Optional.ofNullable(userAccountTestInstance(id = accountIdTestInstance(id = 1L))))

        observer.assertValue(
            CartAffectingData(
                deliveryLocality = deliveryLocalityTestInstance(regionId = 1L),
                personalPromoId = "s1",
                accountId = accountIdTestInstance(id = 1L),
                loyaltyStatus = LoyaltyStatus.AVAILABLE,
                hasYandexPlus = false,
                isBnplEnabled = true,
                isTinkoffInstallmentsEnabled = true,
            )
        )
    }

    @Test
    fun `Maps values from streams into CartAffectingData object`() {
        val deliveryLocality = deliveryLocalityTestInstance(regionId = 1L)
        val secretSale = secretSaleTestInstance()
        val accountId = accountIdTestInstance()
        whenever(regionUseCase.getCurrentDeliveryLocalityStream()) doReturn Observables.stream(deliveryLocality)
        whenever(isBnplEnabledUseCase.execute()) doReturn Single.fromCallable { true }
        whenever(secretSaleUseCase.getSecretSaleStream()) doReturn Observables.stream(Optional.ofNullable(secretSale))
        whenever(getUserAccountStreamUseCase.getUserAccountStream()) doReturn
                Optional.ofNullable(userAccountTestInstance(id = accountId)).asStream()
        whenever(getLoyaltyStatusUseCase.getLoyaltyStatusStream()) doReturn Observables.stream(LoyaltyStatus.AVAILABLE)
        whenever(observeCurrentUserProfileUseCase.execute()) doReturn Observables.stream(
            Optional.of(userProfileTestInstance(hasYandexPlus = true))
        )
        whenever(observeHyperlocalRequestDataUseCase.execute()) doReturn Observables.stream(HyperlocalRequestData.EMPTY)
        whenever(isTinkoffInstallmentsEnabledUseCase.execute()) doReturn Single.fromCallable { true }

        useCase.getCartAffectingDataStream()
            .test()
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(
                CartAffectingData(
                    deliveryLocality = deliveryLocality,
                    personalPromoId = secretSale.id,
                    accountId = accountId,
                    loyaltyStatus = LoyaltyStatus.AVAILABLE,
                    hasYandexPlus = true,
                    isBnplEnabled = true,
                    isTinkoffInstallmentsEnabled = true,
                )
            )
    }
}
