package ru.yandex.market.clean.domain.usecase.cart

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import io.reactivex.processors.PublishProcessor
import org.junit.Test
import org.reactivestreams.Subscriber
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.test.extensions.asStream
import ru.yandex.market.utils.Observables
import ru.yandex.market.checkout.domain.model.ErrorsPack
import ru.yandex.market.clean.data.ActualizedCartItemMapper
import ru.yandex.market.clean.data.actualizedCartItemMapper_ResultTestInstance
import ru.yandex.market.clean.data.repository.cart.CartItemRepositoryLocalCart
import ru.yandex.market.clean.domain.model.CartSynchronizationResult
import ru.yandex.market.clean.domain.model.cartAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.orderItemMappingTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.model.rewriteCartSpecTestInstance
import ru.yandex.market.clean.domain.usecase.CartAffectingDataUseCase

class SyncCartAfterActualizationUseCaseTest {

    private val cartItemRepository = mock<CartItemRepositoryLocalCart> {
        on {
            rewriteCart(
                specification = any(),
                cartAffectingData = any(),
                correspondingOffers = any(),
                cartChangeSubscriber = anyOrNull()
            )
        } doReturn listOf(cartItemTestInstance()).asSingle()
    }
    private val cartAffectingDataUseCase = mock<CartAffectingDataUseCase> {
        on { getCartAffectingDataStream() } doReturn Observables.stream(cartAffectingDataTestInstance())
    }
    private val cartItemMapper = mock<ActualizedCartItemMapper> {
        on { map(any(), any()) } doReturn actualizedCartItemMapper_ResultTestInstance(
            missingCartItems = emptyList(),
            missingOrderItems = emptyList()
        )
    }
    private val prepareSpecsUseCase = mock<PrepareCartRewriteSpecsUseCase> {
        on { prepareRewriteSpecs(any(), any(), any()) } doReturn rewriteCartSpecTestInstance().asSingle()
    }
    private val useCase = SyncCartAfterActualizationUseCase(
        cartItemRepository, cartItemMapper, cartAffectingDataUseCase, prepareSpecsUseCase
    )

    @Test(expected = IllegalArgumentException::class)
    fun `Throws exception if mappings are empty`() {
        useCase.synchronizeCartAfterActualization(
            mappings = emptyList(),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
    }

    @Test
    fun `Rewrite cart using cart affecting data and rewrite specs`() {
        val cartAffectingData = cartAffectingDataTestInstance()
        whenever(cartAffectingDataUseCase.getCartAffectingDataStream()) doReturn cartAffectingData.asStream()
        val specs = rewriteCartSpecTestInstance()
        whenever(prepareSpecsUseCase.prepareRewriteSpecs(any(), any(), any())) doReturn specs.asSingle()

        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance()),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemRepository).rewriteCart(
            specification = same(specs),
            cartAffectingData = same(cartAffectingData),
            correspondingOffers = any(),
            cartChangeSubscriber = anyOrNull()
        )
    }

    @Test
    fun `Passes input parameters to prepare specs use case`() {
        val mappings = listOf(orderItemMappingTestInstance())
        val missingCartItems = listOf(cartItemTestInstance())
        val errors = ErrorsPack.empty()
        useCase.synchronizeCartAfterActualization(
            mappings = mappings,
            missingCartItems = missingCartItems,
            errors = errors,
            cartChangeSubscriber = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(prepareSpecsUseCase).prepareRewriteSpecs(
            mappings = same(mappings),
            missingCartItems = same(missingCartItems),
            errors = same(errors)
        )
    }

    @Test
    fun `Passes input parameters to cart repository`() {
        val cartChangeSubscriber = PublishProcessor.create<Boolean>()
        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance()),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = cartChangeSubscriber
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemRepository).rewriteCart(
            specification = any(),
            cartAffectingData = any(),
            correspondingOffers = any(),
            cartChangeSubscriber = same(cartChangeSubscriber)
        )
    }

    @Test
    fun `Extracts corresponding offers from source cart items and passes them to cart repository`() {
        val firstOffer = productOfferTestInstance(offer = offerTestInstance(persistentId = "1"))
        val secondOffer = productOfferTestInstance(offer = offerTestInstance(persistentId = "2"))
        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance(sourceCartItem = cartItemTestInstance(offer = firstOffer))),
            missingCartItems = listOf(cartItemTestInstance(offer = secondOffer)),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemRepository).rewriteCart(
            specification = any(),
            cartAffectingData = any(),
            correspondingOffers = eq(listOf(firstOffer, secondOffer)),
            cartChangeSubscriber = anyOrNull()
        )
    }

    @Test
    fun `Distinct corresponding offers by persistent offerId`() {
        val firstOffer = productOfferTestInstance(offer = offerTestInstance(persistentId = "1"))
        val secondOffer = productOfferTestInstance(offer = offerTestInstance(persistentId = "1"))
        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance(sourceCartItem = cartItemTestInstance(offer = firstOffer))),
            missingCartItems = listOf(cartItemTestInstance(offer = secondOffer)),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemRepository).rewriteCart(
            specification = any(),
            cartAffectingData = any(),
            correspondingOffers = eq(listOf(firstOffer)),
            cartChangeSubscriber = anyOrNull()
        )
    }

    @Test
    fun `Do not return error when actualized result have missing cart items`() {
        whenever(cartItemMapper.map(any(), any())) doReturn actualizedCartItemMapper_ResultTestInstance(
            missingCartItems = listOf(cartItemTestInstance()),
            missingOrderItems = emptyList()
        )

        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance()),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
            .test()
            .assertNoErrors()
    }

    @Test
    fun `Returns error when actualized result have missing order items`() {
        whenever(cartItemMapper.map(any(), any())) doReturn actualizedCartItemMapper_ResultTestInstance(
            missingCartItems = emptyList(),
            missingOrderItems = listOf(orderItemTestInstance())
        )

        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance()),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
            .test()
            .assertError(IllegalStateException::class.java)
    }

    @Test
    fun `Returns cart synchronization result combining cart repository result with mapper result`() {
        val actualizedCartItems = actualizedCartItemMapper_ResultTestInstance(
            missingCartItems = emptyList(),
            missingOrderItems = emptyList()
        )
        whenever(cartItemMapper.map(any(), any())) doReturn actualizedCartItems
        whenever(
            cartItemRepository.rewriteCart(
                specification = any(),
                cartAffectingData = any(),
                correspondingOffers = any(),
                cartChangeSubscriber = anyOrNull()
            )
        ) doReturn listOf(cartItemTestInstance()).asSingle()

        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance()),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = null
        )
            .test()
            .assertNoErrors()
            .assertValue(CartSynchronizationResult(actualizedCartItems.matchedItems))
    }

    @Test
    fun `Calls nothing on cart change subscriber`() {
        val cartChangeSubscriber = mock<Subscriber<Boolean>>()
        useCase.synchronizeCartAfterActualization(
            mappings = listOf(orderItemMappingTestInstance()),
            missingCartItems = listOf(cartItemTestInstance()),
            errors = ErrorsPack.empty(),
            cartChangeSubscriber = cartChangeSubscriber
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verifyZeroInteractions(cartChangeSubscriber)
    }
}