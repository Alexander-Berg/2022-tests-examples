package ru.yandex.market.clean.presentation.feature.cart.item.welcomecashback

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analitycs.events.cashback.CashbackInformerAnalyticFacade
import ru.yandex.market.analytics.facades.GrowingCashbackAnalytics
import ru.yandex.market.analytics.facades.PossibleCashbackAgitationAnalytics
import ru.yandex.market.analytics.facades.WelcomeCashbackAnalytics
import ru.yandex.market.clean.presentation.feature.bank.YandexBankArguments
import ru.yandex.market.clean.presentation.feature.bank.YandexBankTargetScreen
import ru.yandex.market.clean.presentation.feature.cart.vo.PossibleCashbackVo
import ru.yandex.market.clean.presentation.feature.cart.vo.possibleCashbackVo_AdditionalAnalyticsInfoTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.about.AboutCashBackDialogArguments
import ru.yandex.market.clean.presentation.feature.cashback.about.AboutCashBackTargetScreen
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.GrowingCashbackTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.feature.cahsback.AboutCashBackInfoTypeArgument
import ru.yandex.market.presentationSchedulersMock

class PossibleCashbackPresenterTest {

    private val vo = mock<PossibleCashbackVo>()
    private val currentScreen = Screen.CART
    private val router = mock<Router> {
        on { currentScreen } doReturn currentScreen
    }
    private val viewState = mock<`PossibleCashbackView$$State`>()
    private val welcomeCashbackAnalytics = mock<WelcomeCashbackAnalytics>()
    private val cashbackInformerAnalyticFacade = mock<CashbackInformerAnalyticFacade>()
    private val growingCashbackAnalytics = mock<GrowingCashbackAnalytics>()
    private val possibleCashbackAgitationAnalytics = mock<PossibleCashbackAgitationAnalytics>()

    private val presenter = PossibleCashbackPresenter(
        presentationSchedulersMock(),
        vo,
        router,
        welcomeCashbackAnalytics,
        cashbackInformerAnalyticFacade,
        growingCashbackAnalytics,
        possibleCashbackAgitationAnalytics
    )

    @Before
    fun setup() {
        presenter.setViewState(viewState)
    }

    @Test
    fun `show vo only on first attach`() {
        whenever(vo.cashbackType) doReturn PossibleCashbackVo.CashbackType.MASTERCARD
        presenter.attachView(viewState)
        presenter.attachView(viewState)

        verify(viewState).showInfo(vo)
    }

    @Test
    fun `navigate to welcome cashback about screen`() {
        presenter.onActionClick(PossibleCashbackVo.CashbackType.WELCOME)

        verify(router).navigateTo(
            AboutCashBackTargetScreen(
                AboutCashBackDialogArguments(currentScreen, AboutCashBackInfoTypeArgument.Welcome)
            )
        )
    }

    @Test
    fun `navigate to payment system cashback about screen`() {
        presenter.onActionClick(PossibleCashbackVo.CashbackType.MASTERCARD)

        verify(router).navigateTo(
            AboutCashBackTargetScreen(
                AboutCashBackDialogArguments(currentScreen, AboutCashBackInfoTypeArgument.MasterCard)
            )
        )
    }

    @Test
    fun `navigate to growing cashback action screen`() {
        presenter.onActionClick(PossibleCashbackVo.CashbackType.GROWING)

        verify(router).navigateTo(GrowingCashbackTargetScreen())
    }

    @Test
    fun `navigate to yandex bank screen`() {
        presenter.onActionClick(PossibleCashbackVo.CashbackType.YANDEX_CARD)

        verify(router).navigateTo(YandexBankTargetScreen(YandexBankArguments(currentScreen)))
    }

    @Test
    fun `send payment system cashback shown analytics on first attach`() {
        whenever(vo.cashbackType) doReturn PossibleCashbackVo.CashbackType.MASTERCARD
        presenter.attachView(viewState)

        verify(cashbackInformerAnalyticFacade).sendMastercardInformerShown(currentScreen)
    }

    @Test
    fun `send welcome cashback shown analytics on first attach`() {
        whenever(vo.cashbackType) doReturn PossibleCashbackVo.CashbackType.WELCOME
        presenter.attachView(viewState)

        verify(welcomeCashbackAnalytics).thresholdShown(vo)
    }

    @Test
    fun `send growing cashback shown analytics on first attach`() {
        whenever(vo.cashbackType) doReturn PossibleCashbackVo.CashbackType.GROWING
        whenever(vo.hasYandexPlus) doReturn null
        whenever(vo.isThresholdReached) doReturn true
        presenter.attachView(viewState)

        verify(growingCashbackAnalytics).cartWidgetVisible(
            isLoggedIn = false,
            isPlusUser = false,
            isThresholdReached = true
        )
    }

    @Test
    fun `send yandex card cashback shown analytics on first attach`() {
        whenever(vo.cashbackType) doReturn PossibleCashbackVo.CashbackType.YANDEX_CARD
        presenter.attachView(viewState)

        verify(possibleCashbackAgitationAnalytics).cartPromoShown(vo.additionalAnalyticsInfo)
    }

    @Test
    fun `send growing navigate analytics on button click`() {
        whenever(vo.cashbackType) doReturn PossibleCashbackVo.CashbackType.GROWING
        whenever(vo.hasYandexPlus) doReturn true
        whenever(vo.isThresholdReached) doReturn false
        presenter.onActionClick(PossibleCashbackVo.CashbackType.GROWING)

        verify(growingCashbackAnalytics).cartWidgetNavigate(
            isLoggedIn = true,
            isPlusUser = true,
            isThresholdReached = false
        )
    }

    @Test
    fun `send promo navigate analytics on yandex card promo click`() {
        val analyticsInfo = possibleCashbackVo_AdditionalAnalyticsInfoTestInstance()
        whenever(vo.additionalAnalyticsInfo) doReturn analyticsInfo
        presenter.onActionClick(PossibleCashbackVo.CashbackType.YANDEX_CARD)

        verify(possibleCashbackAgitationAnalytics).cartPromoNavigate(analyticsInfo)
    }
}
