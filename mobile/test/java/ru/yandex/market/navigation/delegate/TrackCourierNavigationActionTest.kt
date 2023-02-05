package ru.yandex.market.navigation.delegate

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.beru.android.R
import ru.yandex.market.activity.order.details.OrderDetailsParams
import ru.yandex.market.activity.order.details.OrderDetailsTargetScreen
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetCourierTrackingUrlUseCase
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.android.ResourcesManager

class TrackCourierNavigationActionTest {

    private val isArchived = false
    private val order = Order.generateTestInstance().copy(
        id = ORDER_ID,
        isUserReceived = false,
        isArchived = isArchived
    )
    private var link = ""

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.ALL_ORDERS
    }

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val getCourierTrackingUrlUseCase = lazy {
        mock<GetCourierTrackingUrlUseCase> {
            on { getTrackingUrl(ORDER_ID) } doReturn Single.just(link)
        }
    }

    private val getOrderUseCase = lazy {
        mock<GetOrderUseCase> {
            on { execute(ORDER_ID.toString(), isArchived) } doReturn Single.just(order)
        }
    }

    private val resourceManager = mock<ResourcesManager> {
        on { getString(R.string.order_track_on_map_title) } doReturn TRACK_ORDER
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getCourierTrackingUrlUseCase = getCourierTrackingUrlUseCase,
        resourcesManager = resourceManager,
        getOrderUseCase = getOrderUseCase
    )

    @Test
    fun `Open track courier screen if link empty`() {
        link = ""
        actionRouter.showCourierTrackingScreen(orderId = ORDER_ID)

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

    @Test
    fun `Open track courier screen if link not empty`() {
        link = "link"

        actionRouter.showCourierTrackingScreen(orderId = ORDER_ID)

        val screen = MarketWebActivityTargetScreen(
            MarketWebActivityArguments.builder()
                .link(link)
                .title(resourceManager.getString(R.string.order_track_on_map_title))
                .isUrlOverridingEnabled(true)
                .build()
        )
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
        private const val TRACK_ORDER = "Отслеживание заказа"
    }
}
