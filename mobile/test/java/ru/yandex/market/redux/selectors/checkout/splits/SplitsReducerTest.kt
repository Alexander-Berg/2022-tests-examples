package ru.yandex.market.redux.selectors.checkout.splits

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.action.Action
import ru.yandex.market.base.redux.stateobject.LoadingState
import ru.yandex.market.base.redux.stateobject.StateObject
import ru.yandex.market.base.redux.stateobject.asStateObject
import ru.yandex.market.base.redux.stateobject.emptyStateObject
import ru.yandex.market.checkout.domain.model.packPositionTestInstance
import ru.yandex.market.clean.domain.model.checkout.CheckoutFlowStatus
import ru.yandex.market.clean.domain.model.checkout.CheckoutSplit
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.usercontact.userContactTestInstance
import ru.yandex.market.data.order.options.OrderSummary
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.cashback.model.CashbackOptionType
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.domain.payment.model.yandexCardInfo_LimitedTestInstance
import ru.yandex.market.internal.sync.Synchronized
import ru.yandex.market.redux.actions.checkout.buckets.BatchBucketAction
import ru.yandex.market.redux.actions.checkout.buckets.BucketAction
import ru.yandex.market.redux.actions.checkout.legacy.UpdateCheckoutStateFromLegacyFlowDataStoreAction
import ru.yandex.market.redux.reducers.checkout.splits.reduceCheckoutSplits
import ru.yandex.market.redux.states.CheckoutState

class SplitsReducerTest {

    @Test
    fun `Checkout split state updated by UpdateCheckoutStateFromLegacyFlowDataStoreAction`() {
        val emptySplitState: StateObject<CheckoutSplit> = emptyStateObject()
        val checkoutState = CheckoutState(checkoutSplits = emptySplitState)
        val split = checkoutSplitTestInstance()
        val updateCheckoutStateAction = createUpdateCheckoutAction(split)
        val reducedSplitState = reduceCheckoutSplits(checkoutState, updateCheckoutStateAction)
        val expectedSplitState = StateObject(
            loadingState = LoadingState.READY,
            stateValue = mapOf(split.id to split),
            allIds = listOf(split.id)
        )

        assertThat(reducedSplitState).isEqualTo(expectedSplitState)
        assertThat(reducedSplitState).isNotEqualTo(emptySplitState)
    }

    @Test
    fun `Checkout split state not updated by random action`() {
        val emptySplitState: StateObject<CheckoutSplit> = emptyStateObject()
        val checkoutState = CheckoutState(checkoutSplits = emptySplitState)
        val randomAction = object : Action {}
        val reducedSplitState = reduceCheckoutSplits(checkoutState, randomAction)

        assertThat(reducedSplitState).isEqualTo(emptySplitState)
    }

    @Test
    fun `Checkout split state updated by BucketAction`() {
        val bucket = bucketInfo2TestInstance()
        val split = checkoutSplitTestInstance().copy(buckets = listOf(bucket))
        val packPosition = packPositionTestInstance()
        val checkoutSplitState = mapOf(split.id to split).asStateObject()
        val checkoutState = CheckoutState(checkoutSplits = checkoutSplitState)
        val bucketAction = BucketAction.UpdateBucketPackPositionAction(
            id = bucket.packId,
            packPosition = packPosition,
        )
        val splits = reduceCheckoutSplits(checkoutState, bucketAction)
        val updatedPackPosition = splits.stateValue[split.id]?.buckets?.first()?.packPosition

        assertThat(updatedPackPosition).isEqualTo(packPosition)
    }

    @Test
    fun `Checkout split state updated by ComposedBucketAction`() {
        val bucket = bucketInfo2TestInstance()
        val split = checkoutSplitTestInstance().copy(buckets = listOf(bucket))
        val packPosition = packPositionTestInstance()
        val checkoutSplitState = mapOf(split.id to split).asStateObject()
        val checkoutState = CheckoutState(checkoutSplits = checkoutSplitState)
        val bucketActions = listOf(
            BucketAction.UpdateBucketPackPositionAction(
                id = bucket.packId,
                packPosition = packPosition,
            ),
            BucketAction.UpdateBucketOnDemandPaymentMethodAction(
                id = bucket.packId,
                paymentMethod = PaymentMethod.GOOGLE_PAY,
            ),
        )
        val splits = reduceCheckoutSplits(checkoutState, BatchBucketAction(bucketActions))
        val updatedPackPosition = splits.stateValue[split.id]?.buckets?.first()?.packPosition
        val updatedOnDemandPaymentMethod = splits.stateValue[split.id]?.buckets?.first()?.onDemandPaymentMethod

        assertThat(updatedPackPosition).isEqualTo(packPosition)
        assertThat(updatedOnDemandPaymentMethod).isEqualTo(PaymentMethod.GOOGLE_PAY)
    }

    private fun createUpdateCheckoutAction(split: CheckoutSplit): UpdateCheckoutStateFromLegacyFlowDataStoreAction {
        val userContact = userContactTestInstance()
        val paymentMethod = PaymentMethod.GOOGLE_PAY
        val selectedCashbackOptionType = CashbackOptionType.KEEP
        val cashback = cashbackTestInstance()
        val orderSummary = OrderSummary.testInstance()

        return UpdateCheckoutStateFromLegacyFlowDataStoreAction(
            status = CheckoutFlowStatus.PREPARED,
            splits = listOf(split),
            selectedUserContact = userContact,
            selectedPaymentMethod = paymentMethod,
            selectedPromoCode = "some promo",
            selectedCashbackOptionType = selectedCashbackOptionType,
            actualizedCashback = cashback,
            orderSummary = Synchronized(orderSummary),
            isSubscriptionRequired = true,
            isSplitsPrefilled = true,
            isBnplSwitched = false,
            yandexCardInfo = yandexCardInfo_LimitedTestInstance(),
            selectedBnplPlanConstructorType = null,
        )
    }
}
