package ru.yandex.market.clean.presentation.feature.checkout.confirm

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.presentation.feature.checkout.confirm.delivery.CheckoutConfirmDeliveryIntervalItemVo
import ru.yandex.market.clean.presentation.feature.checkout.confirm.delivery.DeliveryDateType
import ru.yandex.market.clean.presentation.feature.checkout.confirm.delivery.DeliveryTimeIntervalItemVo
import ru.yandex.market.clean.presentation.feature.checkout.confirm.delivery.OnDemandDeliveryVo
import ru.yandex.market.clean.presentation.vo.DeliveryTimeIntervalVo
import ru.yandex.market.data.payment.network.dto.PaymentMethod

class CheckoutConfirmDeliveryIntervalItemVoEqualsTest {

    @Test
    fun `Equaling same samples`() {
        val copySample = firstSample.copy()
        assertThat(firstSample == copySample).isEqualTo(true)
    }

    @Test
    fun `Equaling samples with no difference in critical fields`() {
        val sampleWithoutCriticalDifferences = firstSample.copy(
            showPrice = true,
            isOndemandAnotherDateAvailable = true,
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY
        )

        assertThat(firstSample == sampleWithoutCriticalDifferences).isEqualTo(true)
    }

    @Test
    fun `Equaling samples with different packId`() {
        val sampleWithDifferentPackId = firstSample.copy(
            packId = secondPackId
        )

        assertThat(firstSample == sampleWithDifferentPackId).isEqualTo(false)
    }

    @Test
    fun `Equaling samples with different onDemandDelivery`() {
        val sampleWithDifferentOnDemandDelivery = firstSample.copy(
            onDemandDelivery = onDemandDeliveryVo.copy(
                dateSubtitle = "date2"
            )
        )

        assertThat(firstSample == sampleWithDifferentOnDemandDelivery).isEqualTo(false)
    }

    @Test
    fun `Equaling samples with different isExpress`() {
        val sampleWithDifferentIsExpress = firstSample.copy(
            isExpress = !firstSample.isExpress
        )

        assertThat(firstSample == sampleWithDifferentIsExpress).isEqualTo(false)
    }

    @Test
    fun `Equaling samples with different cheapestDeliveryTimeIntervalVo`() {
        val sampleWithDifferentCheapestDeliveryTimeIntervalVo = firstSample.copy(
            cheapestDeliveryTimeIntervalVo = cheapestDeliveryVo.copy(
                packId = secondPackId
            )
        )

        assertThat(firstSample == sampleWithDifferentCheapestDeliveryTimeIntervalVo).isEqualTo(false)
    }

    @Test
    fun `Equaling samples with different deliveryTimeIntervals`() {
        val newTimeIntervals = firstSample.deliveryTimeIntervals.toMutableList().apply {
            add(cheapestDeliveryVo)
        }

        val sampleWithDifferentTimeIntervals = firstSample.copy(
            deliveryTimeIntervals = newTimeIntervals
        )

        assertThat(firstSample == sampleWithDifferentTimeIntervals).isEqualTo(false)
    }

    companion object {
        private const val firstPackId = "1"
        private const val secondPackId = "2"
        private val onDemandDeliveryVo = OnDemandDeliveryVo(
            "date",
            "deliveryDate",
            false,
            false,
            "promoHint",
        )

        private val cheapestDeliveryVo = DeliveryTimeIntervalItemVo(
            packId = firstPackId,
            optionId = "1",
            date = DeliveryDateType.SAME_DAY,
            timeInterval = DeliveryTimeIntervalVo(
                id = "12:00 : 13:30",
                timeIntervalString = "12:00 - 13:30, 99",
                timeFrom = "12:00",
                timeTo = "13:30",
                isSelected = false,
                isOneHourInterval = false,
                isExpress = false,
                isFastestExpress = false,
                price = "99",
            )
        )

        val firstSample = CheckoutConfirmDeliveryIntervalItemVo(
            packId = firstPackId,
            onDemandDelivery = onDemandDeliveryVo,
            showPrice = false,
            showIconsOnIntervals = false,
            isOndemandAnotherDateAvailable = false,
            deliveryTimeIntervals = emptyList(),
            cheapestDeliveryTimeIntervalVo = cheapestDeliveryVo,
            isExpress = true,
            paymentMethod = PaymentMethod.CARD_ON_DELIVERY,
        )
    }
}
