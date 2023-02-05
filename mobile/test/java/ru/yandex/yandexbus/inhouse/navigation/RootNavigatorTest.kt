package ru.yandex.yandexbus.inhouse.navigation

import androidx.fragment.app.FragmentActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.common.session.AppStateNotifier
import ru.yandex.yandexbus.inhouse.mvp.mvp_new.BaseMvpPresenter
import ru.yandex.yandexbus.inhouse.navbar.roots.map.MapOwner
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.ActiveScreenAction
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.ActiveScreenAction.DETACH_IF_EXIST
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.ActiveScreenAction.NOTHING
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.ActiveScreenAction.REMOVE
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.AnimationType
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.NewScreenAction
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.NewScreenAction.ATTACH_OR_CREATE_AND_ADD
import ru.yandex.yandexbus.inhouse.navigation.FragmentController.NewScreenAction.CREATE_AND_ADD
import ru.yandex.yandexbus.inhouse.navigation.NavigationRequest.AlarmNavigationRequest
import ru.yandex.yandexbus.inhouse.navigation.NavigationRequest.DeeplinkNavigationRequest
import ru.yandex.yandexbus.inhouse.navigation.NavigationRequest.OpenMenuNavigationRequest
import ru.yandex.yandexbus.inhouse.navigation.RootNavigator.SavedState
import ru.yandex.yandexbus.inhouse.navigation.RootNavigator.ScreenRecord
import ru.yandex.yandexbus.inhouse.navigation.RootNavigator.SpecialCaseEvent
import ru.yandex.yandexbus.inhouse.navigation.RootNavigator.TransactionInfo
import ru.yandex.yandexbus.inhouse.repos.TimeLimitation
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteSetupArgs
import ru.yandex.yandexbus.inhouse.stop.StopModel
import ru.yandex.yandexbus.inhouse.stop.card.StopCardArgs
import ru.yandex.yandexbus.inhouse.ui.main.routetab.details.RouteDetailsArgs
import ru.yandex.yandexbus.inhouse.utils.Screen
import ru.yandex.yandexbus.inhouse.utils.Screen.MAP
import ru.yandex.yandexbus.inhouse.utils.Screen.SEARCH_SUGGEST
import ru.yandex.yandexbus.inhouse.utils.analytics.GenaAppAnalytics.MapShowStopCardSource
import rx.observers.AssertableSubscriber

class RootNavigatorTest : BaseTest() {

    @Mock
    lateinit var activity: FragmentActivity

    @Mock
    lateinit var appStateNotifier: AppStateNotifier

    private lateinit var fragmentController: TestFragmentController

    @Mock
    lateinit var activityLifeCycleProvider: ExtendedLifecycleInfoProvider

    private lateinit var navigator: RootNavigator
    private lateinit var screenChangesSubscriber: AssertableSubscriber<Pair<Screen?, Screen>>

    private lateinit var receivedSpecialEvents: MutableList<SpecialCaseEvent>

    @Before
    override fun setUp() {
        super.setUp()

        receivedSpecialEvents = mutableListOf()
        fragmentController = TestFragmentController()

        navigator = RootNavigator(activity, appStateNotifier, fragmentController, activityLifeCycleProvider)
        screenChangesSubscriber = navigator.screenChanges().test()
        navigator.init(null, alarmNavigationRequest = null) {
            receivedSpecialEvents.add(it)
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()

        navigator.destroy()
    }

    @Test
    fun `initial map screen opened`() {

        assertEquals(listOf(Screen.MAP), fragmentController.activeScreens)

        assertEquals(Screen.MAP, navigator.topScreen)

        screenChangesSubscriber
            .assertNoValues()
            .assertNotCompleted()
    }

    @Test
    fun `open regular screen`() {

        navigator.openScreen(Screen.SEARCH_SUGGEST)

        assertState(
            topScreen = SEARCH_SUGGEST,
            mapActive = false,
            activeScreens = listOf(Screen.SEARCH_SUGGEST),
            detachedScreens = listOf(Screen.MAP)
        )
        assertLastChangeEvent(Screen.MAP to Screen.SEARCH_SUGGEST)
    }

    @Test
    fun `open 'card' screen`() {

        navigator.openScreen(Screen.CARD_TRANSPORT)

        assertState(
            topScreen = Screen.CARD_TRANSPORT,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_TRANSPORT),
            detachedScreens = emptyList()
        )
        assertLastChangeEvent(Screen.MAP to Screen.CARD_TRANSPORT)
    }

