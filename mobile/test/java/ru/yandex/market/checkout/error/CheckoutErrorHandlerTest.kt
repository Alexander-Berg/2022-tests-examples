package ru.yandex.market.checkout.error

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.checkout.domain.model.ErrorsPack
import ru.yandex.market.checkout.domain.model.ItemErrorsPack
import ru.yandex.market.checkout.domain.model.ShopErrorsPack
import ru.yandex.market.checkout.domain.model.SmartCoinErrorsPack
import ru.yandex.market.checkout.domain.model.error.CheckoutError
import ru.yandex.market.checkout.domain.model.error.CountChangeError
import ru.yandex.market.checkout.domain.model.error.DimensionsTotalError
import ru.yandex.market.checkout.domain.model.error.NoStockError
import ru.yandex.market.checkout.domain.model.error.PriceChangeError
import ru.yandex.market.checkout.domain.model.error.ShopError
import ru.yandex.market.checkout.domain.model.error.TotalAmountError
import ru.yandex.market.checkout.domain.model.error.UnusedCoinError
import ru.yandex.market.checkout.domain.model.warning.CheckoutWarning
import ru.yandex.market.clean.domain.model.SmartCoinError

class CheckoutErrorHandlerTest {

    private val checkoutErrorHandler = CheckoutErrorHandler()
    private val callback = mock<CheckoutErrorCallback>()

    @Test
    fun `Not show any error when pack is empty`() {
        checkoutErrorHandler.handleErrors(errorsPack = buildErrorsPack(), callback = callback)

        verify(callback).onPrepareErrorHandler()
    }

    @Test
    fun `Show only error when there are error, warning and info`() {
        val errorsPack = buildErrorsPack(
            itemsErrors = listOf(
                PriceChangeError.testBuilder().build(),
                NoStockError.create(),
                CountChangeError.testBuilder().build()
            )
        )
        checkoutErrorHandler.handleErrors(errorsPack = errorsPack, callback = callback)

        verify(callback).onAllNoStock()
        verify(callback, never()).onPriceModification()
        verify(callback, never()).onCountModification()
    }

    @Test
    fun `Show only first priority error`() {
        val shopError = ShopError.create("requestId")
        val errorsPack = buildErrorsPack(
            shopErrors = listOf(
                shopError
            ),
            itemsErrors = listOf(
                NoStockError.create()
            )
        )
        checkoutErrorHandler.handleErrors(errorsPack = errorsPack, callback = callback)

        verify(callback).onShopError(shopError)
        verify(callback, never()).onNoStock()
    }

    @Test
    fun `Show all info errors`() {
        val errorsPack = buildErrorsPack(
            itemsErrors = listOf(
                UnusedCoinError.create()
            )
        )
        checkoutErrorHandler.handleErrors(errorsPack = errorsPack, callback = callback)

        verify(callback).onUnusedCoin()
    }

    @Test
    fun `Show only NO_STOCK error and ignore payment amount error`() {
        val errorsPack = buildErrorsPack(
            shopErrors = listOf(
                TotalAmountError.create()
            ),
            itemsErrors = listOf(
                NoStockError.create()
            )
        )
        checkoutErrorHandler.handleErrors(errorsPack = errorsPack, callback = callback)

        verify(callback).onAllNoStock()
        verify(callback, never()).onPaymentAmount()
        verify(callback, never()).onTotalAmount()
    }

    @Test
    fun `Show only NO_STOCK error and ignore dimensions total error`() {
        val errorsPack = buildErrorsPack(
            shopErrors = listOf(
                DimensionsTotalError.create()
            ),
            itemsErrors = listOf(
                NoStockError.create()
            )
        )
        checkoutErrorHandler.handleErrors(errorsPack = errorsPack, callback = callback)

        verify(callback).onAllNoStock()
        verify(callback, never()).onDimensionsTotal()
    }

    @Test
    fun `Show only dimensions error and ignore payment amount error`() {
        val errorsPack = buildErrorsPack(
            shopErrors = listOf(
                TotalAmountError.create(),
                DimensionsTotalError.create()
            )
        )
        checkoutErrorHandler.handleErrors(errorsPack = errorsPack, callback = callback)

        verify(callback).onDimensionsTotal()
        verify(callback, never()).onPaymentAmount()
        verify(callback, never()).onTotalAmount()
    }

    private fun buildErrorsPack(
        globalErrors: List<CheckoutError> = emptyList(),
        shopErrors: List<CheckoutError> = emptyList(),
        itemsErrors: List<CheckoutError> = emptyList(),
        coinErrors: List<SmartCoinError> = emptyList(),
        warnings: List<CheckoutWarning> = emptyList()
    ): ErrorsPack {
        return ErrorsPack.builder()
            .errors(globalErrors)
            .warnings(warnings)
            .shopErrorsPacks(
                listOf(
                    ShopErrorsPack.builder()
                        .packId("packId")
                        .shopId("shopId")
                        .smartCoinErrors(
                            SmartCoinErrorsPack.create(coinErrors)
                        )
                        .errors(shopErrors)
                        .itemErrorsPacks(
                            listOf(
                                ItemErrorsPack.builder()
                                    .matchingKey("42")
                                    .errors(itemsErrors)
                                    .build()
                            )
                        )
                        .build()
                )
            )
            .build()
    }

}