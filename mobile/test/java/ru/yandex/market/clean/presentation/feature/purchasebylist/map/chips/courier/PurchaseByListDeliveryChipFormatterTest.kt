package ru.yandex.market.clean.presentation.feature.purchasebylist.map.chips.courier

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.asViewObject
import ru.yandex.market.clean.domain.model.purchasebylist.DeliveryPeriod
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.courier.CourierSpeedType
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.courier.DeliveryPeriodToReadableNameFormatter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.courier.PurchaseByListDeliveryChipFormatter
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.chips.courier.PurchaseByListDeliveryChipVo
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.utils.Characters
import java.math.BigDecimal

@RunWith(Parameterized::class)
class PurchaseByListDeliveryChipFormatterTest(
    private val period: DeliveryPeriod,
    private val offersTotalPrice: BigDecimal,
    private val chipsDeliveryTotalPrice: BigDecimal,
    private val selectedDeliveryPeriod: DeliveryPeriod?,
    private val isLoading: Boolean,
    private val isBookingEnabled: Boolean,
    private val expectedOutput: PurchaseByListDeliveryChipVo,
) {

    private val deliveryPeriodFormatter = mock<DeliveryPeriodToReadableNameFormatter> {
        on { format(false, NON_NULL_EXPRESS_DELIVERY_PERIOD) } doReturn IN_ONE_TWO_HOURS
        on { format(false, NON_NULL_TODAY_DELIVERY_PERIOD) } doReturn TODAY
        on { format(false, NON_NULL_NEXT_DAY_DELIVERY_PERIOD) } doReturn TOMORROW
        on { format(false, NON_NULL_OTHER_DELIVERY_PERIOD) } doReturn COURIER_DELIVERY_TYPE
    }

    private val moneyFormatter = mock<MoneyFormatter> {
        on {
            formatAsMoneyVo(
                amount = offersTotalPrice,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = false
            )
        } doReturn Money.createRub(offersTotalPrice).asViewObject()

        on {
            formatAsMoneyVo(
                amount = chipsDeliveryTotalPrice,
                currency = Characters.RUBLE_SIGN.toString(),
                allowZeroMoney = true
            )
        } doReturn Money.createRub(chipsDeliveryTotalPrice).asViewObject()
    }

    @Test
    fun `Test delivery chip formatting`() {
        val formatter = PurchaseByListDeliveryChipFormatter(
            moneyFormatter = moneyFormatter,
            deliveryPeriodFormatter = deliveryPeriodFormatter,
        )

        val result = formatter.format(
            period = period,
            offersTotalPrice = offersTotalPrice,
            chipsDeliveryTotalPrice = chipsDeliveryTotalPrice,
            selectedDeliveryPeriod = selectedDeliveryPeriod,
            isLoading = isLoading,
            isBookingEnabled = isBookingEnabled,
        )

        assertThat(result).isEqualTo(expectedOutput)
    }

    private companion object {
        val NON_NULL_EXPRESS_DELIVERY_PERIOD = DeliveryPeriod.EXPRESS
        val NON_NULL_TODAY_DELIVERY_PERIOD = DeliveryPeriod.TODAY
        val NON_NULL_NEXT_DAY_DELIVERY_PERIOD = DeliveryPeriod.NEXT_DAY
        val NON_NULL_OTHER_DELIVERY_PERIOD = DeliveryPeriod.OTHER

        const val IN_ONE_TWO_HOURS = "Через 1${Characters.EN_DASH}2 часа"
        const val TODAY = "Сегодня"
        const val TOMORROW = "Завтра"
        const val COURIER_DELIVERY_TYPE = "Доставка"

        val OFFERS_TOTAL_PRICE = BigDecimal(100)
        val OFFERS_TOTAL_PRICE_FORMATTED =
            Money.createRub(OFFERS_TOTAL_PRICE).asViewObject().getFormatted()

        val CHIPS_DELIVERY_TOTAL_PRICE = BigDecimal(50)
        val CHIPS_DELIVERY_TOTAL_PRICE_FORMATTED =
            Money.createRub(CHIPS_DELIVERY_TOTAL_PRICE).asViewObject().getFormatted()

        val CHIPS_DELIVERY_TOTAL_PRICE_FREE = BigDecimal(0)
        val CHIPS_DELIVERY_TOTAL_PRICE_FREE_FORMATTED =
            Money.createRub(CHIPS_DELIVERY_TOTAL_PRICE_FREE).asViewObject().getFormatted()

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            // экспресс выбранный
            arrayOf(
                DeliveryPeriod.EXPRESS,
                OFFERS_TOTAL_PRICE,
                CHIPS_DELIVERY_TOTAL_PRICE,
                DeliveryPeriod.EXPRESS,
                false,
                false,
                PurchaseByListDeliveryChipVo(
                    courierSpeedType = CourierSpeedType.EXPRESS,
                    title = IN_ONE_TWO_HOURS,
                    offersTotalPrice = OFFERS_TOTAL_PRICE_FORMATTED,
                    deliveryTotalPrice = CHIPS_DELIVERY_TOTAL_PRICE_FORMATTED,
                    isSelected = true,
                    isLoading = false,
                    isVisible = true,
                )
            ),

            // сегодня не выбранный
            arrayOf(
                DeliveryPeriod.TODAY,
                OFFERS_TOTAL_PRICE,
                CHIPS_DELIVERY_TOTAL_PRICE,
                DeliveryPeriod.EXPRESS,
                false,
                false,
                PurchaseByListDeliveryChipVo(
                    courierSpeedType = CourierSpeedType.TODAY,
                    title = TODAY,
                    offersTotalPrice = OFFERS_TOTAL_PRICE_FORMATTED,
                    deliveryTotalPrice = CHIPS_DELIVERY_TOTAL_PRICE_FORMATTED,
                    isSelected = false,
                    isLoading = false,
                    isVisible = true,
                )
            ),

            // завтра, доставка ноль рублей
            arrayOf(
                DeliveryPeriod.NEXT_DAY,
                OFFERS_TOTAL_PRICE,
                CHIPS_DELIVERY_TOTAL_PRICE_FREE,
                DeliveryPeriod.NEXT_DAY,
                false,
                false,
                PurchaseByListDeliveryChipVo(
                    courierSpeedType = CourierSpeedType.TOMORROW,
                    title = TOMORROW,
                    offersTotalPrice = OFFERS_TOTAL_PRICE_FORMATTED,
                    deliveryTotalPrice = CHIPS_DELIVERY_TOTAL_PRICE_FREE_FORMATTED,
                    isSelected = true,
                    isLoading = false,
                    isVisible = true,
                )
            ),

            // other не показываем
            arrayOf(
                DeliveryPeriod.OTHER,
                OFFERS_TOTAL_PRICE,
                CHIPS_DELIVERY_TOTAL_PRICE_FREE,
                DeliveryPeriod.NEXT_DAY,
                false,
                false,
                PurchaseByListDeliveryChipVo(
                    courierSpeedType = CourierSpeedType.OTHER,
                    title = COURIER_DELIVERY_TYPE,
                    offersTotalPrice = OFFERS_TOTAL_PRICE_FORMATTED,
                    deliveryTotalPrice = CHIPS_DELIVERY_TOTAL_PRICE_FREE_FORMATTED,
                    isSelected = false,
                    isLoading = false,
                    isVisible = false,
                )
            ),

            // чипса грузиться - isLoading = true
            arrayOf(
                DeliveryPeriod.TODAY,
                OFFERS_TOTAL_PRICE,
                CHIPS_DELIVERY_TOTAL_PRICE_FREE,
                DeliveryPeriod.NEXT_DAY,
                true,
                false,
                PurchaseByListDeliveryChipVo(
                    courierSpeedType = CourierSpeedType.TODAY,
                    title = TODAY,
                    offersTotalPrice = OFFERS_TOTAL_PRICE_FORMATTED,
                    deliveryTotalPrice = CHIPS_DELIVERY_TOTAL_PRICE_FREE_FORMATTED,
                    isSelected = false,
                    isLoading = true,
                    isVisible = true,
                )
            )
        )
    }
}
