package ru.yandex.market.clean.presentation.feature.sis.mainpage.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.shop.shopInfoTestInstance
import ru.yandex.market.clean.domain.model.shop.shopWorkScheduleTestInstance
import ru.yandex.market.clean.presentation.feature.operationalrating.OperationalRatingFormatter
import ru.yandex.market.clean.presentation.feature.sis.mainpage.ShopDeliveryType
import ru.yandex.market.clean.presentation.feature.sis.mainpage.vo.ShopInShopInfoVo
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.formatter.ShopScheduleTimePeriodVoFormatter
import ru.yandex.market.clean.presentation.feature.sis.shopscheduledialog.vo.ShopScheduleTimePeriodVo

class ShopInShopFormatterTest {

    private val shopInfo = shopInfoTestInstance(
        delivery = null
    )

    private val shopScheduleTimePeriodVo = ShopScheduleTimePeriodVo(
        name = null,
        period = "period"
    )

    private val shopDeliveryFormatter: ShopDeliveryFormatter = mock()

    private val shopScheduleTimePeriodVoFormatter = mock<ShopScheduleTimePeriodVoFormatter>() {
        on {
            format(shopInfo.currentWorkSchedule ?: shopWorkScheduleTestInstance())
        } doReturn shopScheduleTimePeriodVo
    }

    private val operationalRatingFormatter = mock<OperationalRatingFormatter>() {
        on {
            format(shopInfo.operationalRating)
        } doReturn null
    }

    private val formatter = ShopInShopFormatter(
        shopScheduleTimePeriodVoFormatter = shopScheduleTimePeriodVoFormatter,
        operationalRatingFormatter = operationalRatingFormatter,
        shopDeliveryFormatter = shopDeliveryFormatter,
    )

    @Test
    fun `Check shop in shop formatter`() {
        val expectedResult = ShopInShopInfoVo(
            title = shopInfo.name,
            imageReference = shopInfo.logo,
            rating = shopInfo.rating,
            operationalRatingVo = null,
            workScheduleVo = shopScheduleTimePeriodVo,
            backgroundColor = null,
            deliveryTime = null,
            deliveryTimeMinutes = null
        )

        val actualResult = formatter.format(shopInfo, ShopDeliveryType.MAIN)
        assertThat(actualResult).isEqualTo(expectedResult)
    }
}