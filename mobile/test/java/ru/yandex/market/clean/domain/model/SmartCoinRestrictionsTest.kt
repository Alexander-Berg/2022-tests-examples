package ru.yandex.market.clean.domain.model

import org.junit.Test

class SmartCoinRestrictionsTest {

    @Test(expected = IllegalStateException::class)
    fun `Throws exception when both sku and category ids are empty`() {
        SmartCoinRestrictions.builder()
            .categoryId(null)
            .skuId("")
            .build()
    }
}