package ru.yandex.market.clean.presentation.feature.sku

import com.annimon.stream.Stream
import com.annimon.stream.test.hamcrest.StreamMatcher.assertElements
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.summary.DeliveryTypeFormatter
import ru.yandex.market.clean.domain.model.offerDeliveryOptionTestInstance
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.domain.delivery.model.deliveryConditions_WithPriceTestInstance

class OfferDeliveryOptionsFormatterTest {

    private val deliveryTypeFormatter = mock<DeliveryTypeFormatter> {
        on { format(DeliveryType.DELIVERY, null, true, false) } doReturn NAME_DELIVERY
        on { format(DeliveryType.DELIVERY, null, false, false) } doReturn NAME_DELIVERY
        on { format(DeliveryType.PICKUP, null, false, false) } doReturn NAME_PICKUP
        on { format(DeliveryType.PICKUP, null, true, false) } doReturn NAME_PICKUP_CLICK_AND_COLLECT
    }
    private val conditionsFormatter = mock<DeliveryConditionsFormatter> {
        on { format(any(), any()) } doReturn deliveryOptionSubtitleVo_FormattedTestInstance()
    }
    private val formatter = OfferDeliveryOptionsFormatter(deliveryTypeFormatter, conditionsFormatter)

    @Test
    fun `Ignores post delivery option when courier delivery is present`() {
        val deliveryOptions = listOf(
            offerDeliveryOptionTestInstance(type = DeliveryType.DELIVERY),
            offerDeliveryOptionTestInstance(type = DeliveryType.PICKUP)
        )

        val formatted = formatter.map(
            deliveryOptions = deliveryOptions,
            isPreorder = false,
            isClickAndCollect = false,
            altOfferDetailedReasonVo = null,
            isMedicine = false,
        )

        Stream.of(formatted)
            .map { it.icon }
            .custom(assertElements(contains(DeliveryOptionVo.Icon.COURIER, DeliveryOptionVo.Icon.PIN)))
    }

    @Test
    fun `Delivery always goes before pickup`() {
        val deliveryOptions = listOf(
            offerDeliveryOptionTestInstance(type = DeliveryType.PICKUP),
            offerDeliveryOptionTestInstance(type = DeliveryType.DELIVERY)
        )

        val formatted = formatter.map(
            deliveryOptions = deliveryOptions,
            isPreorder = false,
            isClickAndCollect = false,
            altOfferDetailedReasonVo = null,
            isMedicine = false,
        )

        Stream.of(formatted)
            .map { it.icon }
            .custom(assertElements(contains(DeliveryOptionVo.Icon.COURIER, DeliveryOptionVo.Icon.PIN)))
    }

    @Test
    fun `Merge options with equal conditions for preorder`() {
        val conditions = deliveryConditions_WithPriceTestInstance()
        val deliveryOptions = listOf(
            offerDeliveryOptionTestInstance(type = DeliveryType.DELIVERY, conditions = conditions),
            offerDeliveryOptionTestInstance(type = DeliveryType.PICKUP, conditions = conditions)
        )
        val name = "Доставка и самовывоз"
        whenever(deliveryTypeFormatter.format(DeliveryType.DELIVERY, DeliveryType.PICKUP))
            .thenReturn(name)

        val formatted = formatter.map(
            deliveryOptions = deliveryOptions,
            isPreorder = true,
            isClickAndCollect = false,
            altOfferDetailedReasonVo = null,
            isMedicine = false,
        )

        assertThat(formatted).hasSize(1)
        assertThat(formatted[0].title).isEqualTo(name)
    }

    companion object {
        private const val NAME_DELIVERY = "Доставка"
        private const val NAME_PICKUP = "Самовывоз"
        private const val NAME_PICKUP_CLICK_AND_COLLECT = "Выкупить в торговом зале"
    }
}
