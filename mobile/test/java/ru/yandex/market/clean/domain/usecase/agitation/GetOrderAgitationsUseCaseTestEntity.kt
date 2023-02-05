package ru.yandex.market.clean.domain.usecase.agitation

import ru.yandex.market.clean.data.fapi.dto.agitationDtoTestInstance
import ru.yandex.market.clean.domain.model.agitations.AgitationOrderItem
import ru.yandex.market.clean.domain.model.agitations.AgitationType
import ru.yandex.market.clean.domain.model.agitations.OrderAgitation
import ru.yandex.market.clean.domain.model.agitations.OrderDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiffStage
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.model.order.orderItemDomainTestInstance
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.data.order.OrderSubstatus
import ru.yandex.market.data.order.PaymentType
import ru.yandex.market.domain.media.model.ImageReference
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.net.sku.SkuType
import java.math.BigDecimal

object GetOrderAgitationsUseCaseTestEntity {

    const val ORDER_ID = "980054"
    const val ORDER_DIFF_ID = "556772-333"

    val AGITATION_DTO_MOCK_1 = agitationDtoTestInstance()
    val AGITATION_DTO_MOCK_2 = agitationDtoTestInstance()

    private val ORDER_ITEM_DIFF_MOCK = OrderItemDiff(

        image = ImageReference.empty(),
        countBadgeText = "",
        skuId = "diff-sku-id",
        persistentOfferId = null,
        skuType = SkuType.MARKET,
        offerId = null,
        vendorId = null,
        basePrice = null,
        title = "Test",
        price = null,
        count = 1,
        deletedItemCount = 1,
        supplierId = null,
        supplierName = "Test",
        categoryId = null,
        modelId = null,
        feedOfferId = null,
        feedId = null,
        warehouseId = null,
        isHavePromocode = false,
        isHaveGift = false,
        isGroupOffer = false,
        discount = null,
        stage = OrderItemDiffStage.OTHER,
    )

    val ORDER_DIFF_MOCK = OrderDiff(ORDER_DIFF_ID, ORDER_ID, moneyTestInstance(), listOf(ORDER_ITEM_DIFF_MOCK))

    val AGITATION_ORDER_ITEM_MOCK = AgitationOrderItem(ImageReference.empty(), "countBadgeText", "skuId", null)

    val ORDER_MOCK = Order.generateTestInstance().copy(
        items = listOf(orderItemDomainTestInstance()),
        id = ORDER_ID.toLong(),
        status = OrderStatus.CANCELLED,
        subStatus = OrderSubstatus.BANK_REJECT_CREDIT_OFFER,
        shopOrderId = "shopOrderId",
        totalPrice = Money(BigDecimal.TEN, Currency.RUR)
    )

    val ORDER_AGITATION_DELETED_ITEM_RESULT = OrderAgitation(
        id = "1234",
        type = AgitationType.ORDER_ITEM_REMOVAL,
        orderId = ORDER_ID,
        orderItems = listOf(AGITATION_ORDER_ITEM_MOCK),
        orderDiff = ORDER_DIFF_MOCK,
        paymentType = PaymentType.PREPAID,
        orderStatus = OrderStatus.PROCESSING,
        orderTotal = moneyTestInstance(),
        isDsbs = true,
        trackingCode = null,
        eventId = null,
        isChangeDeliveryDatesAvailable = false,
        timeFrom = null,
        timeTo = null,
        beginDate = null,
        endDate = null,
        orderSubstatus = OrderSubstatus.UNKNOWN,
        onDemandWarehouseType = null,
        regionId = null,
    )

    val ORDER_CANCELLED_BY_USER_AGITATION_RESULT = OrderAgitation(
        id = "5678",
        type = AgitationType.ORDER_CANCELLED_BY_USER_EXTERNALLY,
        orderId = ORDER_ID,
        orderItems = listOf(AGITATION_ORDER_ITEM_MOCK),
        paymentType = PaymentType.POSTPAID,
        orderStatus = OrderStatus.CANCELLED,
        orderTotal = moneyTestInstance(),
        isDsbs = true,
        trackingCode = null,
        eventId = null,
        isChangeDeliveryDatesAvailable = false,
        timeFrom = null,
        timeTo = null,
        beginDate = null,
        endDate = null,
        orderSubstatus = OrderSubstatus.UNKNOWN,
        onDemandWarehouseType = null,
        regionId = null,
    )
}
