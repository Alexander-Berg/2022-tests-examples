package ru.yandex.market.clean.presentation.feature.live

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.activity.web.MarketWebParams
import ru.yandex.market.activity.web.MarketWebTargetScreen
import ru.yandex.market.analytics.facades.LiveStreamAnalytics
import ru.yandex.market.analytics.facades.OfferEventData
import ru.yandex.market.analytics.facades.health.LiveStreamHealthFacade
import ru.yandex.market.analytics.mapper.OfferEventDataMapper
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.clean.domain.model.livestream.LiveStreamContent
import ru.yandex.market.clean.domain.model.livestream.LiveStreamPromo
import ru.yandex.market.clean.domain.model.livestream.LiveStreamSource
import ru.yandex.market.clean.domain.model.sku.DetailedSku
import ru.yandex.market.clean.domain.model.sku.detailedSkuTestInstance
import ru.yandex.market.clean.presentation.feature.cms.HomeTargetScreen
import ru.yandex.market.clean.presentation.feature.live.description.LiveStreamDescriptionDialogFragment
import ru.yandex.market.clean.presentation.feature.productindialog.ProductInBottomSheetFormatter
import ru.yandex.market.clean.presentation.feature.productindialog.ProductInBottomSheetVo
import ru.yandex.market.clean.presentation.feature.live.promo.LiveStreamPromoDialogFragment
import ru.yandex.market.clean.presentation.feature.live.vo.LiveStreamPreviewDateFormatter
import ru.yandex.market.clean.presentation.feature.live.vo.LiveStreamScreenStateFormatter
import ru.yandex.market.clean.presentation.feature.live.vo.LiveStreamSnackbarFormatter
import ru.yandex.market.clean.presentation.feature.live.vo.LiveStreamSnackbarType
import ru.yandex.market.clean.presentation.feature.live.vo.LiveStreamSnackbarVo
import ru.yandex.market.feature.videosnippets.ui.formatter.TranslationViewersFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.clean.presentation.notifications.NotificationsManager
import ru.yandex.market.common.featureconfigs.models.SupportChatterboxConfig
import ru.yandex.market.domain.product.model.ProductId
import ru.yandex.market.domain.product.model.SkuId
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.common.errors.MetricErrorInfoMapper
import ru.yandex.market.feature.videosnippets.ui.providers.LiveStreamTimerProviderImpl
import ru.yandex.market.feature.videosnippets.ui.vo.LiveStreamStateVo
import ru.yandex.market.utils.Duration
import java.util.Date

class LiveStreamFlowPresenterTest {

    private val liveStreamProductVo = mock<ProductInBottomSheetVo>()
    private val liveStreamStateVo = mock<LiveStreamStateVo.Record>()
    private val liveStreamSnackBarVo = mock<LiveStreamSnackbarVo>()

    private val view = mock<LiveStreamView>()

    private val schedulers = presentationSchedulersMock()
    private val args = mock<LiveStreamArguments> {
        on { source } doReturn LiveStreamSource.MORDA
    }
    private val previewDateFormatter = mock<LiveStreamPreviewDateFormatter>()
    private val screenStateFormatter = mock<LiveStreamScreenStateFormatter>()
    private val useCases = mock<LiveStreamFlowUseCases> {
        on { observeLiveStreamSubscription(DUMMY_SEMANTIC) } doReturn Observable.just(false)
        on { isNotificationsEnabled(NotificationsManager.LIVE_STREAM_CHANNEL_ID) } doReturn true
    }
    private val router = mock<Router>()
    private val liveStreamTimerProvider = mock<LiveStreamTimerProviderImpl> {
        on { create(TRANSLATION_START_TIME, DURATION, DUMMY_TOTAL_VIEWS) } doReturn Observable.just(liveStreamStateVo)
    }
    private val liveStreamProductFormatter = mock<ProductInBottomSheetFormatter>()

    @Suppress("DEPRECATION")
    private val dateFormatter = mock<DateFormatter>()
    private val liveStreamAnalytics = mock<LiveStreamAnalytics>()
    private val offerEventDataMapper = mock<OfferEventDataMapper>()
    private val liveStreamSnackbarFormatter = mock<LiveStreamSnackbarFormatter>()
    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val metricErrorInfoMapper = mock<MetricErrorInfoMapper>()
    private val viewersFormatter = mock<TranslationViewersFormatter>()
    private val liveStreamHealthFacade = mock<LiveStreamHealthFacade>()

