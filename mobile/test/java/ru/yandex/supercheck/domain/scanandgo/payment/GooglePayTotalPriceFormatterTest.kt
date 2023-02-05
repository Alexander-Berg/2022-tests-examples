package ru.yandex.supercheck.domain.scanandgo.payment

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.domain.scanandgo.payment.googlepay.GooglePayTotalPriceFormatter

@RunWith(MockitoJUnitRunner::class)
class GooglePayTotalPriceFormatterTest {

    companion object {

        private const val TOTAL_PRICE_200_ROUBLES = 20000L
        private const val TOTAL_PRICE_200_ROUBLES_FORMATTED = "200"

        private const val TOTAL_PRICE_200_ROUBLES_89_KOPECKS = 20089L
        private const val TOTAL_PRICE_200_ROUBLES_89_KOPECKS_FORMATTED = "200.89"

        private const val TOTAL_PRICE_200_ROUBLES_09_KOPECKS = 20009L
        private const val TOTAL_PRICE_200_ROUBLES_09_KOPECKS_FORMATTED = "200.09"

    }

    private val googlePayTotalPriceFormatter = GooglePayTotalPriceFormatter()

    @Test
    fun testWithoutKopecks() {
        assertEquals(TOTAL_PRICE_200_ROUBLES, TOTAL_PRICE_200_ROUBLES_FORMATTED)
    }

    @Test
    fun testWithKopecks() {
        assertEquals(
            TOTAL_PRICE_200_ROUBLES_89_KOPECKS,
            TOTAL_PRICE_200_ROUBLES_89_KOPECKS_FORMATTED
        )
    }

    @Test
    fun testWithKopecksAndLeadingZero() {
        assertEquals(
            TOTAL_PRICE_200_ROUBLES_09_KOPECKS,
            TOTAL_PRICE_200_ROUBLES_09_KOPECKS_FORMATTED
        )
    }

    private fun assertEquals(totalPrice: Long, expectedFormattedTotalPrice: String) {
        assertEquals(expectedFormattedTotalPrice, googlePayTotalPriceFormatter.format(totalPrice))
    }


}