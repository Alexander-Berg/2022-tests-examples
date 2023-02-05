package ru.yandex.market.clean.presentation.feature.checkout.confirm.plus

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.presentation.feature.cart.vo.PlusInfoVo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeArguments
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeFlowAnalyticsInfo
import ru.yandex.market.clean.presentation.feature.plushome.PlusHomeTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.reduxPresenterDependenciesMock

class CheckoutConfirmPlusInfoItemPresenterTest {
    private val router = mock<Router> {
        on { currentScreen } doReturn Screen.HOME
    }
    val presenter = CheckoutConfirmPlusInfoItemPresenter(
        router,
        reduxPresenterDependenciesMock(),
        mock(),
        mock()
    )

    @Test
    fun testNavigateToPlus() {
        presenter.navigateToPlus(PlusInfoVo.NavigateTarget.PlusHome().plusStoryId)
        verify(router).navigateTo(
            PlusHomeTargetScreen(
                PlusHomeArguments(PlusHomeFlowAnalyticsInfo(Screen.HOME.toString()))
            )
        )
    }
}