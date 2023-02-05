package ru.yandex.market.clean.presentation.feature.cart.formatter

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.model.actualizedCartItemTestInstance
import ru.yandex.market.clean.domain.model.actualizedCartTestInstance
import ru.yandex.market.clean.domain.model.cart.CartValidationResult
import ru.yandex.market.clean.domain.model.cart.cartValidationResultTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.lavka2.cart.lavkaCartTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedOnce_ActualizedTestInstance
import ru.yandex.market.clean.presentation.feature.cart.CartType
import ru.yandex.market.clean.presentation.feature.cart.vo.CartVo
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.formatter.PricesFormatter
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.feature.price.pricesVoTestInstance

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class NewCheckoutButtonFormatterTest {

    private val resourceManager = ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    private val pricesFormatter: PricesFormatter = mock {
        on {
            format(
                orderSummary = any(),
                isCartEmpty = any(),
                isPriceDropPromoApplied = any(),
                areSmartCoinsApplied = any(),
                prefix = anyOrNull(),
                isSummaryServicesCalculationEnabled = any(),
            )
        } doReturn pricesVoTestInstance()
    }
    private val moneyFormatter: MoneyFormatter = mock {
        on {
            formatAsMoneyVo(money = any(), allowZeroMoney = any(), prefix = any(), suffix = any())
        } doReturn MoneyVo.empty()
        on {
            formatAsMoneyVo(money = any(), prefix = any(), suffix = any())
        } doReturn MoneyVo.empty()
    }
    private val eatsRetailCartFormatter: EatsRetailCartFormatter = mock {
        on { formatCartType(any()) } doReturn CartType.Retail("cartId", "shopName")
    }

    private val formatter = NewCheckoutButtonFormatter(
        resourcesManager = resourceManager,
        pricesFormatter = pricesFormatter,
        cartPricePrefixFormatter = mock(),
        moneyFormatter = moneyFormatter,
        eatsRetailCartFormatter = eatsRetailCartFormatter,
    )

    @Test
    fun `Multicart (Market + Lavka + EatsRetail) - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketCart(isExpired = false),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = lavkaCartTestInstance(),
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = mapOf(
                "42" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(),
            ),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `Multicart (Market + Lavka) - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketCart(isExpired = false),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = lavkaCartTestInstance(),
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = emptyMap(),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `Multicart (Market + EatsRetail) - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketCart(isExpired = false),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = null,
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = mapOf(
                "42" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(),
            ),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `Multicart (Lavka + EatsRetail) - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketEmptyCart(),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = lavkaCartTestInstance(),
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = mapOf(
                "42" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(),
            ),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `Market cart - TopCheckoutButton is visible and enabled`() {
        val actual = formatter.format(
            cart = createMarketCart(isExpired = false),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = null,
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = emptyMap(),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isTrue
        assertThat(actual.isTopCheckoutButtonVisible).isTrue
    }

    @Test
    fun `Market cart (all Expired) - TopCheckoutButton is visible and disabled`() {
        val actual = formatter.format(
            cart = createMarketCart(isExpired = true),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = null,
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = emptyMap(),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isTrue
    }

    @Test
    fun `Lavka cart - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketEmptyCart(),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = lavkaCartTestInstance(),
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = emptyMap(),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `EatsRetail cart - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketEmptyCart(),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = null,
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = mapOf(
                "42" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(),
            ),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `Multicart (EatsRetail + EatsRetail) - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketEmptyCart(),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = null,
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = mapOf(
                "42" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(),
                "55" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(),
            ),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    @Test
    fun `Empty cart - TopCheckoutButton is invisible`() {
        val actual = formatter.format(
            cart = createMarketEmptyCart(),
            lastCartVo = null,
            actualization = true,
            isFraudDetected = false,
            appliedCoinsCount = 42,
            hasFailedStrategy = false,
            isSummaryServicesCalculationEnabled = true,
            lavkaCart = null,
            hasActualizingItems = true,
            cartState = CartVo.CartState.NORMAL,
            eatsRetailCarts = emptyMap(),
        )

        assertThat(actual.isTopCheckoutButtonEnabled).isFalse
        assertThat(actual.isTopCheckoutButtonVisible).isFalse
    }

    private fun createMarketCart(isExpired: Boolean): CartValidationResult {
        return cartValidationResultTestInstance(
            cart = actualizedCartTestInstance(
                actualizedItems = listOf(
                    actualizedCartItemTestInstance(
                        cartItem = cartItemTestInstance(
                            isExpired = isExpired,
                        ),
                    ),
                ),
            ),
        )
    }

    private fun createMarketEmptyCart(): CartValidationResult {
        return cartValidationResultTestInstance(
            cart = actualizedCartTestInstance(
                actualizedItems = emptyList(),
                missingCartItems = emptyList(),
            ),
        )
    }
}
