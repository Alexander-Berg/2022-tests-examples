package ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.cart.vo.PossibleCashbackVo
import ru.yandex.market.clean.presentation.feature.cart.vo.ThresholdStyle
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_YandexCardCashbackTestInstance
import ru.yandex.market.domain.money.model.Money
import java.math.BigDecimal

class YandexCardCashbackFormatterTest {

    private val resourcesManager = mock<ResourcesManager>()

    private val formatter = YandexCardCashbackFormatter(resourcesManager)


    @Test
    fun `format payment system cashback option`() {
        val option = possibleCashbackOption_YandexCardCashbackTestInstance(
            percentValue = 15,
            cashbackAmount = BigDecimal(456),
            maxOrderTotal = BigDecimal(15000),
            promoKey = "yandexCard"
        )
        val cartPrice = Money.Companion.createRub(12345)
        val expectedTitle = "title"
        val expectedMessage = "message"
        whenever(
            resourcesManager.getFormattedString(
                R.string.cart_plus_info_yandex_card_title,
                option.cashbackAmount.toInt(),
            )
        ) doReturn expectedTitle

        whenever(
            resourcesManager.getFormattedString(
                R.string.cart_plus_info_yandex_card_subtitle,
                option.percentValue
            )
        ) doReturn expectedMessage

        val actual = formatter.format(
            yandexCardCashbackOption = option,
            hasYandexPlus = true,
            cartTotal = cartPrice
        )

        val expected = PossibleCashbackVo(
            progress = 0,
            title = expectedTitle,
            message = expectedMessage,
            isThresholdReached = false,
            hasYandexPlus = true,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.None,
            imageType = PossibleCashbackVo.ImageType.YANDEX,
            cashbackType = PossibleCashbackVo.CashbackType.YANDEX_CARD,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartPrice.amount.toString(),
                promoKey = option.promoKey,
                cashbackNominal = option.cashbackAmount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.NONE,
        )

        Assertions.assertThat(actual).isEqualTo(expected)
    }
}
