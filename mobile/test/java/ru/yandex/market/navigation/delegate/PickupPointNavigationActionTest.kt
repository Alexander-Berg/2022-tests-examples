package ru.yandex.market.navigation.delegate

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.checkout.pickup.single.PickupPointArguments
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.domain.usecase.pickup.renewal.CanShowPickupRenewalButtonUseCase
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.data.order.options.point.OutletPoint

class PickupPointNavigationActionTest {

    private val isArchived = false
    private val canShowPickupRenewalButton = true
    private val isClickAndCollect = false
    private val order = Order.generateTestInstance().copy(
        id = ORDER_ID,
        isArchived = isArchived,
        isClickAndCollect = isClickAndCollect
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

    private val canShowPickupRenewalButtonUseCase = lazy {
        mock<CanShowPickupRenewalButtonUseCase> {
            on { execute(order) } doReturn Single.just(canShowPickupRenewalButton)
        }
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getOrderUseCase = getOrderUseCase,
        canShowPickupRenewalButtonUseCase = canShowPickupRenewalButtonUseCase
    )

    @Test
    fun `Check change order prepayment navigation`() {
        actionRouter.showPickupPointByOrderId(orderId = ORDER_ID, isArchived = isArchived, onNavigationFinished = {})

        val screen = PickupPointArguments.Builder()
            .isClickAndCollect(order.isClickAndCollect)
            .showNearestDelivery(true)
            .showSelectButton(false)
            .showBuildRouteButton(true)
            .outlet(OutletPoint(order.outletInfo))
            .orderId(order.id)
            .storageLimitDate(order.outletStorageLimitDate)
            .showRenewStorageLimitDateButton(canShowPickupRenewalButton)
            .sourceScreen(router.currentScreen)
            .buildTarget()
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
    }
}
