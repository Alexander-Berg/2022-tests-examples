package ru.yandex.market.data.fintech.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.common.banksdk.api.YandexBankMoney
import ru.yandex.market.common.banksdk.api.YandexBankSdkState
import ru.yandex.market.data.fintech.health.BankSdkMapperHeath
import ru.yandex.market.domain.fintech.data.YandexBankAccountState
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import java.math.BigDecimal

@RunWith(Enclosed::class)
class YandexBankAccountStateMapperTest {

    class HeathTest {
        private val heathFacade = mock<BankSdkMapperHeath>()
        private val yandexBankCurrencyMapper = mock<YandexBankCurrencyMapper> {
            on { mapFromBankCurrency(any()) } doReturn Currency.UNKNOWN
        }
        private val mapper = YandexBankAccountStateMapper(heathFacade, yandexBankCurrencyMapper)

        @Test
        fun `send health on unknown bank currency`() {
            val bankCurrency = "bankCurrency"
            mapper.map(
                YandexBankSdkState.Normal(
                    balance = YandexBankMoney(
                        BigDecimal.TEN,
                        currency = bankCurrency,
                        formattedAmount = "formattedAmount"
                    ),
                    transactions = emptyList()
                )
            )
            verify(heathFacade).sendUnknownBalanceCurrency(bankCurrency)
        }
    }

    @RunWith(Parameterized::class)
    class MappingTest(
        private val input: YandexBankSdkState,
        private val expectedOutput: YandexBankAccountState
    ) {

        private val mapper = YandexBankAccountStateMapper(mock(), YandexBankCurrencyMapper())

        @Test
        fun map() {
            assertThat(mapper.map(input)).isEqualTo(expectedOutput)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                //0
                arrayOf(
                    YandexBankSdkState.Unavailable,
                    YandexBankAccountState.Unavailable("SDK not initialized properly")
                ),
                //1
                arrayOf(
                    YandexBankSdkState.Restricted,
                    YandexBankAccountState.Unavailable("SDK usages restricted")
                ),
                //2
                arrayOf(
                    YandexBankSdkState.NoBankAccount,
                    YandexBankAccountState.NotCreated
                ),
                //3
                arrayOf(
                    YandexBankSdkState.Unauthenticated,
                    YandexBankAccountState.Unauthenticated
                ),
                //4
                arrayOf(
                    YandexBankSdkState.Unauthorized,
                    YandexBankAccountState.Limited
                ),
                //5
                arrayOf(
                    YandexBankSdkState.Normal(
                        balance = null,
                        transactions = emptyList()
                    ),
                    YandexBankAccountState.Available(
                        balance = null
                    )
                ),
                //6
                arrayOf(
                    YandexBankSdkState.Normal(
                        balance = YandexBankMoney(BigDecimal(123), currency = "rur", formattedAmount = "123 rub"),
                        transactions = emptyList()
                    ),
                    YandexBankAccountState.Available(
                        balance = Money(BigDecimal(123), Currency.RUR)
                    )
                ),
                //7
                arrayOf(
                    YandexBankSdkState.Normal(
                        balance = YandexBankMoney(BigDecimal(123), currency = "rub", formattedAmount = "123 rub"),
                        transactions = emptyList()
                    ),
                    YandexBankAccountState.Available(
                        balance = Money(BigDecimal(123), Currency.RUR)
                    )
                ),
                //8
                arrayOf(
                    YandexBankSdkState.Normal(
                        balance = YandexBankMoney(BigDecimal(123), currency = "RUB", formattedAmount = "123 rub"),
                        transactions = emptyList()
                    ),
                    YandexBankAccountState.Available(
                        balance = Money(BigDecimal(123), Currency.RUR)
                    )
                ),
                //9
                arrayOf(
                    YandexBankSdkState.Normal(
                        balance = YandexBankMoney(BigDecimal(224), currency = "kzt", formattedAmount = "123 kzt"),
                        transactions = emptyList()
                    ),
                    YandexBankAccountState.Available(
                        balance = Money(BigDecimal(224), Currency.KZT)
                    )
                ),
                //10
                arrayOf(
                    YandexBankSdkState.Normal(
                        balance = YandexBankMoney(
                            BigDecimal.TEN,
                            currency = "попугаи",
                            formattedAmount = "123 попугая"
                        ),
                        transactions = emptyList()
                    ),
                    YandexBankAccountState.Available(
                        balance = Money(BigDecimal.TEN, Currency.UNKNOWN)
                    )
                )
            )
        }
    }

}
