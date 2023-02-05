package ru.yandex.market.cart.cases

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.common.randomgenerator.RandomGenerator
import ru.yandex.market.clean.data.repository.ShopInShopMetrikaParamsRepository
import ru.yandex.market.clean.data.repository.cart.CartItemRepositoryLocalCart
import ru.yandex.market.clean.domain.model.ShopInShopMetrikaParams
import ru.yandex.market.clean.domain.model.cartAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.giftOfferTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.usecase.CartAffectingDataUseCase
import ru.yandex.market.db.addCartItemsResultTestInstance
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.rub
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.utils.Observables

class AddOfferToCartUseCaseTest {

    private val cartItemRepository = mock<CartItemRepositoryLocalCart>()
    private val randomGenerator = mock<RandomGenerator>()
    private val cartAffectingDataUseCase = mock<CartAffectingDataUseCase> {
        on { getCartAffectingDataStream() } doReturn Observables.stream(cartAffectingDataTestInstance())
    }
    private val shopInShopMetricaParamsRepository = mock<ShopInShopMetrikaParamsRepository> {
        on {
            getShopInShopMetrics(
                anyOrNull(),
                anyOrNull()
            )
        } doReturn Single.fromCallable { Optional.ofNullable<ShopInShopMetrikaParams>(null) }
    }

    private val useCase = AddOfferToCartUseCase(
        cartItemRepository,
        randomGenerator,
        cartAffectingDataUseCase,
        shopInShopMetricaParamsRepository,
    )

    @Test
    fun `Add product offer to cart calls repository method`() {
        whenever(
            cartItemRepository.addOfferToCart(
                offer = any(),
                price = any(),
                count = any(),
                giftOffers = any(),
                bundleId = any(),
                cartAffectingData = any(),
                alternativeOfferReason = anyOrNull(),
                selectedServiceId = anyOrNull(),
                isDirectShopInShop = any(),
                shopInShopMetrikaParams = anyOrNull()
            )
        ) doReturn addCartItemsResultTestInstance().asSingle()

        useCase.addToCart(
            offer = productOfferTestInstance(),
            price = moneyTestInstance(),
            count = 1,
            promotionalOffers = emptyList(),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopPageId = null,
        )
            .test()
            .assertNoErrors()
            .assertValueCount(1)
    }

    @Test
    fun `Add product offer to cart with incorrect count throws exception`() {
        useCase.addToCart(
            offer = productOfferTestInstance(),
            price = moneyTestInstance(),
            count = 0,
            promotionalOffers = emptyList(),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopPageId = null,
        )
            .test()
            .assertError(IllegalArgumentException::class.java)
    }

    @Test
    fun `Add product offer to cart generates bundle id when there are gift offers`() {
        whenever(
            cartItemRepository.addOfferToCart(
                offer = any(),
                price = any(),
                count = any(),
                giftOffers = any(),
                bundleId = any(),
                cartAffectingData = any(),
                alternativeOfferReason = anyOrNull(),
                selectedServiceId = anyOrNull(),
                isDirectShopInShop = any(),
                shopInShopMetrikaParams = anyOrNull(),
            )
        ) doReturn addCartItemsResultTestInstance().asSingle()
        whenever(randomGenerator.getRandomUuid()) doReturn "not an empty string"

        useCase.addToCart(
            offer = productOfferTestInstance(),
            price = moneyTestInstance(),
            count = 1,
            promotionalOffers = listOf(giftOfferTestInstance()),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopPageId = null,
        )
            .test()
            .assertNoErrors()

        verify(cartItemRepository).addOfferToCart(
            offer = any(),
            price = any(),
            count = any(),
            giftOffers = argThat { isNotEmpty() },
            bundleId = any(),
            cartAffectingData = any(),
            alternativeOfferReason = anyOrNull(),
            selectedServiceId = anyOrNull(),
            isDirectShopInShop = any(),
            shopInShopMetrikaParams = anyOrNull(),
        )
    }

    @Test
    fun `Add product offer to cart do not generates bundle id when there are no gift offers`() {
        whenever(
            cartItemRepository.addOfferToCart(
                offer = any(),
                price = any(),
                count = any(),
                giftOffers = any(),
                bundleId = any(),
                cartAffectingData = any(),
                alternativeOfferReason = anyOrNull(),
                selectedServiceId = anyOrNull(),
                isDirectShopInShop = any(),
                shopInShopMetrikaParams = anyOrNull(),
            )
        ) doReturn addCartItemsResultTestInstance().asSingle()

        useCase.addToCart(
            offer = productOfferTestInstance(),
            price = moneyTestInstance(),
            count = 1,
            promotionalOffers = emptyList(),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopPageId = null,
        )
            .test()
            .assertNoErrors()

        verify(cartItemRepository).addOfferToCart(
            offer = any(),
            price = any(),
            count = any(),
            giftOffers = argThat { isEmpty() },
            bundleId = any(),
            cartAffectingData = any(),
            alternativeOfferReason = anyOrNull(),
            selectedServiceId = anyOrNull(),
            isDirectShopInShop = any(),
            shopInShopMetrikaParams = anyOrNull(),
        )
    }

    @Test
    fun `Add product offer uses passed price for bundles`() {
        whenever(
            cartItemRepository.addOfferToCart(
                offer = any(),
                price = any(),
                count = any(),
                giftOffers = any(),
                bundleId = any(),
                cartAffectingData = any(),
                alternativeOfferReason = anyOrNull(),
                selectedServiceId = anyOrNull(),
                isDirectShopInShop = any(),
                shopInShopMetrikaParams = anyOrNull(),
            )
        ) doReturn addCartItemsResultTestInstance().asSingle()

        useCase.addToCart(
            offer = productOfferTestInstance(),
            price = 5_000.rub,
            count = 1,
            promotionalOffers = listOf(giftOfferTestInstance(primaryOfferPrice = 3_000.rub)),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopPageId = null
        )
            .test()
            .assertNoErrors()

        verify(cartItemRepository).addOfferToCart(
            offer = any(),
            price = argThat { equals(5_000.rub) },
            count = any(),
            giftOffers = any(),
            bundleId = any(),
            cartAffectingData = any(),
            alternativeOfferReason = anyOrNull(),
            selectedServiceId = anyOrNull(),
            isDirectShopInShop = any(),
            shopInShopMetrikaParams = anyOrNull()
        )
    }

    @Test
    fun `Add product offer uses passed price when adding none-bundle offers`() {
        whenever(
            cartItemRepository.addOfferToCart(
                offer = any(),
                price = any(),
                count = any(),
                giftOffers = any(),
                bundleId = any(),
                cartAffectingData = any(),
                alternativeOfferReason = anyOrNull(),
                selectedServiceId = anyOrNull(),
                isDirectShopInShop = any(),
                shopInShopMetrikaParams = anyOrNull()
            )
        ) doReturn addCartItemsResultTestInstance().asSingle()

        useCase.addToCart(
            offer = productOfferTestInstance(),
            price = 5_000.rub,
            count = 1,
            promotionalOffers = emptyList(),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopPageId = null
        )
            .test()
            .assertNoErrors()

        verify(cartItemRepository).addOfferToCart(
            offer = any(),
            price = argThat { equals(5_000.rub) },
            count = any(),
            giftOffers = any(),
            bundleId = any(),
            cartAffectingData = any(),
            alternativeOfferReason = anyOrNull(),
            selectedServiceId = anyOrNull(),
            isDirectShopInShop = any(),
            shopInShopMetrikaParams = anyOrNull()
        )
    }
}
