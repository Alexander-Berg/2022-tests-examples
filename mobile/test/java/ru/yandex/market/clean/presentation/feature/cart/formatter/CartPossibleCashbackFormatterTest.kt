package ru.yandex.market.clean.presentation.feature.cart.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.actualizedCartTestInstance
import ru.yandex.market.clean.domain.model.cart.cartPlusInfo_LoginTestInstance
import ru.yandex.market.clean.domain.model.cart.cartValidationResultTestInstance
import ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback.CartPossibleCashbackFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback.GrowingCashbackFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback.PaymentSystemCashbackFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback.WelcomeCashbackFormatter
import ru.yandex.market.clean.presentation.feature.cart.formatter.possiblecashback.YandexCardCashbackFormatter
import ru.yandex.market.clean.presentation.feature.cart.vo.possibleCashbackVoTestInstance
import ru.yandex.market.data.order.PaymentOptionHiddenReason
import ru.yandex.market.data.order.options.OrderSummary
import ru.yandex.market.data.order.options.PaymentOption
import ru.yandex.market.data.order.options.deliveryOptionTestInstance
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_GrowingCashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_PaymentSystemCashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_WelcomeCashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_YandexCardCashbackTestInstance
import ru.yandex.market.domain.money.model.Money

class CartPossibleCashbackFormatterTest {

    private val growingCashbackVo = possibleCashbackVoTestInstance(title = "growingCashback")
    private val welcomeCashbackVo = possibleCashbackVoTestInstance(title = "welcomeCashback")
    private val paymentSystemCashbackVo = possibleCashbackVoTestInstance(title = "paymentSystemCashback")
    private val yandexCardCashbackVo = possibleCashbackVoTestInstance(title = "yandexCardCashback")

    private val paymentSystemCashbackFormatter = mock<PaymentSystemCashbackFormatter> {
        on { format(any(), any(), any(), any()) } doReturn paymentSystemCashbackVo
    }
    private val welcomeCashbackFormatter = mock<WelcomeCashbackFormatter> {
        on { format(any(), any(), any()) } doReturn welcomeCashbackVo
    }
    private val growingCashbackFormatter = mock<GrowingCashbackFormatter> {
        on { format(any(), any(), any()) } doReturn growingCashbackVo
    }
    private val yandexCardCashbackFormatter = mock<YandexCardCashbackFormatter> {
        on { format(any(), any(), any()) } doReturn yandexCardCashbackVo
    }

    private val formatter = CartPossibleCashbackFormatter(
        paymentSystemCashbackFormatter,
        welcomeCashbackFormatter,
        growingCashbackFormatter,
        yandexCardCashbackFormatter,
    )

    @Test
    fun `return growingCashbackVo when GrowingCashback has max priority to show`() {
        val vo = formatter.format(
            cartValidationResultTestInstance(
                cashback = cashbackTestInstance(
                    possibleCashbackOptions = listOf(
                        possibleCashbackOption_PaymentSystemCashbackTestInstance(),
                        possibleCashbackOption_WelcomeCashbackTestInstance(),
                        possibleCashbackOption_GrowingCashbackTestInstance(agitationPriority = 100),
                        possibleCashbackOption_YandexCardCashbackTestInstance()
                    )
                )
            ), cartPlusInfo = cartPlusInfo_LoginTestInstance()
        )
        assertThat(vo).isEqualTo(growingCashbackVo)
    }

    @Test
    fun `return welcomeCashbackVo when WelcomeCashback has max priority to show`() {
        val vo = formatter.format(
            cartValidationResultTestInstance(
                cashback = cashbackTestInstance(
                    possibleCashbackOptions = listOf(
                        possibleCashbackOption_PaymentSystemCashbackTestInstance(),
                        possibleCashbackOption_WelcomeCashbackTestInstance(agitationPriority = 100),
                        possibleCashbackOption_GrowingCashbackTestInstance(),
                        possibleCashbackOption_YandexCardCashbackTestInstance()
                    )
                )
            ), cartPlusInfo = cartPlusInfo_LoginTestInstance()
        )
        assertThat(vo).isEqualTo(welcomeCashbackVo)
    }

    @Test
    fun `return paymentSystemCashbackVo when PaymentSystemCashback has max priority to show`() {
        val vo = formatter.format(
            cartValidationResultTestInstance(
                cashback = cashbackTestInstance(
                    possibleCashbackOptions = listOf(
                        possibleCashbackOption_WelcomeCashbackTestInstance(),
                        possibleCashbackOption_GrowingCashbackTestInstance(),
                        possibleCashbackOption_YandexCardCashbackTestInstance(),
                        possibleCashbackOption_PaymentSystemCashbackTestInstance(agitationPriority = 100),
                    )
                )
            ), cartPlusInfo = cartPlusInfo_LoginTestInstance()
        )
        assertThat(vo).isEqualTo(paymentSystemCashbackVo)
    }

