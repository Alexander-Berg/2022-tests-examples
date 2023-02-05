package ru.yandex.market.redux.reducers.checkout.cashback

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.selector.from
import ru.yandex.market.base.redux.selector.use
import ru.yandex.market.base.redux.stateobject.asSingleStateObject
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.redux.selectors.checkout.cashback.selectCashback
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class CashbackSelectorTest {

    @Test
    fun `Cashback selector on empty state`() {
        val emptyState = AppState()
        val cashback = selectCashback().from(emptyState).use()
        assertThat(cashback).isNull()
    }

    @Test
    fun `Cashback selector on non-empty state`() {
        val cashbackState = cashbackTestInstance().asSingleStateObject()
        val appState = AppState(
            CheckoutState(
                cashbackState = cashbackState
            )
        )
        val selectedCashback = selectCashback().from(appState).use()
        assertThat(selectedCashback).isEqualTo(cashbackState.stateValue)
    }
}