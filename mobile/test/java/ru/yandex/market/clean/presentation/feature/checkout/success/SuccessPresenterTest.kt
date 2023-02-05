package ru.yandex.market.clean.presentation.feature.checkout.success

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.CheckoutSuccessOrderAnalytics
import ru.yandex.market.analytics.facades.CheckoutSuccessSurveyAnalytics
import ru.yandex.market.analytics.facades.CreditInfoAnalytics
import ru.yandex.market.analytics.facades.MiscellaneousAnalyticsFacade
import ru.yandex.market.analytics.facades.ReferralProgramAnalytics
import ru.yandex.market.analytics.facades.SuccessSponsoredBannerAnalyticsFacade
import ru.yandex.market.clean.domain.model.OrderCoins
import ru.yandex.market.clean.domain.model.PackType
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.domain.model.newCoinsPackTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.success.SuccessCashbackFormatter
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandAnalyticFacade
import ru.yandex.market.clean.presentation.feature.smartshopping.HorizontalSmartCoinFormatter
import ru.yandex.market.clean.presentation.feature.smartshopping.choose.ChooseSmartCoinFragment
import ru.yandex.market.clean.presentation.feature.smartshopping.choose.ChooseSmartCoinTargetScreen
import ru.yandex.market.clean.presentation.feature.smartshopping.newcoin.NewSmartCoinsArgs
import ru.yandex.market.clean.presentation.feature.smartshopping.newcoin.NewSmartCoinsScreenType
import ru.yandex.market.clean.presentation.feature.smartshopping.newcoin.NewSmartCoinsTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.feature.manager.BannerThanksFeatureManager
import ru.yandex.market.feature.manager.CheckoutCesOnSuccessScreenFeatureManager
import ru.yandex.market.feature.manager.CheckoutSummaryServicesCalculationFeatureManager
import ru.yandex.market.internal.EventBus
import ru.yandex.market.presentationSchedulersMock

class SuccessPresenterTest {

    private val params = mock<SuccessParams>()
    private val useCases = mock<SuccessUseCases> {
        on { getAuthenticationStatusStream() } doReturn Observable.just(true)
    }
    private val successFormatter = mock<SuccessFormatter>()
    private val horizontalSmartCoinFormatter = mock<HorizontalSmartCoinFormatter>()
    private val smartCoinsInfoHeaderFormatter = mock<SmartCoinsInfoHeaderFormatter>()
    private val successCashbackFormatter = mock<SuccessCashbackFormatter>()
    private val successAdfoxBannerFormatter = mock<SuccessAdfoxBannerFormatter>()
    private val successBnplFormatter = mock<SuccessBnplFormatter>()
    private val router = mock<Router>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val miscellaneousAnalyticsFacade = mock<MiscellaneousAnalyticsFacade>()
    private val eventBus = mock<EventBus>()
    private val metricaSender = mock<MetricaSender>()
    private val creditInfoAnalytics = mock<CreditInfoAnalytics>()
    private val schedulers = presentationSchedulersMock()
    private val checkoutSuccessOrderAnalytics = mock<CheckoutSuccessOrderAnalytics>()
    private val checkoutSuccessSurveyAnalytics = mock<CheckoutSuccessSurveyAnalytics>()
    private val referralProgramAnalytics = mock<ReferralProgramAnalytics>()
    private val onDemandAnalyticFacade = mock<OnDemandAnalyticFacade>()
    private val bannerThanksFeatureManager = mock<BannerThanksFeatureManager>()
    private val cesOnSuccessScreenFeatureManager = mock<CheckoutCesOnSuccessScreenFeatureManager>()
    private val summaryServicesCalculationFeatureManager = mock<CheckoutSummaryServicesCalculationFeatureManager>()
    private val successSponsoredBannerAnalyticsFacade = mock<SuccessSponsoredBannerAnalyticsFacade>()

    private val presenter = SuccessPresenter(
        schedulers,
        params,
        useCases,
        successFormatter,
        router,
        horizontalSmartCoinFormatter,
        smartCoinsInfoHeaderFormatter,
        successCashbackFormatter,
        successAdfoxBannerFormatter,
        successBnplFormatter,
        analyticsService,
        eventBus,
        metricaSender,
        checkoutSuccessOrderAnalytics,
        referralProgramAnalytics,
        creditInfoAnalytics,
        checkoutSuccessSurveyAnalytics,
        miscellaneousAnalyticsFacade,
        onDemandAnalyticFacade,
        bannerThanksFeatureManager,
        successSponsoredBannerAnalyticsFacade,
        cesOnSuccessScreenFeatureManager,
        summaryServicesCalculationFeatureManager,
    )

    @Test
    fun `Open choose bonus popup if order coins allowed choose`() {
        val orderCoin = SmartCoin.testInstance()
        val orderCoins = OrderCoins(listOf(orderCoin), listOf(orderCoin))
        val coinsPack = newCoinsPackTestInstance(packType = PackType.CHOOSE_COIN)
        whenever(useCases.getOrderCoins(any())).thenReturn(Observable.just(orderCoins))
        whenever(useCases.saveOrderCoinsPack(orderCoins)).thenReturn(Single.just(coinsPack))

        presenter.observeOrderCoins()
        verify(router).navigateTo(
            ChooseSmartCoinTargetScreen(
                args = ChooseSmartCoinFragment.Arguments(coinsPack.packId)
            )
        )
    }

    @Test
    fun `Do not navigate to any bonus popup if order coins not allowed choose`() {
        val orderCoin = SmartCoin.testInstance()
        val orderCoins = OrderCoins(newCoins = listOf(orderCoin), chooseFromCoins = listOf())
        val coinsPack = newCoinsPackTestInstance(packType = PackType.NEW_COINS)
        whenever(useCases.getOrderCoins(any())).thenReturn(Observable.just(orderCoins))
        whenever(useCases.saveOrderCoinsPack(orderCoins)).thenReturn(Single.just(coinsPack))

        presenter.observeOrderCoins()
        verify(router, never()).navigateTo(
            NewSmartCoinsTargetScreen(
                args = NewSmartCoinsArgs(coinsPack.packId, NewSmartCoinsScreenType.ORDER)
            )
        )
        verify(router, never()).navigateTo(
            ChooseSmartCoinTargetScreen(
                args = ChooseSmartCoinFragment.Arguments(coinsPack.packId)
            )
        )
    }
}
