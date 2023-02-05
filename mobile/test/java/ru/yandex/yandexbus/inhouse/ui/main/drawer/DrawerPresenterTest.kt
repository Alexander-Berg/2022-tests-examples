package ru.yandex.yandexbus.inhouse.ui.main.drawer

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.account.achievements.Achievement.AROUND_THE_WORLD
import ru.yandex.yandexbus.inhouse.account.achievements.Achievement.IRONMAN
import ru.yandex.yandexbus.inhouse.account.achievements.Achievement.LIKE_GAGARIN
import ru.yandex.yandexbus.inhouse.account.achievements.AchievementsModel
import ru.yandex.yandexbus.inhouse.badge.UnvisitedPromocodesNotificationSource
import ru.yandex.yandexbus.inhouse.common.error.AbortException
import ru.yandex.yandexbus.inhouse.feature.Feature.NEW_FROM_FEEDBACK
import ru.yandex.yandexbus.inhouse.feature.FeatureManager
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo
import ru.yandex.yandexbus.inhouse.navigation.RootNavigator
import ru.yandex.yandexbus.inhouse.navigation.ScreenChange
import ru.yandex.yandexbus.inhouse.navigation.ScreenChangesNotifier
import ru.yandex.yandexbus.inhouse.service.auth.Token
import ru.yandex.yandexbus.inhouse.service.auth.Uid
import ru.yandex.yandexbus.inhouse.service.auth.User
import ru.yandex.yandexbus.inhouse.service.auth.UserManager
import ru.yandex.yandexbus.inhouse.service.award.AwardService
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.transport2maps.showcase.card.ShowcaseCardService
import ru.yandex.yandexbus.inhouse.ui.main.drawer.DrawerContract.Action
import ru.yandex.yandexbus.inhouse.ui.main.drawer.DrawerContract.Action.*
import ru.yandex.yandexbus.inhouse.ui.main.drawer.DrawerContract.UserInfo
import ru.yandex.yandexbus.inhouse.ui.main.drawer.DrawerContract.ViewState
import ru.yandex.yandexbus.inhouse.whenever
import rx.Completable
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

class DrawerPresenterTest : BaseTest() {

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var achievementsRepo: AwardService

    @Mock
    private lateinit var navigator: DrawerNavigator

    @Mock
    private lateinit var rootNavigator: RootNavigator

    @Mock
    private lateinit var screenChangesNotifier: ScreenChangesNotifier

    @Mock
    private lateinit var featureManager: FeatureManager

    @Mock
    private lateinit var regionSettings: RegionSettings

    @Mock
    private lateinit var regionProperty: RegionSettings.RegionProperty

    @Mock
    private lateinit var promocodesNotificationSource: UnvisitedPromocodesNotificationSource

    @Mock
    private lateinit var showcaseCardService: ShowcaseCardService

    @Mock
    private lateinit var view: DrawerContract.View

    private lateinit var analyticsSender: DrawerAnalyticsSender

    private var newFeedbackEnabled: Boolean = true
    private val userSubject = PublishSubject.create<User>()
    private val awardsSubject = PublishSubject.create<List<AchievementsModel>>()
    private val citySubject = PublishSubject.create<CityLocationInfo>()
    private val promocodesBadgeSubject = PublishSubject.create<Boolean>()
    private val tabChanges = PublishSubject.create<ScreenChange>()
    private val actions = PublishSubject.create<Action>()

    private lateinit var logoutCompletable: Completable
    private var logoutCompletableSubscribed = false
    private lateinit var logoutDialogResultSubject: PublishSubject<Any>

    @Before
    override fun setUp() {
        super.setUp()
        logoutCompletableSubscribed = false
        logoutCompletable = Completable.complete().doOnSubscribe { logoutCompletableSubscribed = true }
        logoutDialogResultSubject = PublishSubject.create()
        analyticsSender = DrawerAnalyticsSender(userManager)
        whenever(userManager.logout()).thenReturn(logoutCompletable)
        whenever(userManager.users()).thenReturn(userSubject)

        whenever(navigator.showLogoutConfirmationDialog()).thenReturn(logoutDialogResultSubject.toCompletable())
        whenever(navigator.isMainScreenOnTop).thenReturn(true)

        whenever(achievementsRepo.obtainedAwards).thenReturn(awardsSubject)
        whenever(featureManager.isFeatureEnabled(NEW_FROM_FEEDBACK)).thenReturn(newFeedbackEnabled)
        whenever(regionSettings.currentRegion()).thenReturn(regionProperty)
        whenever(regionProperty.value()).thenReturn(citySubject)
        whenever(promocodesNotificationSource.notificationAvailability()).thenReturn(promocodesBadgeSubject)
        whenever(screenChangesNotifier.screenChanges).thenReturn(tabChanges)
        whenever(view.actions()).thenReturn(actions)
        whenever(view.drawerEvents()).thenReturn(Observable.never())
        whenever(showcaseCardService.showcaseCardState).thenReturn(PublishSubject.create())
    }

