package ru.yandex.market.redux.selectors.checkout.orderitems

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.base.redux.selector.useSelector
import ru.yandex.market.base.redux.stateobject.LoadingState
import ru.yandex.market.base.redux.stateobject.StateObject
import ru.yandex.market.clean.domain.model.OrderItem
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class OrderItemsSelectorTest {

    @Test
    fun `OrderItems selector on empty state`() {
        val emptyState = AppState()
        val orderItems = emptyState.useSelector(selectOrderItems(emptyList()))

        Assert.assertEquals(orderItems, emptyList<OrderItem>())
    }

    @Test
    fun `OrderItems selector on non-empty state`() {
        val split = checkoutSplitTestInstance()
        val checkoutState = CheckoutState(
            checkoutSplits = StateObject(
                loadingState = LoadingState.READY,
                stateValue = mapOf(split.id to split),
                allIds = listOf(split.id)
            )
        )
        val appState = AppState(checkoutState = checkoutState)
        val emptyOrderItems = appState.useSelector(selectOrderItems(emptyList()))
        val nonEmptyOrderItems = appState.useSelector(selectOrderItems(listOf(split.buckets.first().packId)))

        Assert.assertEquals(emptyOrderItems, emptyList<OrderItem>())
        Assert.assertEquals(nonEmptyOrderItems, split.buckets.first().orderItems)
    }
}