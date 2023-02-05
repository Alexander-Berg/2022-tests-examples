package ru.yandex.market.clean.presentation.feature.tabs

import android.net.Uri
import android.os.Build
import com.annimon.stream.OptionalBoolean
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.activity.main.MainUseCases
import ru.yandex.market.activity.main.mapper.TabNavigatorToTabDomainMapper
import ru.yandex.market.analytics.facades.FmcgAnalytics
import ru.yandex.market.analytics.facades.HomeAnalytics
import ru.yandex.market.analytics.facades.LavkaAnalytics
import ru.yandex.market.analytics.facades.LoyaltyNotificationsAnalytics
import ru.yandex.market.analytics.facades.MiscellaneousAnalyticsFacade
import ru.yandex.market.analytics.facades.ReviewAnalytics
import ru.yandex.market.analytics.facades.TabsAnalytics
import ru.yandex.market.analytics.facades.health.LavkaHealthFacade
import ru.yandex.market.analytics.speed.SpeedService
import ru.yandex.market.clean.data.mapper.UserReviewMapper
import ru.yandex.market.clean.domain.model.SmartCoinsCount
import ru.yandex.market.clean.domain.model.auth.AuthCheckingResult
import ru.yandex.market.clean.domain.model.lavka2.LavkaBadgeState
import ru.yandex.market.clean.domain.model.notificationTestInstance
import ru.yandex.market.clean.domain.model.order.ConsultationsUnreadMessageCount
import ru.yandex.market.clean.domain.model.vpn.VpnState
import ru.yandex.market.clean.presentation.feature.cart.formatter.CartItemsCountFormatter
import ru.yandex.market.clean.presentation.feature.lavka.badge.LavkaBadgeFormatter
import ru.yandex.market.clean.presentation.feature.lavka.snackbar.LavkaCartSnackbarFormatter
import ru.yandex.market.clean.presentation.feature.onboarding.manager.PopupManager
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Tab
import ru.yandex.market.common.experiments.manager.ExperimentManager
import ru.yandex.market.common.featureconfigs.managers.OrderConsultationToggleManager
import ru.yandex.market.common.featureconfigs.managers.VpnNotificationToggleManager
import ru.yandex.market.common.featureconfigs.managers.WebViewWhiteListConfigManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.models.WebViewWhiteListConfig
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.deeplinks.links.BrowserDeeplink
import ru.yandex.market.domain.reviews.model.GradeUserReview
import ru.yandex.market.domain.reviews.model.Review
import ru.yandex.market.domain.reviews.model.ReviewSource
import ru.yandex.market.domain.reviews.model.UsagePeriod
import ru.yandex.market.feature.updater.InstallStatus
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.advanceTimeBy
import ru.yandex.market.utils.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TabsPresenterTest {

    private val router = mock<Router>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val miscellaneousAnalyticsFacade = mock<MiscellaneousAnalyticsFacade>()
    private val healthAnalyticsSender = mock<SpeedService>()
    private val installState = mock<InstallStatus>()
    private val tabsAnalytics = mock<TabsAnalytics>()
    private val reviewMapper = mock<UserReviewMapper>()
    private val reviewData = mock<ReviewAgitationDataVo> {
        on { modelId } doReturn DUMMY_LONG_ID
        on { modelName } doReturn DUMMY_STRING_NAME
        on { rating } doReturn DUMMY_RATING
        on { cashback } doReturn DUMMY_CASHBACK
        on { categoryId } doReturn DUMMY_STRING_ID
    }

    private val previousReview = mock<Review> {
        on { usagePeriod } doReturn UsagePeriod.FEW_WEEKS
    }

    private val useCases = mock<MainUseCases> {
        on { unlockShowingRateMe() } doReturn Completable.complete()
        on { getCoinsCountStream() } doReturn Observable.just(SmartCoinsCount.createZero())
        on { getCoinsShownStream() } doReturn Observable.empty()
        on { shouldShowGdprNotification() } doReturn Single.just(false)
        on { checkAutoLogin(any()) } doReturn Single.just(AuthCheckingResult.NOT_NEED_TO_DO_ANYTHING)
        on { shouldRequestUpdate() } doReturn Observable.just(true)
        on { installStatusChangesStream() } doReturn Observable.just(installState)
        on { notifyUpdateFailed() } doReturn Completable.complete()
        on { notifyAskedForUpdate() } doReturn Completable.complete()
        on { getCartItemsStream() } doReturn Observable.just(Optional.empty())
        on { getSelectedByUserCartItemDataStream() } doReturn Observable.just(emptyList())
        on { getCartValidationResultStream() } doReturn Observable.just(Optional.empty())
        on { updateSupportPhoneNumber() } doReturn Completable.complete()
        on { getAuthenticationStatusStream() } doReturn Observable.just(false)
        on { prepareGooglePayAvailability() } doReturn Completable.complete()
        on { getUserPublicationBadgeNeedShown() } doReturn Observable.just(OptionalBoolean.empty())
        on { getAllConsultationsUnreadMessageCountStream() } doReturn Observable.just(
            ConsultationsUnreadMessageCount(0, 0, 0)
        )
        on { observeReferralProgramNewsBadge() } doReturn Observable.just(false)
        on { setUserInteractedWithAgitation() } doReturn Completable.complete()
        on { clearOutdatedTranslationsData() } doReturn Completable.complete()
        on { getRegionObservable() } doReturn Observable.empty()
        on { getNotificationsStream() } doReturn Observable.never()
        on { observeEcomQuestionTriggerEvent() } doReturn Observable.never()
        on { observeIsExpressEntryPoint() } doReturn Observable.just(false)
        on { observeLavkaCartItems() } doReturn Observable.just(emptyList())
        on { observeLavkaCartErrors() } doReturn Observable.never()
        on { observeLavkaBadgeState() } doReturn Observable.just(LavkaBadgeState.NoBadge)
        on { observeFmcgTabEnabled() } doReturn Observable.just(false)
        on { getVpnStateStream() } doReturn Observable.just(VpnState.NO_VPN)
    }
    private val view = mock<TabsView>()
    private val gdprTimeout = 15.seconds
    private val hideSmartShoppingHintTimeout = 10.seconds
    private val experimentManager = mock<ExperimentManager>()
    private val cartItemsCountFormatter = mock<CartItemsCountFormatter>()
    private val notificationFormatter = mock<NotificationFormatter>()
    private val loyaltyNotificationsAnalytics = mock<LoyaltyNotificationsAnalytics>()
    private val timerScheduler = TestScheduler()
    private val schedulers = presentationSchedulersMock {
        on { timer } doReturn timerScheduler
    }

    private val reviewAnalytcs = mock<ReviewAnalytics>()
    private val homeAnalytics = mock<HomeAnalytics>()

    private val orderConsultationToggleManager = mock<OrderConsultationToggleManager> {
        on { getSingle() } doReturn Single.just(FeatureToggle(isEnabled = false))
    }

    private val webViewWhiteListConfigManager = mock<WebViewWhiteListConfigManager> {
        on { get() } doReturn WebViewWhiteListConfig(isEnabled = true, hosts = listOf("yandex.ru"))
    }

    private val vpnNotificationToggleManager = mock<VpnNotificationToggleManager> {
        on { getSingle() } doReturn Single.just(FeatureToggle(isEnabled = false))
    }

    private val featureConfigProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.orderConsultationToggleManager) doReturn orderConsultationToggleManager
        whenever(provider.webViewWhiteListConfigManager) doReturn webViewWhiteListConfigManager
        whenever(provider.vpnNotificationToggleManager) doReturn vpnNotificationToggleManager
    }
    private val mockLavkaCartFormatter = mock<LavkaCartSnackbarFormatter>()
    private val lavkaHealthFacade = mock<LavkaHealthFacade>()
    private val lavkaBadgeFormatter = mock<LavkaBadgeFormatter>()
    private val lavkaAnalytics = mock<LavkaAnalytics>()
    private val fmcgAnalytics = mock<FmcgAnalytics>()
    private val popupManager = mock<PopupManager>()
    private val args = TabsFragment.Arguments(onboardingIdToShow = null, skipOnboarding = false, fromDeeplink = false)

    private val presenter = TabsPresenter(
        schedulers,
        router,
        useCases,
        analyticsService,
        miscellaneousAnalyticsFacade,
        healthAnalyticsSender,
        featureConfigProvider,
        cartItemsCountFormatter,
        reviewAnalytcs,
        tabsAnalytics,
        reviewMapper,
        TabNavigatorToTabDomainMapper(),
        notificationFormatter,
        loyaltyNotificationsAnalytics,
        mockLavkaCartFormatter,
        homeAnalytics,
        lavkaHealthFacade,
        lavkaAnalytics,
        fmcgAnalytics,
        lavkaBadgeFormatter,
        popupManager,
        args
    )

    @Test
    fun `Shows GDPR notification when needed`() {
        whenever(useCases.shouldShowGdprNotification()).thenReturn(Single.just(true))
        whenever(useCases.setGdprNotificationShown()).thenReturn(Completable.complete())
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        presenter.attachView(view)

        verify(view).showGdprNotification()
    }

    @Test
    fun `Don't show GDPR notification when not needed`() {
        whenever(useCases.shouldShowGdprNotification()).thenReturn(Single.just(false))
        whenever(useCases.setGdprNotificationShown()).thenReturn(Completable.complete())
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        presenter.attachView(view)

        verify(view, never()).showGdprNotification()
    }

    @Test
    fun `Mark GDPR notification shown when view says so`() {
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        whenever(useCases.shouldShowGdprNotification()).thenReturn(Single.just(true))
        whenever(useCases.setGdprNotificationShown()).thenReturn(Completable.complete())

        presenter.attachView(view)

        verify(view).showGdprNotification()
        verify(useCases).setGdprNotificationShown()
    }

    @Test
    fun `Show GDPR notification if use case execution timed out`() {
        whenever(useCases.shouldShowGdprNotification()).thenReturn(Single.never())
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())
        whenever(useCases.setGdprNotificationShown()).thenReturn(Completable.complete())

        presenter.attachView(view)
        timerScheduler.advanceTimeBy(gdprTimeout)

        verify(view).showGdprNotification()
    }

    @Test
    fun `Hide smartshopping hint after interval`() {
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        presenter.attachView(view)

        timerScheduler.advanceTimeBy(hideSmartShoppingHintTimeout)
    }

    @Test
    fun `Don't hide smartshopping hint before interval has gone`() {
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        presenter.attachView(view)

        val time = hideSmartShoppingHintTimeout.copy(value = hideSmartShoppingHintTimeout.value - 1)
        timerScheduler.advanceTimeBy(time)

    }

    @Test
    fun `Prepare google pay availability cache on first attach`() {
        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        presenter.attachView(view)

        verify(useCases).prepareGooglePayAvailability()
    }

    @Test
    fun `Should save agitation review when previous review do not exists`() {
        val expectedReviewFactors = listOf(GradeUserReview(DUMMY_RATING))

        whenever(useCases.sendReviewToLocalDataStore(DUMMY_STRING_ID, expectedReviewFactors))
            .thenReturn(Completable.complete())

        whenever(useCases.sendReview(DUMMY_STRING_ID, expectedReviewFactors, ReviewSource.Agitation))
            .thenReturn(Completable.complete())

        presenter.onAgitationRating(reviewData)

        verify(useCases).sendReviewToLocalDataStore(DUMMY_STRING_ID, expectedReviewFactors)
        verify(useCases).sendReview(DUMMY_STRING_ID, expectedReviewFactors, ReviewSource.Agitation)
    }

    @Test
    fun `Should not try to save review when previous review exists`() {
        whenever(reviewData.previousReview).thenReturn(previousReview)

        presenter.onReviewAgitationButtonClick(reviewData)

        verify(useCases, never()).sendReviewToLocalDataStore(any(), any())
        verify(useCases, never()).sendReview(any(), any(), any())
    }

    @Test
    fun `Should hide and delay remote agitation when closed`() {
        whenever(useCases.delayReviewAgitation(DUMMY_STRING_ID)).thenReturn(Completable.complete())
        whenever(useCases.setUserInteractedWithAgitation()).thenReturn(Completable.complete())

        presenter.cancelAgitation(DUMMY_STRING_ID, DUMMY_LONG_ID, DUMMY_CASHBACK)

        verify(reviewAnalytcs).closeAgitation(Tab.MAIN, DUMMY_LONG_ID, false)
        verify(useCases).delayReviewAgitation(DUMMY_STRING_ID)
        verify(useCases).setUserInteractedWithAgitation()
    }

    @Test
    fun `Should show loyalty notification`() {
        val notification = notificationTestInstance()
        val viewObject = notificationVoTestInstance()
        whenever(useCases.getNotificationsStream()).thenReturn(Observable.just(notification))
        whenever(notificationFormatter.format(notification)).thenReturn(viewObject)

        whenever(useCases.getWishListBadgeNeedShownStream()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getComparisonBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.getUserPublicationBadgeNeedShown()).thenReturn(Observable.just(OptionalBoolean.of(false)))
        whenever(useCases.initShortcuts()).thenReturn(Completable.complete())

        presenter.attachView(view)

        verify(view, times(1)).showNotification(viewObject)
    }

    @Test
    fun `Should set notification shown`() {
        val viewObject = notificationVoTestInstance()
        whenever(useCases.setNotificationIsShown(viewObject.id)).thenReturn(Completable.complete())
        presenter.onNotificationShown(viewObject)
        verify(useCases).setNotificationIsShown(viewObject.id)
    }

    @Test
    fun `Navigate from notification`() {
        val link = "https://yandex.ru"
        val viewObject = notificationVoTestInstance(link = link)
        val deeplink = BrowserDeeplink(Uri.parse(link), featureConfigProvider)
        whenever(useCases.setNotificationIsShown(viewObject.id)).thenReturn(Completable.complete())
        whenever(useCases.mapUrlToDeepLink(link)).thenReturn(Single.just(deeplink))
        presenter.onNotificationClick(viewObject)
        verify(router).navigateTo(deeplink.targetScreen)
    }

    private companion object {
        const val DUMMY_LONG_ID = 1L
        const val DUMMY_STRING_ID = "1"
        const val DUMMY_STRING_NAME = "DUMMY_STRING_NAME"
        const val DUMMY_RATING = 5
        const val DUMMY_CASHBACK = 0
    }
}
