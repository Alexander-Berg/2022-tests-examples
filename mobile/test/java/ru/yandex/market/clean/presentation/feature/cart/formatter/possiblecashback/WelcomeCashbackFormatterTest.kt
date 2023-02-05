package ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.cart.vo.PossibleCashbackVo
import ru.yandex.market.clean.presentation.feature.cart.vo.ThresholdStyle
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_WelcomeCashbackTestInstance
import ru.yandex.market.domain.money.model.Money

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class WelcomeCashbackFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val formatter = WelcomeCashbackFormatter(ResourcesManagerImpl(context.resources))

    @Test
    fun `format full progress option`() {
        val welcomeCashback = possibleCashbackOption_WelcomeCashbackTestInstance(
            remainingMultiCartTotal = 0,
            minMultiCartTotal = 100,
            amount = 123
        )
        val hasYandexPlus = true
        val cartTotal = Money.createRub(12345)

        val expected = PossibleCashbackVo(
            progress = 100,
            title = "Ещё :image: :color:${welcomeCashback.amount} баллов:color: за 1-й заказ",
            message = "Баллы придут вместе с заказом",
            isThresholdReached = true,
            hasYandexPlus = hasYandexPlus,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.PlusGradient(Gradient.GRADIENT_RADIAL_YANDEX_PLUS_REDESIGN),
            imageType = PossibleCashbackVo.ImageType.PLUS_COLORED,
            cashbackType = PossibleCashbackVo.CashbackType.WELCOME,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartTotal.amount.toString(),
                promoKey = welcomeCashback.promoKey,
                cashbackNominal = welcomeCashback.amount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.STANDARD,
        )
        val actual = formatter.format(welcomeCashback, hasYandexPlus, cartTotal)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format half progress option`() {
        val welcomeCashback = possibleCashbackOption_WelcomeCashbackTestInstance(
            remainingMultiCartTotal = 50,
            minMultiCartTotal = 100,
            amount = 234
        )
        val hasYandexPlus = false
        val cartTotal = Money.createRub(23456)

        val expected = PossibleCashbackVo(
            progress = 50,
            title = "Ещё :image: :color:${welcomeCashback.amount} баллов:color: за 1-й заказ,",
            message = "если добавите товаров ещё на ${welcomeCashback.remainingMultiCartTotal} ₽",
            isThresholdReached = false,
            hasYandexPlus = hasYandexPlus,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.Red,
            imageType = PossibleCashbackVo.ImageType.PLUS_GRAY,
            cashbackType = PossibleCashbackVo.CashbackType.WELCOME,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartTotal.amount.toString(),
                promoKey = welcomeCashback.promoKey,
                cashbackNominal = welcomeCashback.amount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.STANDARD,
        )
        val actual = formatter.format(welcomeCashback, hasYandexPlus, cartTotal)

        assertThat(actual).isEqualTo(expected)
    }
}
