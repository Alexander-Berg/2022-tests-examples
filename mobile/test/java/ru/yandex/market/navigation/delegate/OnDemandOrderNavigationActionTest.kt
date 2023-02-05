package ru.yandex.market.navigation.delegate

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.beru.android.R
import ru.yandex.market.activity.web.MarketWebActivityArguments
import ru.yandex.market.activity.web.MarketWebActivityTargetScreen
import ru.yandex.market.clean.domain.model.EatsKitService
import ru.yandex.market.clean.domain.model.lavka.OnDemandCourierLink
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.usecase.lavka.OnDemandCourierLinkUseCase
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.presentation.feature.eatskit.EatsKitWebViewArguments
import ru.yandex.market.clean.presentation.feature.eatskit.bottomsheet.EatsKitWebViewBottomSheetTargetScreen
import ru.yandex.market.clean.presentation.feature.eatskit.flow.EatsKitFlowTarget
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandAnalytics
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandCourierScreenManager
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.util.manager.InstalledApplicationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class OnDemandOrderNavigationActionTest {

    private val order = Order.generateTestInstance().copy(id = ORDER_ID, trackingCode = TRACKING_CODE)
    private var isGoInstalled = true
    private var courierLink = OnDemandCourierLink(
        URL,
        APP_LINK,
        true,
        LAVKA_PATH,
        true
    )

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.ALL_ORDERS
    }

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val installedApplicationManager = mock<InstalledApplicationManager> {
        on { isApplicationInstalled(GO_APP_ID) } doReturn Single.just(isGoInstalled)
    }

    private val getOrderUseCase = lazy {
        mock<GetOrderUseCase> {
            on { execute(ORDER_ID.toString(), false) } doReturn Single.just(order)
        }
    }

    private val onDemandCourierLinkUseCase = lazy {
        mock<OnDemandCourierLinkUseCase> {
            on {
                execute(
                    isGoInstalled,
                    requireNotNull(order.trackingCode),
                    order.id.toString(),
                    order.onDemandWarehouseType,
                    order.address?.regionId
                )
            } doReturn Single.just(courierLink)
        }
    }

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val resourceManager = ResourcesManagerImpl(appContext.resources)
    private val onDemandAnalytics = mock<OnDemandAnalytics>()
    private val onDemandCourierScreenManager = OnDemandCourierScreenManager(
        appContext,
        resourceManager,
        onDemandAnalytics
    )
    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
        installedApplicationManager = installedApplicationManager,
        resourcesManager = resourceManager,
        getOrderUseCase = getOrderUseCase,
        onDemandCourierLinkUseCase = onDemandCourierLinkUseCase,
        onDemandCourierScreenManager = onDemandCourierScreenManager
    )

    @Test
    fun `Calling ondemand courier in full screen`() {
        courierLink = courierLink.copy(groupedOrdersFeatureEnabled = true)

        actionRouter.openOnDemandByOrderId(orderId = ORDER_ID, isArchived = false, onNavigationFinished = {})

        val screen = EatsKitFlowTarget(
            args = EatsKitWebViewArguments(
                path = LAVKA_PATH,
                service = EatsKitService.LAVKA
            )
        )
        verify(router).navigateTo(screen)
    }

    @Test
    fun `Calling ondemand courier in bottom sheet screen`() {
        courierLink = courierLink.copy(groupedOrdersFeatureEnabled = false)

        actionRouter.openOnDemandByOrderId(orderId = ORDER_ID, isArchived = false, onNavigationFinished = {})

        val screen = EatsKitWebViewBottomSheetTargetScreen(
            args = EatsKitWebViewArguments(
                path = LAVKA_PATH,
                service = EatsKitService.LAVKA
            )
        )
        verify(router).navigateTo(screen)
    }

    @Test
    fun `Calling ondemand courier in webView`() {
        courierLink = courierLink.copy(lavkaPath = null, appLink = null)

        actionRouter.openOnDemandByOrderId(orderId = ORDER_ID, isArchived = false, onNavigationFinished = {})

        val screen = MarketWebActivityTargetScreen(
            MarketWebActivityArguments.builder()
                .isUrlOverridingEnabled(true)
                .link(requireNotNull(courierLink.url))
                .title(resourceManager.getFormattedString(R.string.order_on_demand_webview_title, ORDER_ID.toString()))
                .build()
        )
        verify(router).navigateTo(screen)
    }

    companion object {
        private const val GO_APP_ID = R.string.taxi_app_id
        private const val ORDER_ID = 1234L
        private const val URL = "url"
        private const val APP_LINK = "applink"
        private const val LAVKA_PATH = "lavkaPath"
        private const val TRACKING_CODE = "trackingCode"
    }
}
