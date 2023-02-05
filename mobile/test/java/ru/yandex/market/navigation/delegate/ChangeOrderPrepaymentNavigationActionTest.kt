package ru.yandex.market.navigation.delegate

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.presentation.feature.order.change.prepayment.flow.ChangePrepaymentFlowFragment
import ru.yandex.market.clean.presentation.feature.order.change.prepayment.flow.ChangePrepaymentFlowTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory

class ChangeOrderPrepaymentNavigationActionTest {

    private val order = Order.generateTestInstance().copy(id = ORDER_ID)
    private val isArchived = false

    private val router = mock<Router>()

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val getOrderUseCase = lazy {
        mock<GetOrderUseCase> {
            on { execute(ORDER_ID.toString(), isArchived) } doReturn Single.just(order)
        }
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getOrderUseCase = getOrderUseCase
    )

    @Test
    fun `Check change order prepayment navigation`() {
        actionRouter.changePrepaymentByOrderId(orderId = ORDER_ID, isArchived = isArchived, onNavigationFinished = {})

        val screen = ChangePrepaymentFlowTargetScreen(
            ChangePrepaymentFlowFragment.Arguments(
                orderId = ORDER_ID.toString(),
                isPreorder = order.isPreOrder,
                isFromCheckout = false,
                isSpasiboPayEnabled = false,
                selectedPaymentMethod = order.paymentMethod,
                payer = null,
                isStationSubscription = order.isStationSubscriptionItem
            )
        )
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
