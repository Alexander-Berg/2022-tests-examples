package ru.yandex.market.redux.reducers.checkout.commonuserinput

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.selector.useSelector
import ru.yandex.market.base.redux.stateobject.asSingleStateObject
import ru.yandex.market.clean.domain.model.checkout.CheckoutCommonUserInput
import ru.yandex.market.redux.selectors.checkout.commonuserinput.selectPaymentMethod
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class CommonUserInputSelectorTest {

    @Test
    fun `PaymentMethod selector on empty state`() {
        val emptyState = AppState()
        val paymentMethod = emptyState.useSelector(selectPaymentMethod())
        assertThat(paymentMethod).isNull()
    }

    @Test
    fun `PaymentMethod selector on non-empty state`() {
        val paymentMethod = PaymentMethod.GOOGLE_PAY
        val commonUserInputState = CheckoutCommonUserInput.EMPTY
            .copy(selectedPaymentMethod = paymentMethod).asSingleStateObject()
        val appState = AppState(
            CheckoutState(
                checkoutCommonUserInput = commonUserInputState
            )
        )
        val selectedPaymentMethod = appState.useSelector(selectPaymentMethod())

        assertThat(selectedPaymentMethod).isEqualTo(paymentMethod)
    }

}