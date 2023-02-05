package com.edadeal.android.data

import com.edadeal.android.model.entity.MetaOffer
import com.edadeal.android.model.entity.Entity
import com.edadeal.protobuf.content.v3.mobile.Offer.Builder
import com.edadeal.android.model.entity.Offer
import com.edadeal.android.model.entity.OfferEntity
import com.edadeal.android.model.entity.Retailer
import com.edadeal.android.model.entity.Segment
import com.edadeal.android.model.toUuidByteString
import okio.ByteString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OfferExtensionsTest {

    @Test
    fun `hasMetaId should return false if metaId is equals EMPTY_ID`() {
        val offerWithEmptyMetaId = getOffer(Entity.EMPTY_ID)
        assertFalse(offerWithEmptyMetaId.hasMetaId())
    }

    @Test
    fun `hasMetaId should return false if metaId is null`() {
        val offerWithNullMetaId = getOffer(null)
        assertFalse(offerWithNullMetaId.hasMetaId())
    }

    @Test
    fun `hasMetaId should return true if metaId is correct`() {
        val offerWithCorrectMetaId = getOffer("f0ef256d-393a-48ff-ba58-cd0d43a1b55f".toUuidByteString())
        assertTrue(offerWithCorrectMetaId.hasMetaId())
    }

    @Test
    fun `distinctByMetaOffer should merge items with same metaId and retailerId to MetaOffer's`() {
        val metaOffer = MetaOffer.merge(offer3, offer4withSameMetaAndRetailerIdAs3)
            .mergeWith(offer5withSameMetaAndRetailerIdAs3)
        val items = listOf(offer1, offer2withSameRetailerIdAs1, metaOffer, offer6withSameMetaAs3)

        assertEquals(items, allOffers.distinctByMetaOffer().toList())
    }

    @Test
    fun `distinctByMetaOffer should return same collection if where is none items to merge`() {
        val items = listOf(offer1, offer3, offer6withSameMetaAs3)

        assertSame(items, items.distinctByMetaOffer())
    }

    companion object {

        private val offer1: Offer = makeOffer(
            id = "258dba22-24cc-441a-9615-1bfa691847fd".toUuidByteString(),
            retailerId = "94d9bd2d-42c3-11e6-9419-52540010b608".toUuidByteString(),
            priceNew = 75f
        )
        private val offer2withSameRetailerIdAs1: Offer = makeOffer(
            id = "e346eba9-e596-496b-8c9b-5e28f247fde4".toUuidByteString(),
            retailerId = offer1.retailer.id,
            priceNew = 250f
        )
        private val offer3: Offer = makeOffer(
            id = "d1a085c8-c4e9-493f-98ed-0c452b8b3546".toUuidByteString(),
            metaId = "f0ef256d-393a-48ff-ba58-cd0d43a1b55f".toUuidByteString(),
            retailerId = "98123a52-b23e-4479-b705-b2f9d7b9fc08".toUuidByteString(),
            priceNew = 125f
        )
        private val offer4withSameMetaAndRetailerIdAs3: Offer = makeOffer(
            id = "8a38ba5c-842e-4542-8efc-3639a740b63c".toUuidByteString(),
            metaId = offer3.metaId,
            retailerId = offer3.retailer.id,
            priceNew = 100f
        )
        private val offer5withSameMetaAndRetailerIdAs3: Offer = makeOffer(
            id = "1f5fb779-9b8f-4427-aaf0-fa67298df438".toUuidByteString(),
            metaId = offer3.metaId,
            retailerId = offer3.retailer.id,
            priceNew = 150f
        )
        private val offer6withSameMetaAs3: Offer = makeOffer(
            id = "b0de09f0-4916-4906-8f9a-748231541842".toUuidByteString(),
            metaId = offer3.metaId,
            retailerId = "c527b79c-c13d-4161-9014-9aa916e7c6db".toUuidByteString(),
            priceNew = 175f
        )

        private val allOffers = listOf(offer1, offer2withSameRetailerIdAs1, offer3,
            offer4withSameMetaAndRetailerIdAs3, offer5withSameMetaAndRetailerIdAs3, offer6withSameMetaAs3)

        private fun getOffer(metaId: ByteString?) = Builder().metaId(metaId).build()

        private fun makeOffer(
            id: ByteString,
            retailerId: ByteString,
            priceNew: Float,
            metaId: ByteString? = null
        ) = OfferEntity(
            id = id,
            metaId = metaId ?: ByteString.EMPTY,
            description = "",
            imageUrl = "",
            priceOld = 0f,
            priceNew = priceNew,
            priceIsFrom = false,
            quantity = 0f,
            quantityUnit = "",
            discount = 0f,
            discountUnit = "",
            discountPercent = 0,
            discountLabel = "",
            dateStart = "",
            dateEnd = "",
            country = "",
            calculatedPrices = emptyList(),
            compilationIds = emptyList(),
            brandIds = emptyList(),
            isInAllShops = false,
            retailer = Retailer.EMPTY.copy(id = retailerId),
            segment1 = Segment.EMPTY,
            segment2 = Segment.EMPTY,
            segment3 = Segment.EMPTY,
            ecomUrl = "",
            ecomText = ""
        )
    }
}
