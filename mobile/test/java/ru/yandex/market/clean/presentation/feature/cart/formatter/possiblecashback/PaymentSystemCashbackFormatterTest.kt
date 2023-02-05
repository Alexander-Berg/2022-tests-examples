package ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.cart.vo.PossibleCashbackVo
import ru.yandex.market.clean.presentation.feature.cart.vo.ThresholdStyle
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_PaymentSystemCashbackTestInstance
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.payment.model.PaymentSystem
import java.math.BigDecimal

class PaymentSystemCashbackFormatterTest {

    private val resourcesManager = mock<ResourcesManager>()

    private val formatter = PaymentSystemCashbackFormatter(resourcesManager)


    @Test
    fun `format payment system cashback option`() {
        val option = possibleCashbackOption_PaymentSystemCashbackTestInstance(
            paymentSystem = PaymentSystem.MASTERCARD,
            percentValue = 5,
            cashbackAmount = BigDecimal(123)
        )
        val cartPrice = Money.Companion.createRub(12345)
        val expectedTitle = "title"
        val expectedMessage = "message"
        whenever(
            resourcesManager.getFormattedQuantityString(
                R.plurals.cart_plus_info_mastercard_title,
                option.cashbackAmount.toInt(),
                option.cashbackAmount.toInt()
            )
        ) doReturn expectedTitle

        whenever(
            resourcesManager.getFormattedString(
                R.string.cart_plus_info_mastercard_subtitle,
                option.percentValue
            )
        ) doReturn expectedMessage

        val actual = formatter.format(
            paymentSystemCashbackOption = option,
            hasYandexPlus = true,
            cartTotal = cartPrice,
            possiblePaymentMethods = setOf(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.YANDEX)
        )

        val expected = PossibleCashbackVo(
            progress = 0,
            title = expectedTitle,
            message = expectedMessage,
            isThresholdReached = false,
            hasYandexPlus = true,
            textGradient = Gradient.PLUS_GRADIENT_2_COLORS,
            thresholdStyle = ThresholdStyle.Red,
            imageType = PossibleCashbackVo.ImageType.MASTERCARD,
            cashbackType = PossibleCashbackVo.CashbackType.MASTERCARD,
            additionalAnalyticsInfo = PossibleCashbackVo.AdditionalAnalyticsInfo(
                totalCartPrice = cartPrice.amount.toString(),
                promoKey = null,
                cashbackNominal = option.cashbackAmount.toString()
            ),
            thresholdPadding = PossibleCashbackVo.ThresholdPadding.STANDARD,
        )

        assertThat(actual).isEqualTo(expected)
    }


    @Test
    fun `return null if only pay by cash available`() {
        val vo = formatter.format(
            paymentSystemCashbackOption = possibleCashbackOption_PaymentSystemCashbackTestInstance(),
            hasYandexPlus = true,
            cartTotal = Money.Companion.createRub(123),
            possiblePaymentMethods = setOf(PaymentMethod.CASH_ON_DELIVERY)
        )

        assertThat(vo).isNull()
    }

}
