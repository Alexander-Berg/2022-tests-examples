package ru.yandex.market.navigation.delegate

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.presentation.feature.pickuprenewal.PickupRenewalArguments
import ru.yandex.market.clean.presentation.feature.pickuprenewal.PickupRenewalTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.data.passport.Address
import java.util.Date

class ExtendOrderStoragePeriodNavigationActionTest {

    private val order = Order.generateTestInstance().copy(
        id = ORDER_ID,
        outletStorageLimitDate = OUTLET_STORAGE_LIMIT_DATE,
        address = ADDRESS
    )

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

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        getOrderUseCase = getOrderUseCase
    )


    @Test
    fun `Check navigate to extend storage period screen`() {
        actionRouter.extendPeriod(orderId = ORDER_ID)

        val screen = PickupRenewalTargetScreen(
            arguments = PickupRenewalArguments(
                orderId = ORDER_ID.toString(),
                currentEndDate = OUTLET_STORAGE_LIMIT_DATE,
                address = ADDRESS
            )
        )
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val ORDER_ID = 1234L
        private val OUTLET_STORAGE_LIMIT_DATE = Date()
        private val ADDRESS = Address.testInstance()
    }
}
