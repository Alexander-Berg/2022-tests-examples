package ru.yandex.market.clean.data.fapi.contract

import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.clean.data.request.DateInStockProductRequest

class DateInStockProductsContractTest {

    @Test(expected = IllegalArgumentException::class)
    fun `should throw error when all ids are null`() {
        DateInStockProductRequest(
            skuIds = null,
            productIds = null,
            offerIds = null,
            regionId = DUMMY_REGION_ID
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw error when all ids are empty`() {
        DateInStockProductRequest(
            skuIds = emptySet(),
            productIds = emptySet(),
            offerIds = emptySet(),
            regionId = DUMMY_REGION_ID
        )
    }

    @Test
    fun `should create request when at least one skuId exists `() {
        val expected = DateInStockProductRequest(
            skuIds = setOf(DUMMY_SKU_ID),
            productIds = null,
            offerIds = null,
            regionId = DUMMY_REGION_ID
        )
        assertThat(expected).isInstanceOf(DateInStockProductRequest::class.java)
    }

    @Test
    fun `should create request when at least one productId exists `() {
        val expected = DateInStockProductRequest(
            skuIds = null,
            productIds = setOf(DUMMY_PRODUCT_ID),
            offerIds = null,
            regionId = DUMMY_REGION_ID
        )
        assertThat(expected).isInstanceOf(DateInStockProductRequest::class.java)
    }

    @Test
    fun `should create request when at least one offerId exists `() {
        val expected = DateInStockProductRequest(
            skuIds = null,
            productIds = null,
            offerIds = setOf(DUMMY_OFFER_ID),
            regionId = DUMMY_REGION_ID
        )
        assertThat(expected).isInstanceOf(DateInStockProductRequest::class.java)
    }

    private companion object {
        const val DUMMY_SKU_ID = "DUMMY_SKU_ID"
        const val DUMMY_PRODUCT_ID = "DUMMY_PRODUCT_ID"
        const val DUMMY_OFFER_ID = "DUMMY_OFFER_ID"
        const val DUMMY_REGION_ID = 0
    }
}
