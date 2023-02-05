package ru.yandex.market.clean.presentation.feature.cms.item

import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.web.MarketWebParams
import ru.yandex.market.activity.web.MarketWebTargetScreen
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.ActualOrderSnippetAnalytics
import ru.yandex.market.analytics.facades.CmsActualOrdersAnalytics
import ru.yandex.market.analytics.facades.EatsRetailAnalytics
import ru.yandex.market.analytics.facades.FmcgAnalytics
import ru.yandex.market.analytics.facades.LavkaAnalytics
import ru.yandex.market.analytics.facades.LiveStreamAnalytics
import ru.yandex.market.analytics.facades.health.BannerNavigationHealthFacade
import ru.yandex.market.analytics.facades.health.CategoryNavigationHealthFacade
import ru.yandex.market.analytics.facades.health.DeliveryOptionsHealthFacade
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.analytics.health.HealthPortion
import ru.yandex.market.analytics.offer.AlternativeOffersAnalyticsFacade
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.analytics.speed.SpeedService
import ru.yandex.market.clean.domain.model.cms.AlternativeOffersLinkAnalyticsData
import ru.yandex.market.clean.domain.model.cms.AlternativeOffersLinkArguments
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.domain.model.cms.IconType
import ru.yandex.market.clean.domain.model.cms.NavigateAction
import ru.yandex.market.clean.domain.model.cms.WidgetData
import ru.yandex.market.clean.domain.model.cms.garson.ActualOrdersGarson
import ru.yandex.market.clean.presentation.feature.cms.item.button.TargetScreenMapper
import ru.yandex.market.clean.presentation.feature.cms.model.CmsButtonItemVo
import ru.yandex.market.clean.presentation.feature.cms.model.LinkButtonVo
import ru.yandex.market.clean.presentation.feature.cms.model.cmsBannerVoTestInstance
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandCourierScreenManager
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandHealthFacade
import ru.yandex.market.clean.presentation.feature.operationalrating.OperationalRatingFormatter
import ru.yandex.market.clean.presentation.feature.order.feedback.dialog.OrderFeedbackQuestionAnalytics
import ru.yandex.market.clean.presentation.feature.orderfeedback.OrderFeedbackHealthFacade
import ru.yandex.market.clean.presentation.feature.sku.multioffer.MultiOfferAnalyticsParam
import ru.yandex.market.clean.presentation.formatter.ProductOrderTypeFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.feature.timer.ui.TimersProvider
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.realtimesignal.RealtimeSignalDelegateWithWidget
import ru.yandex.market.ui.view.mvp.cartcounterbutton.OffersCache
import ru.yandex.market.util.manager.InstalledApplicationManager

class WidgetPresenterTest {

    private val widget = mock<CmsWidget>()
    private val useCases = mock<WidgetUseCases> {
        on { getData(any(), any(), any(), anyOrNull(), any(), anyOrNull()) } doReturn Observable.just(WidgetData.EMPTY)
        on { getAuthStatusStream() } doReturn Observable.just(false)
        on { getRefreshEventsStream() } doReturn Observable.just(Unit)
        on { isTrustFeatureToggleEnabled() } doReturn Single.just(false)
        on { getSessionPageViewUniqueId(any()) } doReturn Single.just(Optional.empty())
    }
    private val eatsRetailAnalytics = mock<EatsRetailAnalytics>()
    private val cmsWidgetDataMapper = mock<Lazy<CmsWidgetDataMapper>>()
    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val view = mock<WidgetView>()
    private val cartCounterOffersCache = mock<OffersCache>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val metricaSender = mock<MetricaSender>()
    private val cmsActualOrdersAnalyticsData = mock<CmsActualOrdersAnalytics>()
    private val actualOrderSnippetAnalytics = mock<ActualOrderSnippetAnalytics>()
    private val flashSaleTimersProvider = mock<TimersProvider>()
    private val targetScreenMapper = mock<TargetScreenMapper>()
    private val schedulers = presentationSchedulersMock()
    private val onDemandCourierScreenManager = mock<OnDemandCourierScreenManager>()
    private val liveStreamAnalytics = mock<LiveStreamAnalytics>()
    private val orderFeedbackQuestionAnalytics = mock<OrderFeedbackQuestionAnalytics>()
    private val lavkaAnalytics = mock<LavkaAnalytics>()
    private val installedApplicationManager = mock<InstalledApplicationManager>()
    private val onDemandHealthFacade = mock<OnDemandHealthFacade>()
    private val operationalRatingFormatter = mock<OperationalRatingFormatter>()
    private val productOrderTypeFormatter = mock<ProductOrderTypeFormatter>()
    private val speedService = mock<SpeedService>()
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val realtimeSignalDelegate = mock<RealtimeSignalDelegateWithWidget>()
    private val widgetDataFlow = mock<WidgetDataFlow>()
    private val orderFeedbackHealthFacade = mock<OrderFeedbackHealthFacade>()
    private val categoryNavigationHealthFacade = mock<Lazy<CategoryNavigationHealthFacade>>()
    private val bannerNavigationHealthFacade = mock<BannerNavigationHealthFacade>()
    private val bannerNavigationHealthFacadeLazy = Lazy { bannerNavigationHealthFacade }
    private val deliveryOptionsHealthFacade = mock<Lazy<DeliveryOptionsHealthFacade>>()
    private val fmcgAnalyticsFacade = mock<FmcgAnalytics>()
    private val dateTimeProvider = mock<DateTimeProvider>()
    private val alternativeOffersAnalyticsFacade = mock<AlternativeOffersAnalyticsFacade>()

