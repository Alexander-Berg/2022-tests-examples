package ru.yandex.market.clean.presentation.feature.trust.vo.popover.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.shop.shopInfoTestInstance
import ru.yandex.market.clean.presentation.feature.trust.vo.popover.TrustShopRatingVo

class TrustShopRatingFormatterTest {

    private val formatter = TrustShopRatingFormatter()

    @Test
    fun `Test format rating per three months null`() {
        val shop = shopInfoTestInstance(rating = null)

        val result = formatter.format(shop)

        assertThat(result.ignoreError()).isNull()
    }

    @Test
    fun `Test format grades per three months null`() {
        val shop = shopInfoTestInstance(gradesPerThreeMonths = null)

        val result = formatter.format(shop)

        assertThat(result.ignoreError()).isNull()
    }

    @Test
    fun `Test default format`() {
        val shop = shopInfoTestInstance(
            gradesPerThreeMonths = MARKS_PER_THREE_MONTHS,
            gradesPerAllTime = MARKS_FOR_ALL_TIME,
            rating = RATING
        )

        val expected = TrustShopRatingVo(
            marksForAllTime = MARKS_FOR_ALL_TIME.toString(),
            marksPerThreeMonths = MARKS_PER_THREE_MONTHS.toString(),
            totalRating = "%.1f".format(RATING)
        )

        val actual = formatter.format(shop)

        assertThat(actual.throwError()).isEqualTo(expected)
    }

    private companion object {
        const val MARKS_FOR_ALL_TIME = 1000
        const val MARKS_PER_THREE_MONTHS = 5000
        const val RATING = 4.123
    }
}