package ru.yandex.market.clean.data.store.checkout

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.base.redux.store.LegacyCompatibleAppStateStore
import ru.yandex.market.base.redux.store.configureStore
import ru.yandex.market.clean.domain.model.checkout.CheckoutFlowStatus
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.usercontact.userContactTestInstance
import ru.yandex.market.data.order.options.OrderSummary
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.cashback.model.CashbackOptionType
import ru.yandex.market.domain.cashback.model.cashbackTestInstance
import ru.yandex.market.internal.sync.Synchronized
import ru.yandex.market.redux.reducers.AppReducer
import ru.yandex.market.redux.states.AppState

class CheckoutFlowStateDataStoreTest {

    private lateinit var checkoutFlowStateDataStore: CheckoutFlowStateDataStore

    private val reduxStore = LegacyCompatibleAppStateStore(
        appStateStore = configureStore(AppState()) { reducer = AppReducer() },
        reduxCommonHealthAnalytics = mock()
    )

    @Before
    fun setUp() {
        checkoutFlowStateDataStore = ReduxCheckoutFlowStateDataStore(reduxStore)
    }

    @Test
    fun `Can run transaction and get updated fields`() {
        val fieldsToUpdate = createTestUpdateFields()

        val beforeUpdateFields = checkoutFlowStateDataStore.getFields()
        checkoutFlowStateDataStore.runTransaction { fieldsToUpdate }
        val afterUpdateField = checkoutFlowStateDataStore.getFields()

        Assert.assertNotEquals(fieldsToUpdate, beforeUpdateFields)
        Assert.assertEquals(fieldsToUpdate, afterUpdateField)
    }

    @Test
    fun `Can observe new fields on each transaction`() {
        val firstUpdate = createTestUpdateFields()
        val secondUpdate = createTestUpdateFields().copy(
            isSubscriptionRequired = false,
            isSplitsPrefilled = false,
            isBnplSwitched = true
        )

        val subscriber = checkoutFlowStateDataStore.getFieldsStream().test()
        checkoutFlowStateDataStore.runTransaction { firstUpdate }
        checkoutFlowStateDataStore.runTransaction { secondUpdate }

        subscriber
            .assertValueCount(3)
            .assertValueAt(1) { it == firstUpdate }
            .assertValueAt(2) { it == secondUpdate }
    }

    @Test
    fun `Can reset fields to empty state`() {
        val emptyState = CheckoutFlowStateFields()
        val testUpdate = createTestUpdateFields()

        checkoutFlowStateDataStore.runTransaction { testUpdate }
        val fieldsAfterUpdate = checkoutFlowStateDataStore.getFields()
        checkoutFlowStateDataStore.reset()
        val fieldsAfterReset = checkoutFlowStateDataStore.getFields()

        Assert.assertNotEquals(emptyState, fieldsAfterUpdate)
        Assert.assertEquals(emptyState, fieldsAfterReset)
    }

    private fun createTestUpdateFields(): CheckoutFlowStateFields {
        val split = checkoutSplitTestInstance()
        val userContact = userContactTestInstance()
        val paymentMethod = PaymentMethod.GOOGLE_PAY
        val selectedCashbackOptionType = CashbackOptionType.KEEP
        val cashback = cashbackTestInstance()
        val orderSummary = OrderSummary.testInstance()

        return CheckoutFlowStateFields(
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
        )
    }
}