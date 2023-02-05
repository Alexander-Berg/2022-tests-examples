package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.data.order.OrderLiftTypeDto
import ru.yandex.market.data.order.PaymentType
import ru.yandex.market.data.passport.Address
import ru.yandex.market.extensions.formatAsPriceString
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.utils.Characters

@RunWith(Parameterized::class)
class OrderLiftToFloorFormatterTest(
    private val input: Order,
    private val expectedResult: String?
) {

    private val elevatorMessage = when (input.liftType) {
        OrderLiftTypeDto.CARGO_ELEVATOR -> CARGO_ELEVATOR
        OrderLiftTypeDto.ELEVATOR -> ELEVATOR
        OrderLiftTypeDto.MANUAL -> MANUAL
        else -> NOT_NEEDED
    }

    private val priceMessage = when (input.paymentType) {
        PaymentType.POSTPAID -> {
            val liftPrice = input.liftPrice
            "${liftPrice.formatAsPriceString()}${Characters.NON_BREAKING_SPACE}${Characters.RUBLE_SIGN}"
        }
        else -> PAID
    }

    private val resourceDataSource = mock<ResourcesManager> {
        on {
            getString(R.string.order_details_delivery_floor_free)
        } doReturn FREE
        on {
            getString(R.string.order_details_delivery_floor_with_elevator)
        } doReturn ELEVATOR
        on {
            getString(R.string.order_details_delivery_floor_without_elevator)
        } doReturn MANUAL
        on {
            getString(R.string.order_details_delivery_floor_paid)
        } doReturn PAID
        on {
            getFormattedString(
                R.string.order_details_delivery_floor_paid_with_price,
                input.address?.floor,
                elevatorMessage,
                priceMessage
            )
        } doReturn input.address?.floor + " этаж, " + elevatorMessage + ", " + priceMessage
        on {
            getString(R.string.order_details_delivery_floor_unpaid)
        } doReturn NOT_NEEDED
        on {
            getString(R.string.order_details_delivery_floor_with_cargo_elevator)
        } doReturn CARGO_ELEVATOR
    }

    private val formatter = OrderLiftToFloorFormatter(resourceDataSource)

    @Test
    fun format() {
        val formatted = formatter.format(input)
        assertThat(formatted).isEqualTo(expectedResult)
    }

    companion object {

        private const val FREE = "бесплатно"
        private const val ELEVATOR = "пассажирский лифт"
        private const val CARGO_ELEVATOR = "грузовой лифт"
        private const val MANUAL = "без лифта"
        private const val NOT_NEEDED = "не включён"
        private const val PAID = "оплачено"
        private val LIFT_PRICE = 1000.toBigDecimal()

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            // 0
            arrayOf(
                Order.generateTestInstance().copy(address = Address.empty()),
                null
            ),
            // 1
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.testBuilder().build(),
                    liftType = null
                ),
                null
            ),
            // 2
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.empty(),
                    liftType = OrderLiftTypeDto.FREE
                ),
                null
            ),
            // 3
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.testBuilder().build(),
                    liftType = OrderLiftTypeDto.FREE
                ),
                "бесплатно"
            ),
            // 4
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.testBuilder().floor("12").build(),
                    liftType = OrderLiftTypeDto.ELEVATOR,
                    paymentType = PaymentType.PREPAID
                ),
                "12 этаж, пассажирский лифт, оплачено"
            ),
            // 5
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.testBuilder().floor("12").build(),
                    liftType = OrderLiftTypeDto.MANUAL,
                    liftPrice = LIFT_PRICE,
                    paymentType = PaymentType.POSTPAID
                ),
                "12 этаж, без лифта, 1 000 ₽"
            ),
            // 6
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.testBuilder().floor("12").build(),
                    liftType = OrderLiftTypeDto.CARGO_ELEVATOR,
                    liftPrice = LIFT_PRICE,
                    paymentType = PaymentType.POSTPAID
                ),
                "12 этаж, грузовой лифт, 1 000 ₽"
            ),
            // 7
            arrayOf(
                Order.generateTestInstance().copy(
                    address = Address.testBuilder().floor("12").build(),
                    liftType = OrderLiftTypeDto.NOT_NEEDED
                ),
                "не включён"
            )
        )
    }
}