    @Test
    fun `go back works`() {

        navigator.openScreenFromDrawer(Screen.SETTINGS)
        navigator.openScreen(Screen.ABOUT_SCREEN)

        assertTrue(navigator.goBack())

        assertState(
            topScreen = Screen.SETTINGS,
            mapActive = false,
            activeScreens = listOf(Screen.SETTINGS),
            detachedScreens = listOf(Screen.MAP)
        )
        assertEquals(Screen.SETTINGS, navigator.topScreen)
        assertLastChangeEvent(Screen.ABOUT_SCREEN to Screen.SETTINGS)

        assertTrue(navigator.goBack())

        assertOnlyRootMapScreenOpened()
        assertLastChangeEvent(Screen.SETTINGS to Screen.MAP)
    }

    @Test
    fun `open 'card' screen above 'card' and go back`() {

        navigator.openScreen(Screen.CARD_TRANSPORT)
        navigator.openScreen(Screen.CARD_USER_PLACES)

        assertState(
            topScreen = Screen.CARD_USER_PLACES,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_USER_PLACES),
            detachedScreens = emptyList()
        )
        assertLastChangeEvent(Screen.CARD_TRANSPORT to Screen.CARD_USER_PLACES)

        assertTrue(navigator.goBack())

        assertOnlyRootMapScreenOpened()
        assertLastChangeEvent(Screen.CARD_USER_PLACES to Screen.MAP)
    }

    @Test
    fun `open 'relative card' screens and go back`() {

        navigator.openScreen(Screen.CARD_TRANSPORT)

        val args = StopCardArgs(StopModel("stopId", name = null, point = null), MapShowStopCardSource.MAP, true)
        navigator.openScreen(Screen.CARD_STOP, args)

        assertState(
            topScreen = Screen.CARD_STOP,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_STOP),
            detachedScreens = listOf(Screen.CARD_TRANSPORT)
        )
        assertLastChangeEvent(Screen.CARD_TRANSPORT to Screen.CARD_STOP)

        assertTrue(navigator.goBack())

        assertState(
            topScreen = Screen.CARD_TRANSPORT,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_TRANSPORT),
            detachedScreens = emptyList()
        )
        assertLastChangeEvent(Screen.CARD_STOP to Screen.CARD_TRANSPORT)
    }

    @Test
    fun `open 'card' when 'relative card' screens already opened`() {

        navigator.openScreen(Screen.CARD_TRANSPORT)

        val args = StopCardArgs(StopModel("stopId", name = null, point = null), MapShowStopCardSource.MAP, true)
        navigator.openScreen(Screen.CARD_STOP, args)

        navigator.openScreen(Screen.CARD_TRANSPORT)

        assertState(
            topScreen = Screen.CARD_TRANSPORT,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_TRANSPORT),
            detachedScreens = emptyList()
        )
        assertLastChangeEvent(Screen.CARD_STOP to Screen.CARD_TRANSPORT)

        assertTrue(navigator.goBack())

        assertOnlyRootMapScreenOpened()
        assertLastChangeEvent(Screen.CARD_TRANSPORT to Screen.MAP)
    }

    @Test
    fun `open 'card' on map`() {

        navigator.openScreen(Screen.FAVORITES)
        navigator.openCardOnMap(Screen.CARD_STOP)

        assertState(
            topScreen = Screen.CARD_STOP,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_STOP),
            detachedScreens = emptyList()
        )
        assertLastChangeEvent(Screen.FAVORITES to Screen.CARD_STOP)

        assertTrue(navigator.goBack())

        assertOnlyRootMapScreenOpened()
        assertLastChangeEvent(Screen.CARD_STOP to Screen.MAP)
    }

    @Test
    fun `go back to map from 'card'`() {

        navigator.openScreen(Screen.CARD_STOP)
        navigator.openScreen(Screen.CARD_TRANSPORT)

        assertState(
            topScreen = Screen.CARD_TRANSPORT,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_TRANSPORT),
            detachedScreens = emptyList()
        )
        assertEquals(Screen.CARD_TRANSPORT, navigator.topScreen)
        assertLastChangeEvent(Screen.CARD_STOP to Screen.CARD_TRANSPORT)

        navigator.goBackToMap()

        assertOnlyRootMapScreenOpened()
        assertLastChangeEvent(Screen.CARD_TRANSPORT to Screen.MAP)
    }

    @Test
    fun `go back to map from 'card' above route details screen`() {

        navigator.openScreen(Screen.ROUTE_SETUP)
        navigator.openScreen(Screen.ROUTE_DETAILS)
        navigator.openScreen(Screen.CARD_STOP)
        navigator.openScreen(Screen.CARD_TRANSPORT)

        assertState(
            topScreen = Screen.CARD_TRANSPORT,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_TRANSPORT),
            detachedScreens = listOf(Screen.ROUTE_SETUP, Screen.ROUTE_DETAILS)
        )
        assertLastChangeEvent(Screen.CARD_STOP to Screen.CARD_TRANSPORT)

        navigator.goBackToMap()

        assertState(
            topScreen = Screen.ROUTE_DETAILS,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.ROUTE_DETAILS),
            detachedScreens = listOf(Screen.ROUTE_SETUP)
        )
        assertLastChangeEvent(Screen.CARD_TRANSPORT to Screen.ROUTE_DETAILS)
    }

    @Test
    fun `restore state with only one screen`() {
        fragmentController = TestFragmentController()
        fragmentController.activeScreens.add(Screen.MAP)

        val savedState = SavedState(arrayListOf(ScreenRecord(Screen.MAP, null)))

        navigator = RootNavigator(activity, appStateNotifier, fragmentController, activityLifeCycleProvider)
        navigator.init(savedState, alarmNavigationRequest = null) {}

        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `restore state with several screens`() {
        fragmentController = TestFragmentController()
        fragmentController.activeScreens.add(Screen.MAP)
        fragmentController.activeScreens.add(Screen.CARD_TRANSPORT)

        val savedState = SavedState(arrayListOf(ScreenRecord(Screen.MAP, null), ScreenRecord(Screen.CARD_TRANSPORT, null)))

        navigator = RootNavigator(activity, appStateNotifier, fragmentController, activityLifeCycleProvider)
        navigator.init(savedState, alarmNavigationRequest = null) {}

        assertState(
            topScreen = Screen.CARD_TRANSPORT,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_TRANSPORT),
            detachedScreens = emptyList()
        )

        navigator.goBack()

        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `restore state when collapsed on route details screen`() {
        fragmentController = TestFragmentController()
        fragmentController.activeScreens.addAll(listOf(Screen.MAP, Screen.ROUTE_DETAILS))

        val savedState = SavedState(arrayListOf(
            ScreenRecord(Screen.MAP, null),
            ScreenRecord(Screen.ROUTE_SETUP, null),
            ScreenRecord(Screen.ROUTE_DETAILS, null)
        ))

        navigator = RootNavigator(activity, appStateNotifier, fragmentController, activityLifeCycleProvider)
        navigator.init(savedState, alarmNavigationRequest = null) {}

        assertState(
            topScreen = Screen.ROUTE_DETAILS,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.ROUTE_DETAILS),
            detachedScreens = emptyList()
        )

        navigator.goBack()

        assertState(
            topScreen = Screen.ROUTE_SETUP,
            mapActive = false,
            activeScreens = listOf(Screen.ROUTE_SETUP),
            detachedScreens = listOf(Screen.MAP)
        )

        navigator.goBack()

        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `restore state went wrong`() {

        fragmentController = TestFragmentController()
        // for some reason it hasn't got Screen.MAP (something went wrong)
        fragmentController.activeScreens.add(Screen.CARD_TRANSPORT)

        val savedState = SavedState(arrayListOf(ScreenRecord(Screen.MAP, null), ScreenRecord(Screen.CARD_TRANSPORT, null)))

        navigator = RootNavigator(activity, appStateNotifier, fragmentController, activityLifeCycleProvider)
        navigator.init(savedState, alarmNavigationRequest = null) {}

        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `generate state has all screens`() {

        navigator.openScreen(Screen.ROUTE_SETUP)
        navigator.openScreen(Screen.ROUTE_DETAILS)
        navigator.openScreen(Screen.CARD_STOP)

        val savedState = navigator.generateSavedState()
        assertEquals(
            listOf(
                ScreenRecord(Screen.MAP, null),
                ScreenRecord(Screen.ROUTE_SETUP, null),
                ScreenRecord(Screen.ROUTE_DETAILS, null),
                ScreenRecord(Screen.CARD_STOP, null)
            ),
            savedState.screensStack
        )
    }

    @Test
    fun `open main screen when alarm is working`() {

        fragmentController = TestFragmentController()
        val alarmRequest = AlarmNavigationRequest(ROUTE_SETUP_ARGS, ROUTE_DETAILS_ARGS, fromNotification = false)

        navigator = RootNavigator(activity, appStateNotifier, fragmentController, activityLifeCycleProvider)
        navigator.init(savedState = null, alarmNavigationRequest = alarmRequest) {}

        assertState(
            topScreen = Screen.ROUTE_DETAILS,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.ROUTE_DETAILS),
            detachedScreens = emptyList() //when opening 2 screens at once, first is only added to internal stack, not attached to fragmentManager
        )

        navigator.goBack()

        assertState(
            topScreen = Screen.ROUTE_SETUP,
            mapActive = false,
            activeScreens = listOf(Screen.ROUTE_SETUP),
            detachedScreens = listOf(Screen.MAP)
        )
    }

    @Test
    fun `tap on notification when alarm is working and there are screens above route details`() {

        navigator.openScreen(Screen.ROUTE_SETUP)
        navigator.openScreen(Screen.ROUTE_DETAILS)
        navigator.openScreen(Screen.CARD_STOP)

        val alarmRequest = AlarmNavigationRequest(ROUTE_SETUP_ARGS, ROUTE_DETAILS_ARGS, fromNotification = true)
        navigator.openPath(alarmRequest)

        assertState(
            topScreen = Screen.ROUTE_DETAILS,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.ROUTE_DETAILS),
            detachedScreens = listOf(Screen.ROUTE_SETUP)
        )

        navigator.goBack()

        assertState(
            topScreen = Screen.ROUTE_SETUP,
            mapActive = false,
            activeScreens = listOf(Screen.ROUTE_SETUP),
            detachedScreens = listOf(Screen.MAP)
        )
    }

    @Test
    fun `go back to root screen on search close click`() {

        navigator.openScreen(Screen.SEARCH_SUGGEST)
        navigator.openScreen(Screen.SEARCH_LIST)
        navigator.openScreen(Screen.CARD_SEARCH_RESULT_PLACE)

        assertState(
            topScreen = Screen.CARD_SEARCH_RESULT_PLACE,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_SEARCH_RESULT_PLACE),
            detachedScreens = listOf(Screen.SEARCH_SUGGEST, Screen.SEARCH_LIST)
        )

        navigator.goBackToScreen(Screen.MAP)

        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `opening and closing cards above search list screen`() {

        navigator.openScreen(Screen.SEARCH_SUGGEST)
        navigator.openScreen(Screen.SEARCH_LIST)

        navigator.openScreen(Screen.CARD_SEARCH_RESULT_PLACE)

        assertState(
            topScreen = Screen.CARD_SEARCH_RESULT_PLACE,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_SEARCH_RESULT_PLACE),
            detachedScreens = listOf(Screen.SEARCH_SUGGEST, Screen.SEARCH_LIST)
        )

        navigator.openScreen(Screen.CARD_STOP) //another 'card' screen is expected to replace current "card"

        assertState(
            topScreen = Screen.CARD_STOP,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.CARD_STOP),
            detachedScreens = listOf(Screen.SEARCH_SUGGEST, Screen.SEARCH_LIST)
        )

        navigator.goBackToMap() //should return to SEARCH_LIST

        assertState(
            topScreen = Screen.SEARCH_LIST,
            mapActive = true,
            activeScreens = listOf(Screen.MAP, Screen.SEARCH_LIST),
            detachedScreens = listOf(Screen.SEARCH_SUGGEST)
        )
    }

    @Test
    fun `open route setup from map`() {

        navigator.openScreen(Screen.ROUTE_SETUP)

        assertState(
            topScreen = Screen.ROUTE_SETUP,
            mapActive = false,
            activeScreens = listOf(Screen.ROUTE_SETUP),
            detachedScreens = listOf(Screen.MAP)
        )
        assertTrue(receivedSpecialEvents.isEmpty())
    }

    @Test
    fun `open route setup open it is already opened for another route`() {
        navigator.openScreen(Screen.ROUTE_SETUP)
        navigator.openScreen(Screen.ROUTE_DETAILS)
        navigator.openScreen(Screen.CARD_ORGANIZATION)

        navigator.openScreen(Screen.ROUTE_SETUP)

        assertState(
            topScreen = Screen.ROUTE_SETUP,
            mapActive = false,
            activeScreens = listOf(Screen.ROUTE_SETUP),
            detachedScreens = listOf(Screen.MAP)
        )
        assertEquals(SpecialCaseEvent.ROUTE_SETUP_OPENED_ABOVE_ROUTE_SETUP, receivedSpecialEvents.first())

        navigator.goBack()
        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `open route setup from context menu when route detail already opened`() {
        navigator.openScreen(Screen.ROUTE_SETUP)
        navigator.openScreen(Screen.ROUTE_DETAILS)
        navigator.openScreen(Screen.MAP_CONTEXT_MENU)
        navigator.openScreenAndReplace(Screen.ROUTE_SETUP)

        assertState(
            topScreen = Screen.ROUTE_SETUP,
            mapActive = false,
            activeScreens = listOf(Screen.ROUTE_SETUP),
            detachedScreens = listOf(Screen.MAP)
        )
    }

    @Test
    fun `selects proper animation types`() {

        navigator.openScreen(Screen.CARD_ORGANIZATION) //card
        assertEquals(AnimationType.NO_ANIMATION, fragmentController.lastAnimationType)

        navigator.goBack()
        assertEquals(AnimationType.NO_ANIMATION, fragmentController.lastAnimationType)

        navigator.openScreen(Screen.SEARCH_LIST) //not a card, but map is visible
        assertEquals(AnimationType.NO_ANIMATION, fragmentController.lastAnimationType)

        navigator.goBack()
        assertEquals(AnimationType.NO_ANIMATION, fragmentController.lastAnimationType)

        navigator.openScreen(Screen.SETTINGS) //regular screen above map
        assertEquals(AnimationType.OPEN_ABOVE_MAP, fragmentController.lastAnimationType)

        navigator.goBack() //regular screen to map
        assertEquals(AnimationType.CLOSE_TO_MAP, fragmentController.lastAnimationType)
    }

    @Test
    fun `open main screen from deeplink`() {
        navigator.openScreen(Screen.CARD_ORGANIZATION) //let's open any screen

        navigator.openPath(DeeplinkNavigationRequest())

        assertOnlyRootMapScreenOpened()
    }

    @Test
    fun `open menu from deeplink`() {
        navigator.openScreen(Screen.CARD_ORGANIZATION) //let's open any screen

        navigator.openPath(OpenMenuNavigationRequest)

        assertOnlyRootMapScreenOpened()
        assertEquals(SpecialCaseEvent.OPEN_MENU, receivedSpecialEvents.first())
    }

    @Test
    fun `open menu from deeplink when routes screen is opened`() {
        navigator.openScreen(Screen.ROUTE_SETUP)
        navigator.openScreen(Screen.ROUTE_DETAILS)

        navigator.openPath(OpenMenuNavigationRequest)

        assertOnlyRootMapScreenOpened()
        assertEquals(listOf(SpecialCaseEvent.ROUTE_SETUP_OPENED_ABOVE_ROUTE_SETUP, SpecialCaseEvent.OPEN_MENU), receivedSpecialEvents)
    }

    //TODO: currentTransactionInfo

    private fun assertOnlyRootMapScreenOpened() {
        assertState(
            topScreen = Screen.MAP,
            mapActive = true,
            activeScreens = listOf(Screen.MAP),
            detachedScreens = emptyList()
        )
        assertFalse(navigator.goBack())
    }

    private fun assertState(topScreen: Screen, mapActive: Boolean, activeScreens: List<Screen>, detachedScreens: List<Screen>) {
        assertEquals(topScreen, navigator.topScreen)
        assertEquals(mapActive, fragmentController.mapActive)
        assertEquals(activeScreens, fragmentController.activeScreens)
        assertEquals(detachedScreens, fragmentController.detachedScreens)
    }

    private fun assertLastChangeEvent(change: Pair<Screen?, Screen>) {
        assertEquals(change, screenChangesSubscriber.onNextEvents.last())
    }

    private companion object {
        val ROUTE_SETUP_ARGS = RouteSetupArgs(null, null, null, false)
        val ROUTE_DETAILS_ARGS = RouteDetailsArgs(emptyList(), 0, emptySet(), emptySet(), emptySet(), emptyList(), TimeLimitation.departureNow())
    }
}

