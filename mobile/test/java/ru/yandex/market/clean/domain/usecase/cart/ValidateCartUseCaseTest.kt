package ru.yandex.market.clean.domain.usecase.cart

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.DeviceInfoRepository
import ru.yandex.market.clean.data.repository.PromoCodeRepository
import ru.yandex.market.clean.data.repository.cart.CartItemRepositoryLocalCart
import ru.yandex.market.clean.domain.model.cart.cartValidationResultTestInstance
import ru.yandex.market.clean.domain.model.cartAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.deviceInfoTestInstance
import ru.yandex.market.clean.domain.usecase.CartAffectingDataUseCase
import ru.yandex.market.clean.domain.usecase.promocode.GetLoyaltyPromoCodeUseCase
import ru.yandex.market.mockResult
import ru.yandex.market.optional.Optional

class ValidateCartUseCaseTest {

    private val cartItemRepository = mock<CartItemRepositoryLocalCart>()
    private val getCartValidationResultUseCase = mock<GetCartValidationResultUseCase>()
    private val promoCodeRepository = mock<PromoCodeRepository>()
    private val deviceInfoRepository = mock<DeviceInfoRepository>()
    private val cartAffectingDataUseCase = mock<CartAffectingDataUseCase>()
    private val cartPartialPurchaseUseCase = mock<CartPartialPurchaseUseCase>()
    private val getLoyaltyPromoCodeUseCase = mock<GetLoyaltyPromoCodeUseCase> {
        on { execute() } doReturn Observable.just(Unit)
    }

    private val useCase = ValidateCartUseCase(

        cartItemRepository = cartItemRepository,
        getCartValidationResultUseCase = getCartValidationResultUseCase,
        promoCodeRepository = promoCodeRepository,
        deviceInfoRepository = deviceInfoRepository,
        cartAffectingDataUseCase = cartAffectingDataUseCase,
        getLoyaltyPromoCodeUseCase = getLoyaltyPromoCodeUseCase,
        cartPartialPurchaseUseCase = cartPartialPurchaseUseCase,
    )

    @Test
    fun `check on cart affecting data empty`() {

        cartAffectingDataUseCase.getCartAffectingDataStream().mockResult(Observable.empty())

        useCase.validateCart(emptyList(), PublishProcessor.create(), 1)
            .test()
            .assertError(NoSuchElementException::class.java)
    }

    @Test
    fun `check full validation flow`() {

        val cartAffectingData = cartAffectingDataTestInstance()
        val cartValidationResult = cartValidationResultTestInstance()

        cartItemRepository.getCartItemsStream(cartAffectingData)
            .mockResult(Observable.just(Optional.of(listOf(cartAffectingData))))
        getCartValidationResultUseCase.execute(any(), any(), any(), any(), any(), any(), any(), any(), any())
            .mockResult(Single.just(cartValidationResult))
        promoCodeRepository.getPromoCodeStream().mockResult(Observable.just(""))
        deviceInfoRepository.getDeviceInfo().mockResult(Single.just(deviceInfoTestInstance()))
        cartAffectingDataUseCase.getCartAffectingDataStream().mockResult(Observable.just(cartAffectingData))

        useCase.validateCart(emptyList(), PublishProcessor.create(), 1)
            .test()
            .assertNoErrors()
            .assertResult(cartValidationResult)
    }
}
