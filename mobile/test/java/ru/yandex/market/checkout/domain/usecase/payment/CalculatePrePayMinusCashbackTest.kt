package ru.yandex.market.checkout.domain.usecase.payment

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.domain.cashback.model.CashBackBalance
import ru.yandex.market.domain.cashback.model.CashbackOption
import ru.yandex.market.domain.cashback.model.CashbackOptionType
import ru.yandex.market.domain.cashback.model.CashbackOptions
import ru.yandex.market.domain.cashback.model.CashbackRestrictionReason
import ru.yandex.market.domain.cashback.model.cashbackOptionProfileTestInstance
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.domain.cashback.model.possibleCashbackOption_WelcomeCashbackTestInstance
import ru.yandex.market.domain.payment.model.PaymentMethod
import java.math.BigDecimal

class CalculatePrePayMinusCashbackTest {

    val cashback = cashbackTestInstance(
        cashBackBalance = CashBackBalance(BigDecimal(1000), 0),
        selectedOption = CashbackOptionType.SPEND,
        cashbackOptions = CashbackOptions(
            spendOption = CashbackOption(
                value = BigDecimal(100),
                restrictionReason = null,
                promos = emptyList(),
                details = null,
            ),
            emitOption = CashbackOption(
                value = BigDecimal(0),
                restrictionReason = CashbackRestrictionReason.NOT_SUITABLE_CATEGORY,
                promos = emptyList(),
                details = null,
            )
        ),
        cashbackOptionProfiles = listOf(
            cashbackOptionProfileTestInstance(
                cashbackOptions = CashbackOptions(
                    spendOption = CashbackOption(
                        value = BigDecimal(100),
                        restrictionReason = null,
                        promos = emptyList(),
                        details = null,
                    ),
                    emitOption = CashbackOption(
                        value = BigDecimal(0),
                        restrictionReason = CashbackRestrictionReason.UNKNOWN,
                        promos = emptyList(),
                        details = null,
                    )
                ),
                availablePaymentMethods = listOf(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX)
            ),
            cashbackOptionProfileTestInstance(
                cashbackOptions = CashbackOptions(
                    spendOption = CashbackOption(
                        value = BigDecimal(0),
                        restrictionReason = CashbackRestrictionReason.UNKNOWN,
                        promos = emptyList(),
                        details = null,
                    ),
                    emitOption = CashbackOption(
                        value = BigDecimal(200),
                        restrictionReason = null,
                        promos = emptyList(),
                        details = null,
                    )
                ),
                availablePaymentMethods = emptyList()
            )
        ),
        possibleCashbackOptions = listOf(possibleCashbackOption_WelcomeCashbackTestInstance())
    )

    @Test
    fun `прямой сценарий - списываем с плюсов`() {
        val sum = BigDecimal(1000)
        val res = calculatePrePayMinusCashback(prePayTotalSum = sum, cashback = cashback)
        Assertions.assertThat(res).isEqualTo(BigDecimal(900))
    }

    @Test
    fun `плюсы не выбранны`() {
        val sum = BigDecimal(1000)
        val cashback = cashback.copy(selectedOption = CashbackOptionType.EMIT)
        val res = calculatePrePayMinusCashback(prePayTotalSum = sum, cashback = cashback)
        Assertions.assertThat(res).isEqualTo(BigDecimal(1000))
    }

    @Test
    fun `попытка списать больше плюсов чем стоит товар`() {
        val sum = BigDecimal(10)
        val res = calculatePrePayMinusCashback(prePayTotalSum = sum, cashback = cashback)
        Assertions.assertThat(res).isEqualTo(BigDecimal(10))
    }
}