package ru.yandex.market.redux.selectors.checkout.splits

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.selector.useSelector
import ru.yandex.market.base.redux.stateobject.LoadingState
import ru.yandex.market.base.redux.stateobject.StateObject
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class SplitsSelectorTest {
    @Test
    fun `CheckoutSplits selector on empty state`() {
        val emptyState = AppState()
        val splits = emptyState.useSelector(selectCheckoutSplits())
        val nullSplit = emptyState.useSelector(selectCheckoutSplit("0"))

        assertThat(splits).isEmpty()
        assertThat(nullSplit).isNull()
    }

    @Test
    fun `CheckoutSplits selector on non-empty state`() {
        val checkoutSplit = checkoutSplitTestInstance()
        val appState = AppState(
            CheckoutState(
                checkoutSplits = StateObject(
                    loadingState = LoadingState.READY,
                    stateValue = mapOf(checkoutSplit.id to checkoutSplit),
                    allIds = listOf(checkoutSplit.id)
                )
            )
        )
        val selectedSplits = appState.useSelector(selectCheckoutSplits())
        val singleSplit = appState.useSelector(selectCheckoutSplit(checkoutSplit.id))

        assertThat(selectedSplits).isEqualTo(listOf(checkoutSplit))
        assertThat(singleSplit).isEqualTo(checkoutSplit)
    }
}