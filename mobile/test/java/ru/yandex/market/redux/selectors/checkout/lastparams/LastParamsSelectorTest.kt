package ru.yandex.market.redux.selectors.checkout.lastparams

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.selector.useSelector
import ru.yandex.market.base.redux.stateobject.asSingleStateObject
import ru.yandex.market.base.redux.stateobject.asStateObject
import ru.yandex.market.clean.domain.model.checkout.BucketState
import ru.yandex.market.clean.domain.model.checkout.CheckoutCommonUserInput
import ru.yandex.market.clean.domain.model.checkout.CheckoutLastParams
import ru.yandex.market.clean.domain.model.checkout.CheckoutParcels
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.checkout.extractIdForBucketState
import ru.yandex.market.clean.domain.model.usercontact.userContactTestInstance
import ru.yandex.market.data.order.PaymentType
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class LastParamsSelectorTest {

    @Test
    fun `LastParams selector on empty state`() {
        val emptyState = AppState()
        val lastParams = emptyState.useSelector(selectLastParams())
        assertThat(lastParams).isEqualTo(CheckoutLastParams.EMPTY)
    }

    @Test
    fun `LastParams selector on non-empty state`() {
        val split = checkoutSplitTestInstance()
        val bucket = split.buckets.first()
        val userContact = userContactTestInstance()
        val checkoutState = CheckoutState(
            checkoutSplits = mapOf(split.id to split).asStateObject(),
            checkoutCommonUserInput = CheckoutCommonUserInput.EMPTY
                .copy(
                    selectedUserContact = userContact,
                    selectedPaymentMethod = PaymentMethod.GOOGLE_PAY,
                ).asSingleStateObject()
        )
        val appState = AppState(checkoutState = checkoutState)
        val lastParams = appState.useSelector(selectLastParams())
        val states = lastParams.parcelsInfo?.states?.first()
        val expected = CheckoutLastParams(
            paymentType = PaymentType.PREPAID,
            paymentMethod = PaymentMethod.GOOGLE_PAY,
            contactId = userContact.id,
            paymentOptionId = null,
            parcelsInfo = CheckoutParcels(
                states = listOf(
                    BucketState(
                        id = bucket.extractIdForBucketState(),
                        deliveryType = split.selectedDeliveryType,
                        isOnDemandSelected = bucket.isOnDemandDeliverySelected,
                        address = split.selectedUserAddress,
                        outletId = split.selectedPostOutletPoint?.outletInfo?.id,
                        intervalBeginDate = states?.intervalBeginDate,
                        intervalEndDate = states?.intervalEndDate,
                        deliveryTimeInterval = states?.deliveryTimeInterval,
                    )
                )
            )
        )
        assertThat(lastParams).isEqualTo(expected)
    }
}
