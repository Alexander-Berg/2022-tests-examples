package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.agitations.AgitationOrderItem
import ru.yandex.market.clean.domain.model.agitations.AgitationType
import ru.yandex.market.clean.domain.model.agitations.OrderAgitation
import ru.yandex.market.clean.domain.model.agitations.OrderDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiffStage
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.data.order.OrderSubstatus
import ru.yandex.market.data.order.PaymentType
import ru.yandex.market.domain.media.model.ImageReference
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.feature.money.viewobject.moneyVoTestInstance
import ru.yandex.market.net.sku.SkuType

class OrderAgitationFormatterTest {

    private val moneyFormatter = mock<MoneyFormatter>()

    private var agitationFormatter: OrderAgitationFormatter = OrderAgitationFormatter(moneyFormatter)

    private val formattedAmount = moneyVoTestInstance()
    private val agitationId = "agitation-id"
    private val orderId = "order-id"
    private val eventId = "event-id"
    private val orderStatus = OrderStatus.DELIVERY
    private val orderDiffId = "order-diff-id"
    private val paymentType = PaymentType.PREPAID
    private val orderDeliveryInfo = "order-delivery-info"


    @Test
    fun `Testing formatting of the agitation with order diff`() {
        whenever(moneyFormatter.formatAsMoneyVo(any<Money>(), any<String>(), any<String>())).thenReturn(formattedAmount)

        val orderItemDiff = OrderItemDiff(
            ImageReference.empty(),
            "",
            "diff-sku-id",
            null,
            null,
            null,
            null,
            SkuType.MARKET,
            null,
            null,
            null,
            "Test",
            null,
            1,
            0,
            null,
            "Test",
            null,
            null,
            false,
            false,
            false,
            null,
            OrderItemDiffStage.OTHER
        )
        val orderDiff = OrderDiff(orderDiffId, orderId, moneyTestInstance(), listOf(orderItemDiff))

        val orderItems = listOf(
            AgitationOrderItem(ImageReference.empty(), "1", "sku-id-1", null),
            AgitationOrderItem(ImageReference.empty(), "", "sku-id-2", null),
        )

        val agitation = OrderAgitation(
            id = agitationId,
            type = AgitationType.ORDER_ITEM_REMOVAL,
            orderId = orderId,
            orderStatus = orderStatus,
            orderItems = orderItems,
            orderTotal = moneyTestInstance(),
            paymentType = paymentType,
            isDsbs = true,
            orderDeliveryInfo = orderDeliveryInfo,
            orderDiff = orderDiff,
            trackingCode = null,
            eventId = eventId,
            isChangeDeliveryDatesAvailable = false,
            timeFrom = null,
            timeTo = null,
            beginDate = null,
            endDate = null,
            orderSubstatus = OrderSubstatus.UNKNOWN,
            onDemandWarehouseType = null,
            regionId = null,
        )

        val agitationVo = agitationFormatter.format(agitation)

        assertThat(agitationVo?.agitationId).isEqualTo(agitationId)
        assertThat(agitationVo?.orderId).isEqualTo(orderId)
        assertThat(agitationVo?.orderStatus).isEqualTo(orderStatus)
        assertThat(agitationVo?.agitationType).isEqualTo(AgitationType.ORDER_ITEM_REMOVAL)
        assertThat(agitationVo?.delta).isEqualTo(formattedAmount)
        assertThat(agitationVo?.orderItems).hasSize(orderDiff.items.size)
        assertThat(agitationVo?.orderItems?.get(0)?.skuId).isEqualTo(orderItemDiff.skuId)
        assertThat(agitationVo?.paymentType).isEqualTo(paymentType)
    }

    @Test
    fun `Testing formatting of the agitation without order diff`() {
        whenever(moneyFormatter.formatAsMoneyVo(any<Money>(), any<String>(), any<String>())).thenReturn(formattedAmount)

        val orderItems = listOf(
            AgitationOrderItem(ImageReference.empty(), "2", "sku-id-1", null),
            AgitationOrderItem(ImageReference.empty(), "", "sku-id-2", null),
            AgitationOrderItem(ImageReference.empty(), "", "sku-id-3", null),
        )

        val agitation = OrderAgitation(
            id = agitationId,
            type = AgitationType.ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY,
            orderId = orderId,
            eventId = eventId,
            orderStatus = orderStatus,
            orderItems = orderItems,
            orderTotal = moneyTestInstance(),
            paymentType = paymentType,
            isDsbs = true,
            orderDeliveryInfo = orderDeliveryInfo,
            orderDiff = null,
            trackingCode = null,
            isChangeDeliveryDatesAvailable = false,
            timeFrom = null,
            timeTo = null,
            beginDate = null,
            endDate = null,
            orderSubstatus = OrderSubstatus.UNKNOWN,
            onDemandWarehouseType = null,
            regionId = null,
        )

        val agitationVo = agitationFormatter.format(agitation)

        assertThat(agitationVo?.agitationId).isEqualTo(agitationId)
        assertThat(agitationVo?.orderId).isEqualTo(orderId)
        assertThat(agitationVo?.orderStatus).isEqualTo(orderStatus)
        assertThat(agitationVo?.agitationType).isEqualTo(AgitationType.ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY)
        assertThat(agitationVo?.delta).isNull()
        assertThat(agitationVo?.orderItems).hasSize(orderItems.size)
        assertThat(agitationVo?.orderItems?.get(1)?.skuId).isEqualTo(orderItems[1].skuId)
        assertThat(agitationVo?.paymentType).isEqualTo(paymentType)
    }
}
