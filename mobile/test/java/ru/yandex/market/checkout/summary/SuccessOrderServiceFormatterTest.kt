package ru.yandex.market.checkout.summary

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.OrderItem
import ru.yandex.market.clean.domain.model.checkout.orderOptionsServiceTestInstance
import ru.yandex.market.clean.presentation.feature.order.details.services.OrderServiceDateFormatter
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.feature.money.viewobject.MoneyVo
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class SuccessOrderServiceFormatterTest {

    private val stringCaptor = ArgumentCaptor.forClass(String::class.java)
    private val intCaptor = ArgumentCaptor.forClass(Int::class.java)

    private val orderServiceDateFormatter = mock<OrderServiceDateFormatter> {
        on {
            format(eq(ORDER_SERVICES[0]))
        } doReturn DATES[0].second

        on {
            format(eq(ORDER_SERVICES[1]))
        } doReturn DATES[1].second
    }

    private val resourcesDataStore = mock<ResourcesManager> {
        on {
            getString(R.string.success_service_title)
        } doReturn SERVICE_TITLE

        on {
            getFormattedString(eq(R.string.template_service_date), stringCaptor.capture())
        } doAnswer { SERVICE_DATE.format(stringCaptor.value) }

        on {
            getFormattedString(eq(R.string.service_status_postpay), stringCaptor.capture())
        } doAnswer { SERVICE_STATUS.format(stringCaptor.value) }

        on {
            getFormattedString(eq(R.string.template_units_piece), intCaptor.capture())
        } doAnswer { UNITS.format(intCaptor.value) }
    }

    private val moneyFormatter = mock<MoneyFormatter> {
        on {
            formatAsMoneyVo(PRICES[0])
        } doReturn MoneyVo.builder().amount("1000").currency('₽').separator(' ').build()

        on {
            formatAsMoneyVo(PRICES[1])
        } doReturn MoneyVo.builder().amount("2000").currency('₽').separator(' ').build()
    }

    private val orderItem1 = mock<OrderItem> {
        on {
            service
        } doReturn ORDER_SERVICES[0]

        on {
            count
        } doReturn COUNT
    }

    private val orderItem2 = mock<OrderItem> {
        on {
            service
        } doReturn ORDER_SERVICES[1]

        on {
            count
        } doReturn COUNT
    }

    private val formatter = SuccessOrderServiceFormatter(orderServiceDateFormatter, resourcesDataStore, moneyFormatter)

    @Test
    fun format() {
        val formatted = listOf(orderItem1, orderItem2).mapNotNull(formatter::format)
        assertThat(formatted.size).isEqualTo(2)
        formatted.forEachIndexed { index, value ->
            assertThat(value.item.title).isEqualTo(ORDER_SERVICES[index].title)
            assertThat(value.serviceStatus).isEqualTo(SERVICE_DATES[index])
            assertThat(value.postPaymentStatus).isEqualTo(STATUSES[index])
        }
    }

    companion object {
        val DATES = listOf(
            Pair(Date(2021, 7, 21), "в субботу, 21 августа, с 12:00 до 14:00"),
            Pair(Date(2021, 9, 29), "в пятницу, 29 октября, с 16:00 до 18:00"),
        )

        val SERVICE_DATES = listOf(
            "Запланирована в субботу, 21 августа, с 12:00 до 14:00. Чуть позже сообщим точное время, когда приедет мастер.",
            "Запланирована в пятницу, 29 октября, с 16:00 до 18:00. Чуть позже сообщим точное время, когда приедет мастер.",
        )

        val STATUSES = listOf(
            "Оплата услуги после оказания 1000 ₽",
            "Оплата услуги после оказания 2000 ₽"
        )

        val PRICES = listOf(
            Money.createRub(1000),
            Money.createRub(2000)
        )

        val ORDER_SERVICES = listOf(
            orderOptionsServiceTestInstance(
                date = DATES[0].first,
                price = PRICES[0],
            ),
            orderOptionsServiceTestInstance(
                date = DATES[1].first,
                price = PRICES[1],
            )
        )

        private const val SERVICE_TITLE = "Установка"
        private const val SERVICE_DATE = "Запланирована %s. Чуть позже сообщим точное время, когда приедет мастер."
        private const val SERVICE_STATUS = "Оплата услуги после оказания %s"
        private const val UNITS = "%d шт."
        private const val COUNT = 1
    }
}
