package ru.yandex.market.clean.presentation.feature.cart.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.lavka2.cart.LavkaCart
import ru.yandex.market.clean.domain.model.lavka2.cart.lavkaCartItemTestInstance
import ru.yandex.market.clean.domain.model.lavka2.cart.lavkaCartTestInstance
import ru.yandex.market.clean.presentation.feature.cart.vo.LavkaCartVo
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.formatter.PhotoFormatter
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.feature.money.viewobject.moneyVoTestInstance
import java.math.BigDecimal

class LavkaCartFormatterTest {

    private val resourceManager: ResourcesManager = mock()
    private val photoFormatter: PhotoFormatter = mock()
    private val moneyFormatter: MoneyFormatter = mock()

    private val formatter = LavkaCartFormatter(
        resourceManager = resourceManager,
        photoFormatter = photoFormatter,
        moneyFormatter = moneyFormatter,
    )

    private val lavkaCart = lavkaCartTestInstance(
        items = listOf(
            lavkaCartItemTestInstance().copy(
                price = DUMMY_PRICE.toString(),
                discountPrice = DUMMY_DISCOUNT_PRICE.toString(),
                formattedCurrency = Currency.RUR,
                cashback = null
            )
        )
    )

    @Test
    fun `should return null if lavka cart is empty`() {
        val expected = expectedVo(cart = null)
        assertThat(expected).isNull()
    }

    @Test
    fun `should return null if lavka has cart exists but has no items`() {
        val cart = lavkaCart.copy(items = emptyList())
        val expected = expectedVo(cart = cart)
        assertThat(expected).isNull()
    }

    @Test
    fun `should return null if lavka has cart exists and items but has no total price`() {
        val cart = lavkaCart.copy(items = emptyList(), totalPrice = null)
        val expected = expectedVo(cart = cart)
        assertThat(expected).isNull()
    }

    @Test
    fun `should return null if lavka has cart exists and items but total price is blank`() {
        val cart = lavkaCart.copy(items = emptyList(), totalPrice = "null")
        val expected = expectedVo(cart = cart)
        assertThat(expected).isNull()
    }

    @Test
    fun `should return LavkaCartVo`() {
        whenever(resourceManager.getString(any())).thenReturn("1")
        whenever(resourceManager.getQuantityString(any(), any())).thenReturn("1")
        whenever(resourceManager.getColor(any())).thenReturn(1)
        whenever(resourceManager.getDrawable(any())).thenReturn(null)

        whenever(
            moneyFormatter.formatPriceAsViewObject(Money(BigDecimal(DUMMY_PRICE), Currency.RUR))
        ).thenReturn(moneyVoTestInstance())

        whenever(
            moneyFormatter.formatPriceAsViewObject(Money(BigDecimal(DUMMY_DISCOUNT_PRICE), Currency.RUR))
        ).thenReturn(moneyVoTestInstance())

        val expected = expectedVo(cart = lavkaCart)
        assertThat(expected).isInstanceOf(LavkaCartVo::class.java)
    }

    private fun expectedVo(cart: LavkaCart?): LavkaCartVo? {
        return formatter.format(
            lavkaCart = cart,
            lavketPageId = DUMMY_LAVKET_PAGE_ID,
            hasYandexPlus = true,
            showCashBack = true,
        )
    }

    private companion object {
        const val DUMMY_LAVKET_PAGE_ID = "DUMMY_LAVKET_PAGE_ID"
        const val DUMMY_PRICE = 42
        const val DUMMY_DISCOUNT_PRICE = 23
    }

}