    @Test
    fun `return yandexCardCashbackVo when YandexCardCashback has max priority to show`() {
        val vo = formatter.format(
            cartValidationResultTestInstance(
                cashback = cashbackTestInstance(
                    possibleCashbackOptions = listOf(
                        possibleCashbackOption_WelcomeCashbackTestInstance(),
                        possibleCashbackOption_GrowingCashbackTestInstance(),
                        possibleCashbackOption_YandexCardCashbackTestInstance(agitationPriority = 100),
                        possibleCashbackOption_PaymentSystemCashbackTestInstance(),
                    )
                )
            ), cartPlusInfo = cartPlusInfo_LoginTestInstance()
        )
        assertThat(vo).isEqualTo(yandexCardCashbackVo)
    }

    @Test
    fun `return null if has growing cashback option and growingCashback disabled`() {
        val vo = formatter.format(
            cartValidationResultTestInstance(
                cashback = cashbackTestInstance(
                    possibleCashbackOptions = listOf(
                        possibleCashbackOption_GrowingCashbackTestInstance()
                    )
                )
            ), cartPlusInfo = cartPlusInfo_LoginTestInstance(
                isGrowingCashbackEnabled = false
            )
        )
        assertThat(vo).isNull()
    }

    @Test
    fun `return null if has payment system cashback option and paymentSystemCashback disabled`() {
        val vo = formatter.format(
            cartValidationResultTestInstance(
                cashback = cashbackTestInstance(
                    possibleCashbackOptions = listOf(
                        possibleCashbackOption_PaymentSystemCashbackTestInstance()
                    )
                )
            ), cartPlusInfo = cartPlusInfo_LoginTestInstance(
                isMastercardPromoAvailable = false
            )
        )
        assertThat(vo).isNull()
    }

    @Test
    fun `check growing cashback arguments`() {
        val cartPrice = Money.createRub(123)
        val option = possibleCashbackOption_GrowingCashbackTestInstance()
        formatter.format(
            cartValidationResult = cartValidationResultTestInstance(
                cart = actualizedCartTestInstance(
                    summary = OrderSummary.testInstance().copy(totalPrice = cartPrice)
                ),
                cashback = cashbackTestInstance(possibleCashbackOptions = listOf(option))
            ),
            cartPlusInfo = cartPlusInfo_LoginTestInstance(hasYandexPlus = false)
        )
        verify(growingCashbackFormatter).format(option, false, cartPrice)
    }

    @Test
    fun `check welcome cashback arguments`() {
        val cartPrice = Money.createRub(234)
        val option = possibleCashbackOption_WelcomeCashbackTestInstance()
        formatter.format(
            cartValidationResult = cartValidationResultTestInstance(
                cart = actualizedCartTestInstance(
                    summary = OrderSummary.testInstance().copy(totalPrice = cartPrice)
                ),
                cashback = cashbackTestInstance(possibleCashbackOptions = listOf(option))
            ),
            cartPlusInfo = cartPlusInfo_LoginTestInstance(hasYandexPlus = true)
        )
        verify(welcomeCashbackFormatter).format(option, true, cartPrice)
    }

    @Test
    fun `check payment cashback arguments`() {
        val cartPrice = Money.createRub(654)
        val option = possibleCashbackOption_PaymentSystemCashbackTestInstance()
        val paymentOptions = setOf(
            PaymentMethod.APPLE_PAY,
            PaymentMethod.GOOGLE_PAY,
            PaymentMethod.CARD_ON_DELIVERY,
            PaymentMethod.YANDEX
        )
        formatter.format(
            cartValidationResult = cartValidationResultTestInstance(
                cart = actualizedCartTestInstance(
                    summary = OrderSummary.testInstance().copy(totalPrice = cartPrice),
                ),
                cashback = cashbackTestInstance(possibleCashbackOptions = listOf(option)),
                deliveryOptionsByPack = mapOf(
                    "deliveryOptionsByPack1" to listOf(
                        deliveryOptionTestInstance(
                            paymentMethods = listOf(
                                PaymentOption(PaymentMethod.CASH_ON_DELIVERY, PaymentOptionHiddenReason.MUID),
                                PaymentOption(PaymentMethod.CARD_ON_DELIVERY, PaymentOptionHiddenReason.MUID),
                                PaymentOption(PaymentMethod.APPLE_PAY, null),
                                PaymentOption(PaymentMethod.GOOGLE_PAY, null)
                            )
                        )
                    ),
                    "deliveryOptionsByPack2" to listOf(
                        deliveryOptionTestInstance(
                            paymentMethods = listOf(
                                PaymentOption(PaymentMethod.CARD_ON_DELIVERY, null),
                                PaymentOption(PaymentMethod.YANDEX, null),
                                PaymentOption(PaymentMethod.GOOGLE_PAY, PaymentOptionHiddenReason.MUID)
                            )
                        )
                    )
                )
            ),
            cartPlusInfo = cartPlusInfo_LoginTestInstance()
        )
        verify(paymentSystemCashbackFormatter).format(option, true, cartPrice, paymentOptions)
    }
}
