package ru.yandex.market.navigation.delegate

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.activity.order.RepeatOrderUseCase
import ru.yandex.market.clean.data.mapper.OrderItemInfoMapper
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.presentation.feature.cart.CartParams
import ru.yandex.market.clean.presentation.feature.cart.CartTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.fragment.order.OrderItemInfo

class RepeatOrderActionTest {

    private val order = Order.generateTestInstance().copy(id = ORDER_ID, isUserReceived = false)

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.ALL_ORDERS
    }

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val getOrderUseCase = lazy {
        mock<GetOrderUseCase> {
            on { execute(ORDER_ID.toString(), false) } doReturn Single.just(order)
        }
    }

    private val orderInfoListMock: List<OrderItemInfo> = listOf(mock())

    private val orderItemInfoMapper = mock<OrderItemInfoMapper> {
        on { map(order) } doReturn orderInfoListMock
    }

    private val repeatOrderUseCase = lazy {
        mock<RepeatOrderUseCase> {
            on { execute(orderInfoListMock) } doReturn Single.just(listOf(mock()))
        }
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getOrderUseCase = getOrderUseCase,
        repeatOrderUseCase = repeatOrderUseCase,
        orderItemInfoMapper = orderItemInfoMapper
    )

    @Test
    fun `Check repeat order`() {
        actionRouter.repeatOrder(orderId = ORDER_ID, isArchived = false)

        val screen = CartTargetScreen(params = CartParams(isPriceDropSpecialPlace = false))
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