    private val presenter = WidgetPresenter(
        schedulers,
        widget,
        speedService,
        router,
        useCases,
        cmsWidgetDataMapper,
        analyticsService,
        cartCounterOffersCache,
        resourcesDataStore,
        flashSaleTimersProvider,
        metricaSender,
        cmsActualOrdersAnalyticsData,
        actualOrderSnippetAnalytics,
        targetScreenMapper,
        installedApplicationManager,
        liveStreamAnalytics,
        eatsRetailAnalytics,
        orderFeedbackQuestionAnalytics,
        onDemandCourierScreenManager,
        lavkaAnalytics,
        onDemandHealthFacade,
        offerAnalyticsFacade,
        operationalRatingFormatter,
        productOrderTypeFormatter,
        realtimeSignalDelegate,
        widgetDataFlow,
        orderFeedbackHealthFacade,
        fmcgAnalyticsFacade,
        categoryNavigationHealthFacade,
        bannerNavigationHealthFacadeLazy,
        deliveryOptionsHealthFacade,
        dateTimeProvider,
        alternativeOffersAnalyticsFacade,
    )

    @Test
    fun `Not send empty widget report for actual orders widget`() {
        whenever(widget.garsons()).thenReturn(listOf(ActualOrdersGarson(emptyList())))
        presenter.attachView(view)

        verify(analyticsService, never()).report(argThat<HealthEvent> { name == HealthName.EMPTY_WIDGET })
    }

    @Test
    fun `Health event sent if banner has bad link`() {
        val badLink = "bad link"
        val exception = RuntimeException()
        whenever(useCases.mapUrlToDeepLinkAndReportBannerClick(badLink)).thenReturn(Single.error(exception))
        val bannerVo = cmsBannerVoTestInstance(link = badLink)
        presenter.onBannerClicked(bannerVo)

        verify(bannerNavigationHealthFacade).onTransitionFailed(exception, badLink, bannerVo.id, PORTION)
    }

    @Test
    fun `Nothing happened when banner link is empty`() {
        val bannerVo = cmsBannerVoTestInstance(link = "")
        presenter.onBannerClicked(bannerVo)

        verify(useCases, never()).mapUrlToDeepLinkAndReportBannerClick(any())
        verify(analyticsService, never()).report(any<HealthEvent>())
    }

    @Test
    fun `Navigate to Market Web Screen when click on link button`() {
        val url = "https://test.url.com"
        presenter.onItemClicked(LinkButtonVo("button", "test", url, IconType.BASKET), false)

        verify(router).navigateTo(
            MarketWebTargetScreen(
                MarketWebParams(url = url)
            )
        )
    }

    @Test
    fun `Alternative offer navigate event`() {
        val analyticsData = mock<AlternativeOffersLinkAnalyticsData>()
        presenter.onButtonClicked(
            CmsButtonItemVo(
                "",
                NavigateAction(AlternativeOffersLinkArguments("", "", "", 0, "", analyticsData)),
                null
            )
        )
        verify(offerAnalyticsFacade).allAlternativeOffersNavigateEvent(analyticsData, CURRENT_SCREEN)
    }

    @Test
    fun `Alternative offer visible event`() {
        val params = mock<MultiOfferAnalyticsParam>()
        presenter.alternativeOfferItemVisibleEvent(params)
        verify(alternativeOffersAnalyticsFacade).alternativeOfferItemVisibleEvent(
            isSkuScreenParent = true,
            params = params,
            screen = CURRENT_SCREEN,
            isExclusive = null,
            isNovice = null
        )
    }

    companion object {
        private val CURRENT_SCREEN = Screen.HOME
        private val PORTION = HealthPortion.PROMO_SCREEN
    }

}
