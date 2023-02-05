package ru.yandex.market.clean.domain.usecase.cart

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.yandex.market.clean.domain.model.SuccessOrderResult
import ru.yandex.market.clean.domain.model.cart.cartItemIdTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.successOrderResultTestInstance
import ru.yandex.market.mockResult

class CleanupCartAfterOrderUseCaseTest {

    private val getCartItemsUseCase = mock<GetCartItemsUseCase>()
    private val deleteCartItemUseCase = mock<DeleteCartItemUseCase>()

    private val useCase = CleanupCartAfterOrderUseCase(getCartItemsUseCase, deleteCartItemUseCase)

    @Before
    fun setUp() {

        getCartItemsUseCase.getCartItems().mockResult(Single.just(listOf(cartItemTestInstance())))
        deleteCartItemUseCase.deleteItems(listOf(any()), anyOrNull(), anyOrNull()).mockResult(Completable.complete())
    }

    @Test
    fun `check with empty order items`() {

        useCase.cleanupCartAfterOrder(SuccessOrderResult(emptyList(), false))
            .test()
            .assertNoErrors()
            .assertComplete()

        verifyZeroInteractions(getCartItemsUseCase)
        verifyZeroInteractions(deleteCartItemUseCase)
    }

    @Test
    fun `check with empty order item ids`() {

        val cartItem = cartItemTestInstance().copy(persistentOfferId = "test_id")
        getCartItemsUseCase.getCartItems().mockResult(Single.just(listOf(cartItem)))

        useCase.cleanupCartAfterOrder(successOrderResultTestInstance())
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(getCartItemsUseCase).getCartItems()
        verifyZeroInteractions(deleteCartItemUseCase)
    }

    @Test
    fun `check full cleanup`() {

        val cartItemIds = listOf(cartItemIdTestInstance())

        deleteCartItemUseCase.deleteItems(cartItemIds, null, null).mockResult(Completable.complete())

        useCase.cleanupCartAfterOrder(successOrderResultTestInstance())
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(getCartItemsUseCase).getCartItems()
        verify(deleteCartItemUseCase).deleteItems(cartItemIds, null, null)
    }
}