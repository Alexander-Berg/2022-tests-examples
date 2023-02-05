package ru.yandex.market.navigation.delegate

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.domain.usecase.orderfeedback.SetOrderDeliveryFeedbackUseCase
import ru.yandex.market.clean.presentation.feature.orderfeedback.OrderFeedbackDialogFragment
import ru.yandex.market.clean.presentation.feature.orderfeedback.OrderFeedbackDialogTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen

class AlreadyDeliveredGradeNavigationActionTest {

    private val isArchived = false
    private val order = Order.generateTestInstance().copy(
        id = ORDER_ID,
        isUserReceived = false,
        isArchived = isArchived
    )

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.ALL_ORDERS
    }

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val getOrderUseCase = lazy {
        mock<GetOrderUseCase> {
            on { execute(ORDER_ID.toString(), isArchived) } doReturn Single.just(order)
        }
    }

    private val setOrderDeliveryFeedbackUseCase = lazy {
        mock<SetOrderDeliveryFeedbackUseCase> {
            on { execute(ORDER_ID.toString(), isArchived) } doReturn Completable.complete()
        }
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getOrderUseCase = getOrderUseCase,
        setOrderDeliveryFeedbackUseCase = setOrderDeliveryFeedbackUseCase
    )

    @Test
    fun `Check already delivered navigation`() {
        actionRouter.alreadyDelivered(ORDER_ID, isArchived) {}

        val screenArgs = OrderFeedbackDialogFragment.Arguments(
            orderId = ORDER_ID.toString(),
            sourceScreen = router.currentScreen
        )
        val screen = OrderFeedbackDialogTargetScreen(screenArgs)
        verify(router).navigateForResult(eq(screen),
            argThat { _ -> true })
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
