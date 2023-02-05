package ru.yandex.market.clean.domain.usecase.agitation

import ru.yandex.market.clean.domain.model.agitations.OrderDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiff
import ru.yandex.market.clean.domain.model.agitations.OrderItemDiffStage
import ru.yandex.market.clean.domain.model.sku.detailedSkuTestInstance
import ru.yandex.market.clean.domain.model.supplierTestInstance
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.domain.media.model.ImageReference
import ru.yandex.market.net.sku.SkuType

object GetOrderDiffWithSkuInformationUseCaseTestEntity {

    const val ORDER_ID = "13391023"

    val DETAILED_SKU_MOCK = detailedSkuTestInstance()
    val SUPPLIER_MOCK = supplierTestInstance()

    val ORDER_ITEM_DIFF_MOCK = OrderItemDiff(

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
        deletedItemCount = 0,
        supplierId = 123L,
        supplierName = "Test",
        categoryId = null,
        modelId = null,
        feedOfferId = null,
        warehouseId = null,
        feedId = null,
        isHavePromocode = false,
        isHaveGift = false,
        isGroupOffer = false,
        discount = null,
        stage = OrderItemDiffStage.OTHER
    )

    val ORDER_DIFF_MOCK = OrderDiff(

        id = "1",
        orderId = ORDER_ID,
        delta = moneyTestInstance(),
        items = listOf(ORDER_ITEM_DIFF_MOCK)
    )
}
