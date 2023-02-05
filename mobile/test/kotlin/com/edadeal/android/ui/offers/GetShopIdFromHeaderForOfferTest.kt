package com.edadeal.android.ui.offers

import com.edadeal.android.model.entity.Offer
import com.edadeal.android.model.entity.OfferEntity
import com.edadeal.android.model.entity.Retailer
import com.edadeal.android.model.entity.Shop
import com.edadeal.android.model.toUuidByteString
import com.edadeal.android.ui.offers.bindings.ShopsBinding
import okio.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class GetShopIdFromHeaderForOfferTest(
    private val testCaseMessage: String,
    private val expected: ByteString?,
    private val offer: Offer,
    private val items: List<Any>
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<out Any?>> = listOf(
            arrayOf(
                "Метод должен вернуть null, если оффера нет в списке айтемов",
                null,
                offer,
                listOf(headerWithShop1, dummyOffer, headerWithShop2, dummyOffer)
            ),
            arrayOf(
                "Метод должен вернуть id шопа, который в списке находится перед оффером, переданным в параметр",
                shop2.id,
                offer,
                listOf(headerWithShop1, dummyOffer, headerWithShop2, dummyOffer, offer)
            ),
            arrayOf(
                "Метод должен вернуть null, если в списке нет хидеров с одним шопом",
                null,
                offer,
                listOf(headerWithFewShops, dummyOffer, headerWithoutShops, dummyOffer, offer)
            )
        )

        private val offer = OfferEntity.EMPTY.copy(id = "f0ef256d-393a-48ff-ba58-cd0d43a1b55f".toUuidByteString())
        private val dummyOffer = OfferEntity.EMPTY
        private val shop1 = Shop.EMPTY.copy(id = "258dba22-24cc-441a-9615-1bfa691847fd".toUuidByteString())
        private val shop2 = Shop.EMPTY.copy(id = "94d9bd2d-42c3-11e6-9419-52540010b608".toUuidByteString())
        private val headerWithShop1 = ShopsBinding.Item(Retailer.EMPTY, listOf(shop1))
        private val headerWithShop2 = ShopsBinding.Item(Retailer.EMPTY, listOf(shop2))
        private val headerWithFewShops = ShopsBinding.Item(Retailer.EMPTY, listOf(shop1, shop2))
        private val headerWithoutShops = ShopsBinding.Item(Retailer.EMPTY, emptyList())
    }

    @Test
    fun executeTestCase() {
        assertEquals(expected, getShopIdFromHeaderForOffer(offer, items), testCaseMessage)
    }
}
