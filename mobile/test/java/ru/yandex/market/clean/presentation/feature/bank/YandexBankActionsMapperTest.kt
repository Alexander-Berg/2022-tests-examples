package ru.yandex.market.clean.presentation.feature.bank

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.domain.fintech.data.YandexBankAccountState
import ru.yandex.market.domain.money.model.Money

@RunWith(Parameterized::class)
class YandexBankActionsMapperTest(
    private val oldState: YandexBankAccountState,
    private val newState: YandexBankAccountState,
    private val expectedResult: YandexBankResult.Action?,
    stepDescription: String,
) {

    private val mapper = YandexBankActionsMapper()

    @Test
    fun map() {
        assertThat(mapper.mapFromStateChanges(oldState, newState)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index} {3}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                YandexBankAccountState.Unauthenticated,
                YandexBankAccountState.Available(null),
                YandexBankResult.Action.AuthorizationSuccess,
                "expected AuthorizationSuccess from Unauthenticated -> Available"
            ),
            //1
            arrayOf(
                YandexBankAccountState.Unauthenticated,
                YandexBankAccountState.NotCreated,
                YandexBankResult.Action.AuthorizationSuccess,
                "expected AuthorizationSuccess from Unauthenticated -> NotCreated"
            ),
            //2
            arrayOf(
                YandexBankAccountState.Unauthenticated,
                YandexBankAccountState.Limited,
                YandexBankResult.Action.AuthorizationSuccess,
                "expected AuthorizationSuccess from Unauthenticated -> Limited"
            ),
            //3
            arrayOf(
                YandexBankAccountState.NotCreated,
                YandexBankAccountState.Available(null),
                YandexBankResult.Action.AccountCreated,
                "expected AccountCreated from NotCreated -> Available"
            ),
            //4
            arrayOf(
                YandexBankAccountState.NotCreated,
                YandexBankAccountState.Limited,
                YandexBankResult.Action.AccountCreated,
                "expected AccountCreated from NotCreated -> Limited"
            ),
            //5
            arrayOf(
                YandexBankAccountState.Available(Money.createRub(10)),
                YandexBankAccountState.Available(Money.createRub(20)),
                YandexBankResult.Action.WalletTopUp,
                "expected WalletTopUp from Available(balance = 10) -> Available(balance = 20)"
            ),
            //6
            arrayOf(
                YandexBankAccountState.Available(Money.createRub(20)),
                YandexBankAccountState.Available(Money.createRub(10)),
                null as YandexBankResult.Action?,
                "expected null from Available(balance = 20) -> Available(balance = 10)"
            ),
            //7
            arrayOf(
                YandexBankAccountState.Available(null),
                YandexBankAccountState.Available(Money.createRub(10)),
                null as YandexBankResult.Action?,
                "expected null from Available(balance = null) -> Available(balance = 10)"
            ),
            //8
            arrayOf(
                YandexBankAccountState.Available(Money.createRub(20)),
                YandexBankAccountState.Available(null),
                null as YandexBankResult.Action?,
                "expected null from Available(balance = 20) -> Available(balance = null)"
            ),
            //9
            arrayOf(
                YandexBankAccountState.Unavailable("test"),
                YandexBankAccountState.Available(null),
                null as YandexBankResult.Action?,
                "expected null from Unavailable -> Available(balance = null)"
            ),
            //10
            arrayOf(
                YandexBankAccountState.Limited,
                YandexBankAccountState.Unavailable("test"),
                null as YandexBankResult.Action?,
                "expected null from Limited -> Unavailable"
            )
        )
    }
}
