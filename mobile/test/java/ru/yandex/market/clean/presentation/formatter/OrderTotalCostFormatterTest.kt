package ru.yandex.market.clean.presentation.formatter

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.beru.android.R
import ru.yandex.market.data.order.OrderDto
import ru.yandex.market.data.order.OrderItemDto
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.common.android.ResourcesManager
import java.math.BigDecimal

class OrderTotalCostFormatterTest {

    private val moneyFormatter = mock<MoneyFormatter>()

    private val resourcesDataStore = mock<ResourcesManager>()

    private val formatter = OrderTotalCostFormatter(moneyFormatter, resourcesDataStore)

    @Test
    fun `Properly formats order total cost`() {
        val formattedAmount = "amount"
        whenever(moneyFormatter.formatPrice(any<Money>())).thenReturn(formattedAmount)
        val expectedResult = ""
        whenever(resourcesDataStore.getFormattedString(any(), any())).thenReturn(expectedResult)
        val totalAmount = BigDecimal.valueOf(100)
        val currency = Currency.BYN
        val order = OrderDto.testBuilder()
            .items(listOf(OrderItemDto.testBuilder().count(2).build()))
            .total(totalAmount)
            .currency(currency)
            .build()

        val formatted = formatter.format(order)

        verify(moneyFormatter).formatPrice(
            Money(totalAmount, currency)
        )
        verify(resourcesDataStore).getFormattedString(
            R.string.order_total_x,
            formattedAmount
        )
        assertThat(formatted).isEqualTo(expectedResult)
    }
}
