package ru.yandex.market.redux.checkout

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.stateobject.asStateObject
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.redux.dispatchers.checkout.setup.CheckoutStateUpdateFromLegacyAction
import ru.yandex.market.redux.reducers.checkout.reduceCheckoutState
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class CheckoutReducerTest {

    @Test
    fun `Checkout state updated by CheckoutStateUpdateAction`() {
        val checkoutState = CheckoutState()
        val split = checkoutSplitTestInstance()
        val splits = mapOf(split.id to split)
        val action = CheckoutStateUpdateFromLegacyAction {
            it.copy(checkoutSplits = splits.asStateObject())
        }
        val reducedState = reduceCheckoutState(AppState(checkoutState = checkoutState), action)
        val expectedState = checkoutState.copy(
            checkoutSplits = splits.asStateObject()
        )

        assertThat(reducedState).isEqualTo(expectedState)
    }
}