    private val detailedSku = mock<DetailedSku>()

    private val promo = mock<LiveStreamPromo> {
        on { discount } doReturn DUMMY_DISCOUNT
        on { promo } doReturn DUMMY_PROMO_TEXT
        on { toDate } doReturn PROMO_EXPIRE_DATE
        on { shortLabel } doReturn DUMMY_PROMO_TEXT
    }

    private val presenter by lazy {
        LiveStreamFlowPresenter(
            schedulers,
            args,
            useCases,
            router,
            liveStreamTimerProvider,
            liveStreamProductFormatter,
            dateFormatter,
            liveStreamAnalytics,
            offerAnalyticsFacade,
            offerEventDataMapper,
            liveStreamSnackbarFormatter,
            previewDateFormatter,
            screenStateFormatter,
            metricErrorInfoMapper,
            viewersFormatter,
            liveStreamHealthFacade
        )
    }

    private val liveStreamContent = mock<LiveStreamContent> {
        on { chatId } doReturn DUMMY_CHAT_ID
        on { semanticId } doReturn DUMMY_SEMANTIC
        on { startTime } doReturn TRANSLATION_START_TIME
        on { duration } doReturn DURATION
        on { translationId } doReturn DUMMY_TRANSLATION_ID
        on { title } doReturn DUMMY_TITLE
        on { description } doReturn DUMMY_DESCRIPTION
    }

    @Test
    fun `Should load stream data if args has no stream data`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(liveStreamProductFormatter.format(detailedSku)).thenReturn(liveStreamProductVo)

        presenter.attachView(view)

