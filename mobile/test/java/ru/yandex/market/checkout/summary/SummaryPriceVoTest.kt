package ru.yandex.market.checkout.summary

import org.junit.Test

class SummaryPriceVoTest {

    @Test
    fun `Creating empty instance not throwing exception`() {
        SummaryPriceVo(isCreditVisible = false, productsCount = 1)
    }
}