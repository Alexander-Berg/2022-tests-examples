package ru.yandex.market.ui.view.mvp.cartcounterbutton

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.junit.Test
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.test.extensions.asStream
import ru.yandex.market.clean.data.repository.cart.CartItemRepositoryLocalCart
import ru.yandex.market.clean.domain.model.cartAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.usecase.CartAffectingDataUseCase

class GetCartItemUseCaseTest {

    private val cartItemRepository = mock<CartItemRepositoryLocalCart>()
    private val cartAffectingDataUseCase = mock<CartAffectingDataUseCase>()
    private val useCase = GetCartItemUseCase(cartItemRepository, cartAffectingDataUseCase)

    @Test
    fun `Prioritize bundle items when search item by skuId`() {
        whenever(cartAffectingDataUseCase.getCartAffectingDataStream()) doReturn
                cartAffectingDataTestInstance().asStream()
        val skuId = "id"
        val offerId = "persistentId"
        val productOffer = productOfferTestInstance(offer = offerTestInstance(persistentId = offerId))
        val cartItemWithBundle = cartItemTestInstance(bundleId = "bundle", skuId = skuId, offer = productOffer)
        whenever(cartItemRepository.getCartItemsBy(any(), any(), any(), any())) doReturn
                listOf(
                    cartItemTestInstance(bundleId = "", skuId = skuId),
                    cartItemWithBundle
                ).asSingle()

        useCase.getCartItem(skuId, offerId, true)
            .test()
            .assertNoErrors()
            .assertResult(cartItemWithBundle)
    }
}