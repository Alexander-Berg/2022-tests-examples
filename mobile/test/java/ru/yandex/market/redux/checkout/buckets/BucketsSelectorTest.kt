package ru.yandex.market.redux.checkout.buckets

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.base.redux.selector.useSelector
import ru.yandex.market.base.redux.stateobject.LoadingState
import ru.yandex.market.base.redux.stateobject.StateObject
import ru.yandex.market.clean.domain.model.checkout.BucketInfo2
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.redux.selectors.checkout.buckets.selectBuckets
import ru.yandex.market.redux.selectors.checkout.buckets.selectBucketsCount
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class BucketsSelectorTest {

    @Test
    fun `Buckets selectors on empty state`() {
        val emptyState = AppState()
        val buckets = emptyState.useSelector(selectBuckets())
        val bucketsCount = emptyState.useSelector(selectBucketsCount())

        Assert.assertEquals(buckets, emptyList<BucketInfo2>())
        Assert.assertEquals(bucketsCount, 0)
    }

    @Test
    fun `Buckets selectors on non-empty state`() {
        val split = checkoutSplitTestInstance()
        val checkoutState = CheckoutState(
            checkoutSplits = StateObject(
                loadingState = LoadingState.READY,
                stateValue = mapOf(split.id to split),
                allIds = listOf(split.id)
            )
        )
        val appState = AppState(checkoutState = checkoutState)
        val buckets = appState.useSelector(selectBuckets())
        val bucketsCount = appState.useSelector(selectBucketsCount())

        Assert.assertEquals(buckets, split.buckets)
        Assert.assertEquals(bucketsCount, split.buckets.size)
    }
}