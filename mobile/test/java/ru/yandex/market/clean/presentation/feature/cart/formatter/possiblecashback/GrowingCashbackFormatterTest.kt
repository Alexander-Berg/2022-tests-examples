package ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.presentation.feature.cart.vo.PossibleCashbackVo
import ru.yandex.market.clean.presentation.feature.cart.vo.ThresholdStyle
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_GrowingCashbackTestInstance
import ru.yandex.market.domain.money.model.Money

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class GrowingCashbackFormatterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val formatter = GrowingCashbackFormatter(ResourcesManagerImpl(context.resources))

    @Test
    fun `format full progress option`() {
        val option = possibleCashbackOption_GrowingCashbackTestInstance(
            remainingMultiCartTotal = 0,
            minMultiCartTotal = 100,
            amount = 350,
        )
        val hasYandexPlus = true
        val cartTotal = Money.createRub(12345)

        val expected = PossibleCashbackVo(
            progress = 100,
            title = "Ещё :image: :color:${option.amount} баллов:color: за заказ",
            message = "Баллы придут вместе с заказом",
            isThresholdReached = true,
            hasYandexPlus = hasYandexPlus,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.PlusGradient(Gradient.GRADIENT_RADIAL_YANDEX_PLUS_REDESIGN),
            imageType = PossibleCashbackVo.ImageType.PLUS_COLORED,
            cashbackType = PossibleCashbackVo.CashbackType.GROWING,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartTotal.amount.toString(),
                promoKey = option.promoKey,
                cashbackNominal = option.amount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.STANDARD,
        )
        val actual = formatter.format(option, hasYandexPlus, cartTotal)

        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `format half progress option`() {
        val option = possibleCashbackOption_GrowingCashbackTestInstance(
            remainingMultiCartTotal = 50,
            minMultiCartTotal = 100,
            amount = 453,
        )
        val hasYandexPlus = true
        val cartTotal = Money.createRub(23456)

        val expected = PossibleCashbackVo(
            progress = 50,
            title = "Ещё :image: :color:${option.amount} балла:color: за заказ,",
            message = "если добавите товаров ещё на ${option.remainingMultiCartTotal} ₽",
            isThresholdReached = false,
            hasYandexPlus = hasYandexPlus,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.Red,
            imageType = PossibleCashbackVo.ImageType.PLUS_GRAY,
            cashbackType = PossibleCashbackVo.CashbackType.GROWING,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartTotal.amount.toString(),
                promoKey = option.promoKey,
                cashbackNominal = option.amount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.STANDARD,
        )
        val actual = formatter.format(option, hasYandexPlus, cartTotal)

        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `no plus option`() {
        val option = possibleCashbackOption_GrowingCashbackTestInstance(
            remainingMultiCartTotal = 50,
            minMultiCartTotal = 100,
            amount = 453,
        )
        val hasYandexPlus = false
        val cartTotal = Money.createRub(23456)

        val expected = PossibleCashbackVo(
            progress = 50,
            title = ":image: :color:${option.amount} балла:color: за заказ от ${option.minMultiCartTotal} ₽,",
            message = "если подключите Яндекс Плюс",
            isThresholdReached = false,
            hasYandexPlus = hasYandexPlus,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.None,
            imageType = PossibleCashbackVo.ImageType.NO_PLUS,
            cashbackType = PossibleCashbackVo.CashbackType.GROWING,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartTotal.amount.toString(),
                promoKey = option.promoKey,
                cashbackNominal = option.amount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.STANDARD,
        )
        val actual = formatter.format(option, hasYandexPlus, cartTotal)

        Assertions.assertThat(actual).isEqualTo(expected)
    }
}
