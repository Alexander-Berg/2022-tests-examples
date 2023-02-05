package ru.yandex.market.clean.presentation.feature.purchasebylist.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.OfferSpecificationInternal
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.feature.purchaseByList.analogs.PurchaseByListSelectedAnalog
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.PurchaseByListCartItem
import ru.yandex.market.clean.presentation.feature.purchaseByList.map.PurchaseByListOrder
import ru.yandex.market.clean.presentation.feature.purchaseByList.mapper.PurchaseByListOrderMapper
import ru.yandex.market.images.ImageUrlFormatter

class PurchaseByListOrderMapperTest {

    private val imageUrlFormatter = mock<ImageUrlFormatter>() {
        on { format(any(), anyOrNull()) }.doReturn("")
    }

    private val purchaseByListOrderMapper = PurchaseByListOrderMapper(imageUrlFormatter)

    @Test
    fun `Check correct map from selected analog`() {
        val purchaseByListOrder = purchaseByListOrderMapper.map(selectedAnalog, purchaseByListCartItem)
        assertThat(purchaseByListOrder).isEqualTo(correctPurchaseByListOrder)
    }

    companion object {
        private const val OLD_SKU_ID = "OLD_SKU_ID"
        private const val COUNT = 42
        private const val IMAGE_URL = ""
        private const val ATC_CODE = "ATC_CODE"

        private val cartItem = cartItemTestInstance()
        private val purchaseByListCartItem = PurchaseByListCartItem.fromCartItem(cartItem)
        private val newProductOffer = productOfferTestInstance().copy(
            internalOfferProperties = OfferSpecificationInternal(usedParams = listOf(ATC_CODE))
        )
        private val oldProductOffer = productOfferTestInstance()

        private val selectedAnalog = PurchaseByListSelectedAnalog(
            oldOffer = oldProductOffer,
            oldSkuId = OLD_SKU_ID,
            newOffer = newProductOffer,
            count = COUNT,
        )

        private val correctPurchaseByListOrder = PurchaseByListOrder(
            skuId = OLD_SKU_ID,
            title = newProductOffer.title,
            imageUrl = IMAGE_URL,
            count = COUNT,
            atcCode = ATC_CODE,
            vendorId = newProductOffer.vendorId,
            supplierId = newProductOffer.supplierId,
            persistentOfferId = newProductOffer.persistentOfferId,
            hasPrescription = newProductOffer.internalOfferProperties.isOfferPrescription,
            purchaseByListCartItem = purchaseByListCartItem,
            analogSkuId = oldProductOffer.stockKeepingUnitId,
        )
    }
}