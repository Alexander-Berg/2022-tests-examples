package ru.yandex.market.clean.presentation.feature.cashback.about

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.checkout.summary.SummaryCashbackVo
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.clean.presentation.navigation.TargetScreen
import ru.yandex.market.feature.cahsback.AboutCashBackInfoTypeArgument

@RunWith(Parameterized::class)
class AboutCashbackNavigateDelegateTest(
    private val navigationTarget: SummaryCashbackVo.NavigationTarget,
    private val expectedScreen: TargetScreen<*>
) {

    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }
    private val delegate = AboutCashbackNavigateDelegate(router)

    @Test
    fun onClick() {
        delegate.onClick(navigationTarget)
        verify(router).navigateTo(expectedScreen)
    }

    companion object {

        private val CURRENT_SCREEN = Screen.SKU

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                SummaryCashbackVo.NavigationTarget.AboutPlus(semanticId = null),
                AboutCashBackTargetScreen(
                    AboutCashBackDialogArguments(
                        CURRENT_SCREEN,
                        AboutCashBackInfoTypeArgument.Common
                    )
                ),
            ),
            //1
            arrayOf(
                SummaryCashbackVo.NavigationTarget.AboutMasterCard,
                AboutCashBackTargetScreen(
                    AboutCashBackDialogArguments(
                        CURRENT_SCREEN,
                        AboutCashBackInfoTypeArgument.MasterCard
                    )
                ),
            ),
        )
    }
}