        verify(useCases).resolveLiveStream(DUMMY_SEMANTIC)
    }

    @Test
    fun `Should enable pip mode if allowed`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(liveStreamProductFormatter.format(detailedSku)).thenReturn(liveStreamProductVo)
        whenever(useCases.isPipAvailable()).thenReturn(Single.just(true))

        presenter.attachView(view)
        presenter.onMinimizeClicked()

        verify(view).enablePipMode()
    }

    @Test
    fun `Should not enable pip mode if it's forbidden`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(liveStreamProductFormatter.format(detailedSku)).thenReturn(liveStreamProductVo)
        whenever(useCases.isPipAvailable()).thenReturn(Single.just(false))

        presenter.attachView(view)
        presenter.onMinimizeClicked()

        verify(view, never()).enablePipMode()
    }

    @Test
    fun `Should open promo info when clicked on report`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(useCases.getChatterboxConfigUseCase()).thenReturn(
            Single.just(
                SupportChatterboxConfig.SupportChatterboxConfigEnable(SUPPORT_CHAT_LINK)
            )
        )

        presenter.attachView(view)
        presenter.reportError()

        verify(router).navigateTo(
            MarketWebTargetScreen(
                MarketWebParams(url = SUPPORT_CHAT_LINK)
            )
        )
    }

    @Test
    fun `Should navigate to home screen when opened from deeplink and clicked back`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(args.source).thenReturn(LiveStreamSource.SERVICE)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(liveStreamProductFormatter.format(detailedSku)).thenReturn(liveStreamProductVo)

        presenter.attachView(view)
        presenter.navigateBack()

        verify(router).navigateTo(HomeTargetScreen())
    }

    @Test
    fun `Should invoke router back when opened not from morda and clicked back`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))

        presenter.attachView(view)
        presenter.navigateBack()

        verify(router).back()
    }

    @Test
    fun `Should show notification when on pause invoked`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(args.launchMode).thenReturn(LiveStreamLaunchMode.FRAGMENT)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))

        presenter.attachView(view)
        presenter.onParentPaused()

        verify(view).showNotification(
            liveStreamContent.title, liveStreamContent.description, SERVICE_DEEP_LINK
        )
    }

    @Test
    fun `Should show promo when on copy promo clicked`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(liveStreamContent.promo).thenReturn(promo)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(useCases.copyToClipboard(DUMMY_PROMO_TEXT)).thenReturn(Completable.complete())
        whenever(dateFormatter.formatHourInDay(PROMO_EXPIRE_DATE)).thenReturn(PROMO_EXPIRE_DATE_TEXT)

        presenter.attachView(view)
        presenter.copyPromo(DUMMY_PROMO_TEXT)

        verify(view).showPromoDialog(
            LiveStreamPromoDialogFragment.Arguments(
                DUMMY_PROMO_TEXT,
                promo.discount,
                PROMO_EXPIRE_DATE_TEXT,
                DUMMY_PROMO_TEXT,
                false
            )
        )
    }

    @Test
    fun `Should open product deeplink when product clicked and launched from activity and offed id empty`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(args.launchMode).thenReturn(LiveStreamLaunchMode.ACTIVITY)
        whenever(liveStreamProductVo.productId).thenReturn(ProductId.create(DUMMY_PRODUCT_ID, DUMMY_MODEL_ID, ""))
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(useCases.isPipAvailable()).thenReturn(Single.just(true))

        presenter.attachView(view)
        presenter.onProductClicked(liveStreamProductVo)

        verify(view).openProductWithDeeplink(PRODUCT_DEEP_LINK)
    }

    @Test
    fun `Should open offer deeplink when product clicked and launched from activity and offer id not null`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(args.launchMode).thenReturn(LiveStreamLaunchMode.ACTIVITY)
        whenever(liveStreamProductVo.productId).thenReturn(
            ProductId.create(
                DUMMY_PRODUCT_ID,
                DUMMY_MODEL_ID,
                DUMMY_OFFER_ID
            )
        )
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(useCases.isPipAvailable()).thenReturn(Single.just(true))

        presenter.attachView(view)
        presenter.onProductClicked(liveStreamProductVo)

        verify(view).openProductWithDeeplink(OFFER_DEEP_LINK)
    }

    @Test
    fun `Should open product bottom sheet when on product clicked and launched from fragment`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(args.launchMode).thenReturn(LiveStreamLaunchMode.FRAGMENT)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(liveStreamProductVo.productId).thenReturn(
            ProductId.create(
                DUMMY_PRODUCT_ID,
                DUMMY_MODEL_ID,
                DUMMY_OFFER_ID
            )
        )
        whenever(liveStreamProductVo.detailedSku).thenReturn(detailedSkuTestInstance())

        presenter.attachView(view)
        presenter.onProductClicked(liveStreamProductVo)

        verify(view).showProductBottomSheetDialog(
            liveStreamProductVo.productId,
            liveStreamProductVo.detailedSku.sku.productOffer?.cpc.orEmpty()
        )
    }

    @Test
    fun `Should open description when description clicked`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))

        presenter.attachView(view)
        presenter.onDescriptionClicked()

        verify(view).showDescriptionDialog(
            LiveStreamDescriptionDialogFragment.Arguments(
                title = DUMMY_TITLE,
                description = DUMMY_DESCRIPTION,
                semanticId = DUMMY_SEMANTIC
            )
        )
    }

    @Test
    fun `Should not open description when description clicked and description is empty`() {
        whenever(liveStreamContent.description).thenReturn("")
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))

        presenter.attachView(view)
        presenter.onDescriptionClicked()

        verify(view, never()).showDescriptionDialog(any())
    }

    @Test
    fun `Should not open description when description clicked and description is null`() {
        whenever(liveStreamContent.description).thenReturn(null)
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))

        presenter.attachView(view)
        presenter.onDescriptionClicked()

        verify(view, never()).showDescriptionDialog(any())
    }

    @Test
    fun `Should schedule notification when subscribe clicked`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(useCases.addScheduledNotification(any())).thenReturn(Completable.complete())
        whenever(liveStreamSnackbarFormatter.format(LiveStreamSnackbarType.SUBSCRIBED)).thenReturn(liveStreamSnackBarVo)

        presenter.attachView(view)
        presenter.onSubscribeClicked()

        verify(liveStreamAnalytics).onSubscribeClicked(DUMMY_SEMANTIC)
        verify(liveStreamSnackbarFormatter).format(LiveStreamSnackbarType.SUBSCRIBED)
        verify(view).scheduleNotification(any(), any())
    }

    @Test
    fun `Should cancel notification when subscribe clicked and user already subscribed`() {
        whenever(useCases.observeLiveStreamSubscription(DUMMY_SEMANTIC)).thenReturn(Observable.just(true))
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(useCases.removeScheduledNotification(DUMMY_SEMANTIC)).thenReturn(Completable.complete())
        whenever(liveStreamSnackbarFormatter.format(LiveStreamSnackbarType.UNSUBSCRIBED))
            .thenReturn(liveStreamSnackBarVo)

        presenter.attachView(view)
        presenter.onSubscribeClicked()

        verify(liveStreamAnalytics).onUnsubscribeClicked(DUMMY_SEMANTIC)
        verify(liveStreamSnackbarFormatter).format(LiveStreamSnackbarType.UNSUBSCRIBED)
        verify(view).cancelNotification(any(), any())
    }

    @Test
    fun `Test broadcast Snippet Click`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(liveStreamContent.promo).thenReturn(promo)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(liveStreamProductVo.productId).thenReturn(
            ProductId.create(
                DUMMY_PRODUCT_ID,
                DUMMY_MODEL_ID,
                DUMMY_OFFER_ID
            )
        )
        whenever(args.launchMode).thenReturn(LiveStreamLaunchMode.FRAGMENT)

        presenter.attachView(view)

        val skuId = mock<SkuId> {
            on { id } doReturn "id"
        }
        val vo = mock<ProductInBottomSheetVo> {
            on { productId } doReturn skuId
            on { detailedSku } doReturn detailedSkuTestInstance()
        }
        presenter.onProductClicked(vo)
        verify(offerAnalyticsFacade).broadcastSnippetClick(DUMMY_SEMANTIC, DUMMY_PROMO_TEXT, "id", null)
    }

    @Test
    fun `Test broadcast Snippet Visible`() {
        whenever(args.semanticId).thenReturn(DUMMY_SEMANTIC)
        whenever(liveStreamContent.promo).thenReturn(promo)
        whenever(useCases.resolveLiveStream(DUMMY_SEMANTIC)).thenReturn(Single.just(liveStreamContent))
        whenever(router.currentScreen).thenReturn(Screen.HOME)
        whenever(liveStreamProductVo.productId).thenReturn(
            ProductId.create(
                DUMMY_PRODUCT_ID,
                DUMMY_MODEL_ID,
                DUMMY_OFFER_ID
            )
        )
        whenever(args.launchMode).thenReturn(LiveStreamLaunchMode.FRAGMENT)
        val detailedSkuMock = detailedSkuTestInstance()
        val skuId = mock<SkuId> {
            on { id } doReturn "id"
        }
        val vo = mock<ProductInBottomSheetVo> {
            on { productId } doReturn skuId
            on { detailedSku } doReturn detailedSkuMock
        }
        val eventData = mock<OfferEventData>()
        whenever(offerEventDataMapper.map(detailedSkuMock)).thenReturn(eventData)

        presenter.attachView(view)

        presenter.onProductShown(vo)
        verify(offerAnalyticsFacade).broadcastSnippetVisible(
            DUMMY_SEMANTIC,
            eventData,
            DUMMY_PROMO_TEXT,
            "id",
            null,
            Screen.HOME
        )
    }

    private companion object {
        const val DUMMY_SEMANTIC = "DUMMY_SEMANTIC"
        const val DUMMY_PRODUCT_ID = "0"
        const val DUMMY_TITLE = "DUMMY_TITLE"
        const val DUMMY_DESCRIPTION = "DUMMY_DESCRIPTION"

        const val DUMMY_MODEL_ID = "DUMMY_MODEL_ID"
        const val DUMMY_OFFER_ID = "DUMMY_OFFER_ID"

        const val DUMMY_CHAT_ID = "DUMMY_CHAT_ID"
        const val DUMMY_TRANSLATION_ID = "DUMMY_TRANSLATION_ID"

        const val DUMMY_PROMO_TEXT = "DUMMY_PROMO"

        val TRANSLATION_START_TIME = Date(10L)
        val PROMO_EXPIRE_DATE = Date(20L)
        const val PROMO_EXPIRE_DATE_TEXT = "PROMO_EXPIRE_DATE_TEXT"
        val DURATION = Duration(1.0)

        const val DUMMY_DISCOUNT = 10
        const val DUMMY_TOTAL_VIEWS = 0
        const val SERVICE_DEEP_LINK = "beru://live/DUMMY_SEMANTIC?source=service"
        const val SUPPORT_CHAT_LINK = "https://help-frontend.taxi.yandex.ru/ecom/yandex/yp/ru_ru/market/chat"
        const val PRODUCT_DEEP_LINK = "product/$DUMMY_PRODUCT_ID"
        const val OFFER_DEEP_LINK = "product/$DUMMY_PRODUCT_ID?offerid=$DUMMY_OFFER_ID"
    }

}