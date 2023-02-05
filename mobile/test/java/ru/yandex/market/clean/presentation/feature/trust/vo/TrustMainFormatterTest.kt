package ru.yandex.market.clean.presentation.feature.trust.vo

import org.junit.Test

import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.model.supplierTestInstance
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import ru.yandex.market.clean.domain.model.operationalRatingTestInstance
import ru.yandex.market.domain.media.model.EmptyImageReference
import ru.yandex.market.feature.productsnippets.ui.offer.trust.vo.TrustMainVo

class TrustMainFormatterTest {

    private val formatter = TrustMainFormatter()

    private val configurator = RecursiveComparisonConfiguration.builder().build()

    @Test
    fun `Test format offer with empty supplier name`() {
        val offer = productOfferTestInstance(offer = offerTestInstance(supplier = supplierTestInstance(name = "")))

        val result = formatter.format(offer)

        assertThat(result).isNull()
    }

    @Test
    fun `Test format default offer with all chips`() {
        val offer = productOfferTestInstance(
            offer = offerTestInstance(
                supplier = supplierTestInstance(
                    operationalRating = operationalRatingTestInstance(
                        total = 95f
                    ),
                    newGradesCount3M = GRADES_PER_THREE_MONTHS,
                    ratingToShow = RATING
                )
            )
        )

        val expected = TrustMainVo(
            chips = setOf(
                TrustMainVo.TrustChip.OFFICIAL_SHOP,
                TrustMainVo.TrustChip.GOOD_ORDERS_RATING,
                TrustMainVo.TrustChip.REPRESENTATIVE_SHOP,
            ),
            isYandexMarketShop = false,
            ratingPerThreeMonths = TrustMainVo.Rating("%.1f".format(RATING), "$GRADES_PER_THREE_MONTHS"),
            shopImage = EmptyImageReference(),
            shopName = offer.supplierName,
            vendorName = offer.vendor?.name ?: ""
        )

        val actual = formatter.format(offer)

        assertThat(actual).usingRecursiveComparison(configurator).isEqualTo(expected)
    }

    @Test
    fun `Test format yandex market offer with all chips`() {
        val offer = productOfferTestInstance(
            offer = offerTestInstance(
                supplier = supplierTestInstance(
                    id = YANDEX_MARKET_SUPPLIER_ID,
                    operationalRating = operationalRatingTestInstance(
                        total = 95f
                    ),
                    newGradesCount3M = GRADES_PER_THREE_MONTHS,
                    ratingToShow = RATING
                ),
                shopId = YANDEX_MARKET_SHOP_ID
            )
        )

        val expected = TrustMainVo(
            chips = setOf(TrustMainVo.TrustChip.REPRESENTATIVE_SHOP),
            isYandexMarketShop = true,
            ratingPerThreeMonths = null,
            shopImage = EmptyImageReference(),
            shopName = offer.supplierName,
            vendorName = offer.vendor?.name ?: ""
        )

        val actual = formatter.format(offer)

        assertThat(actual).usingRecursiveComparison(configurator).isEqualTo(expected)
    }

    private companion object {
        const val GRADES_PER_THREE_MONTHS = 10
        const val RATING = 4.123
        const val YANDEX_MARKET_SUPPLIER_ID = 465852L
        const val YANDEX_MARKET_SHOP_ID = 431782L
    }
}