class TestFragmentController : FragmentController {

    var initialScreen: Screen? = null
        set(screen) {
            activeScreens.add(screen!!)
            field = screen
        }

    var lastAnimationType: AnimationType? = null

    val mapActive: Boolean
        get() = activeScreens.contains(MAP)

    var activeScreens: MutableList<Screen> = mutableListOf()
    val detachedScreens: MutableList<Screen> = mutableListOf()

    override val currentTransactionInfo: TransactionInfo? = null

    override fun executeTransaction(
        mapShouldBeActive: Boolean,
        screenBelowStartingScreen: Screen?,
        activeScreenAction: ActiveScreenAction,
        screensToRemove: List<Screen>,
        newScreenAction: NewScreenAction,
        newScreen: Screen,
        newScreenArgs: Args?,
        animationType: AnimationType,
        onSuccess: () -> Unit
    ) {

        if (mapShouldBeActive) {
            if (detachedScreens.contains(MAP)) {
                detachedScreens.remove(MAP)
                activeScreens.add(0, MAP)
            }
        } else {
            if (activeScreens.contains(MAP)) {
                activeScreens.remove(MAP)
                detachedScreens.add(0, MAP)
            }
        }

        when (activeScreenAction) {
            NOTHING -> {}
            DETACH_IF_EXIST -> {
                activeScreens.lastOrNull()?.let {
                    detachedScreens.add(it)
                    activeScreens.remove(it)
                }
            }
            REMOVE -> { activeScreens.remove(activeScreens.last()) }
        }

        //screensToRemove
        activeScreens.removeAll(screensToRemove)
        detachedScreens.removeAll(screensToRemove)

        when (newScreenAction) {
            CREATE_AND_ADD -> {
                activeScreens.add(newScreen)
            }
            ATTACH_OR_CREATE_AND_ADD -> {
                detachedScreens.remove(newScreen)
                if (!activeScreens.contains(newScreen)) {
                    activeScreens.add(newScreen)
                }
            }
        }

        onSuccess.invoke()

        lastAnimationType = animationType
    }

    override fun addInitialScreen(screen: Screen) {
        if (!activeScreens.contains(screen)) {
            activeScreens.add(screen)
        }
    }

    override fun removeScreens(screens: List<Screen>) {
        activeScreens.removeAll(screens)
        detachedScreens.removeAll(screens)
    }

    override fun checkState(activeScreens: List<Screen>, detachedScreens: List<Screen>): Boolean {
        return this.activeScreens.containsAll(activeScreens) && this.detachedScreens.containsAll(detachedScreens)
    }

    override fun getMapOwner(): MapOwner {
        return object : MapOwner {

            override fun <V> addPresenter(presenter: BaseMvpPresenter<V>, view: V) {
            }

            override fun removePresenter() {
            }
        }
    }
}
