package ru.yandex.market.navigation.delegate

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
import ru.yandex.market.clean.presentation.feature.order.feedback.dialog.OrderFeedbackQuestionsDialogFragment
import ru.yandex.market.clean.presentation.feature.order.feedback.dialog.OrderFeedbackQuestionsDialogTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen

class AlreadyDeliveredQuestionnaireNavigationActionTest {

    private val order = Order.generateTestInstance().copy(id = ORDER_ID, isUserReceived = false)
    private val isArchived = false

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

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getOrderUseCase = getOrderUseCase,
    )

    @Test
    fun `Check already delivered questionare`() {
        actionRouter.stillNotDelivery(ORDER_ID) {}

        val screen = OrderFeedbackQuestionsDialogTargetScreen(
            OrderFeedbackQuestionsDialogFragment.Arguments(
                orderId = ORDER_ID.toString(),
                isDsbs = false,
                sourceScreen = router.currentScreen
            )
        )
        verify(router).navigateForResult(eq(screen),
            argThat { _ -> true })
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