    @Test
    fun `shows proper view state`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        verify(view).actions()
        verify(view).drawerEvents()
        verifyNoMoreInteractions(view)

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)

        verify(view).show(
            ViewState(
                UserInfo(AUTHORIZED_USER.displayName, AUTHORIZED_USER.avatar),
                ACHIEVEMENTS.size,
                topUpAvailable = true,
                passengerInfoAvailable = false,
                promoCodesBadgeVisible = false
            )
        )
    }

    @Test
    fun `shows proper promocodes badge state`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState())

        promocodesBadgeSubject.onNext(true)
        verify(view).show(viewState(promoCodesBadgeVisible = true))
    }

    @Test
    fun `shows proper user state`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        setEnvironment(UNAUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState(userInfo = null))

        userSubject.onNext(AUTHORIZED_USER)
        verify(view).show(viewState())
    }

    @Test
    fun `shows proper achievements count`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState())

        val updatedAwards = listOf(AchievementsModel(AROUND_THE_WORLD, true), AchievementsModel(LIKE_GAGARIN, true))
        awardsSubject.onNext(updatedAwards)
        verify(view).show(viewState(achievementsCount = updatedAwards.size))
    }

    @Test
    fun `shows proper top up or info item for different cities`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState())

        citySubject.onNext(CityLocationInfo.UNKNOWN.copy(CityLocationInfo.PETERSBURG_ID))
        verify(view).show(viewState(topUpAvailable = false, passengerInfoAvailable = true))

        citySubject.onNext(CityLocationInfo.UNKNOWN.copy(CityLocationInfo.MINSK_ID))
        verify(view).show(viewState(topUpAvailable = false, passengerInfoAvailable = false))
    }

    @Test
    fun `logout click but canceled in dialog`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState())

        actions.onNext(LogoutClick)
        assertFalse(logoutCompletableSubscribed)

        logoutDialogResultSubject.onError(AbortException())
        assertFalse(logoutCompletableSubscribed)
    }

    @Test
    fun `logout click and confirmed in dialog`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState())

        actions.onNext(LogoutClick)
        assertFalse(logoutCompletableSubscribed)

        logoutDialogResultSubject.onCompleted()
        assertTrue(logoutCompletableSubscribed)
    }

    @Test
    fun `proper action on login click`() {
        val presenter = createPresenter()
        presenter.createAttachStart()
        verify(screenChangesNotifier).screenChanges

        setEnvironment(UNAUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState(userInfo = null))

        actions.onNext(LogoutClick)
        verifyNoMoreNavigatorInteractions() //already logged out
        assertFalse(logoutCompletableSubscribed)

        actions.onNext(LoginClick)
        verify(navigator).toLoginScreen()

        actions.onNext(AvatarClick)
        verify(navigator, times(2)).toLoginScreen()
    }

    @Test
    fun `proper actions on menu items clicks`() {
        val presenter = createPresenter()
        presenter.createAttachStart()
        verify(screenChangesNotifier).screenChanges

        setEnvironment(UNAUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        verify(view).show(viewState(userInfo = null))

        actions.onNext(AchievementsClick)
        verify(navigator).toFirstAchievement()

        actions.onNext(SettingsClick)
        verify(navigator).toSettings()

        actions.onNext(TopUpClick)
        verify(navigator).toTopUpScreen()

        actions.onNext(PassengerInfoClick)
        verify(navigator).toPassengerInfo()

        actions.onNext(PromoCodesClick)
        verify(navigator).toPromocodes()

        actions.onNext(FavoritesClick)
        verify(navigator).toFavorites()

        actions.onNext(BetaTestClick)
        verify(navigator).toBetaTest()

        actions.onNext(FeedbackClick)
        verify(navigator).toFeedback()

        setEnvironment(AUTHORIZED_USER, ACHIEVEMENTS, MOSCOW)
        actions.onNext(AchievementsClick)
        verify(navigator).toAchievements()
    }

    @Test
    fun `no items opening above each other`() {
        val presenter = createPresenter()
        presenter.createAttachStart()

        actions.onNext(AchievementsClick)
        verify(navigator).toFirstAchievement()

        whenever(navigator.isMainScreenOnTop).thenReturn(false)

        actions.onNext(AchievementsClick)

        verifyNoMoreNavigatorInteractions()
    }

    private fun verifyNoMoreNavigatorInteractions() {
        // this is allowed in order to filter navigation
        verify(navigator, atLeastOnce()).isMainScreenOnTop

        verifyNoMoreInteractions(navigator)
    }

    private fun viewState(
        userInfo: UserInfo? = UserInfo(AUTHORIZED_USER.displayName, AUTHORIZED_USER.avatar),
        achievementsCount: Int = ACHIEVEMENTS.size,
        topUpAvailable: Boolean = true,
        passengerInfoAvailable: Boolean = false,
        promoCodesBadgeVisible: Boolean = false
    ): ViewState {
        return ViewState(
            userInfo,
            achievementsCount,
            topUpAvailable = topUpAvailable,
            passengerInfoAvailable = passengerInfoAvailable,
            promoCodesBadgeVisible = promoCodesBadgeVisible
        )
    }

    private fun createPresenter(
        schedulerProvider: SchedulerProvider = SchedulerProvider(
            main = Schedulers.immediate(),
            io = Schedulers.immediate(),
            computation = Schedulers.immediate()
        )
    ): DrawerPresenter {
        return DrawerPresenter(
            userManager,
            achievementsRepo,
            navigator,
            screenChangesNotifier,
            featureManager,
            regionSettings,
            analyticsSender,
            rootNavigator,
            promocodesNotificationSource,
            schedulerProvider,
            showcaseCardService
        )
    }

    private fun DrawerPresenter.createAttachStart() {
        onCreate()
        onAttach(view)
        onViewStart()
    }

    private fun setEnvironment(user: User, achievements: List<AchievementsModel>, city: CityLocationInfo) {
        userSubject.onNext(user)
        awardsSubject.onNext(achievements)
        citySubject.onNext(city)
    }

    companion object {
        val AUTHORIZED_USER = User.Authorized(
            Uid(0),
            Token.Valid("token"),
            "name",
            "email",
            Uri.parse("http://avatarUri"),
            hasPlus = false
        )
        val UNAUTHORIZED_USER = User.Unauthorized
        val ACHIEVEMENTS = listOf(AchievementsModel(IRONMAN, true))
        val MOSCOW = CityLocationInfo.UNKNOWN.copy(id = CityLocationInfo.MOSCOW_ID)
    }
}
