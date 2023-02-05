package ru.yandex.market.navigation.delegate

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.EatsKitService
import ru.yandex.market.clean.presentation.feature.eatskit.EatsKitWebViewArguments
import ru.yandex.market.clean.presentation.feature.eatskit.flow.EatsKitFlowTarget
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.clean.presentation.navigation.Screen

class FapiLinkNavigationActionTest {

    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.ALL_ORDERS
    }

    private val routerFactory = mock<RouterFactory> {
        on { create(any()) } doReturn router
    }

    private val actionRouter = ActionRouterGenerator().generate(
        routerFactory = routerFactory,
    )

    @Test
    fun `Check fapi link navigation`() {
        actionRouter.openEatsKitFlow(targetUrl = TARGET_LINK)

        EatsKitFlowTarget(args = EatsKitWebViewArguments(path = TARGET_LINK, service = EatsKitService.LAVKA))

        verify(router).navigateTo(any<EatsKitFlowTarget>())
    }

    companion object {
        private const val TARGET_LINK = "targetLink"
    }
}
