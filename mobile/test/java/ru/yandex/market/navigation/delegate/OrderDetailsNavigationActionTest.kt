package ru.yandex.market.navigation.delegate

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.activity.order.details.OrderDetailsParams
import ru.yandex.market.activity.order.details.OrderDetailsTargetScreen
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen

class OrderDetailsNavigationActionTest {

    private val isArchived = false
    private val order = Order.generateTestInstance().copy(id = ORDER_ID, isArchived = isArchived)

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
    fun `Check open order details screen`() {
        actionRouter.showOrderDetailsByOrderId(orderId = ORDER_ID, isArchived = isArchived, onNavigationFinished = {})

        val screen = OrderDetailsTargetScreen(
            params = OrderDetailsParams(
                orderId = order.id.toString(),
                shopOrderId = order.shopOrderId,
                trackDeliveryServiceId = order.trackDeliveryServiceId,
                trackingCode = order.trackingCode,
                isClickAndCollect = order.isClickAndCollect,
                isArchived = isArchived
            )
        )
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
