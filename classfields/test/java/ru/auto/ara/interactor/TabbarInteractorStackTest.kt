package ru.auto.ara.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.interactor.ICarPresentationDotInteractor
import ru.auto.data.interactor.IYaPlusInteractor
import ru.auto.data.interactor.TabbarInteractor
import ru.auto.data.interactor.YaPlusLoginInfo
import ru.auto.data.interactor.YaPlusPromoAvailability
import ru.auto.data.interactor.sync.IFavoriteInteractor
import ru.auto.data.interactor.sync.ISavedSearchNewCountEmmitter
import ru.auto.data.model.AutoruUserProfile
import ru.auto.data.model.User
import ru.auto.data.model.UserPhone
import ru.auto.data.model.UserProfile
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.tabbar.MainTabbarTab
import ru.auto.data.model.tabbar.RefreshState
import ru.auto.data.repository.ITabbarRepository
import ru.auto.data.repository.IUserOffersRepository
import ru.auto.data.repository.user.IUserRepository
import ru.auto.feature.chats.messages.data.database.IDialogsObserveRepository
import rx.Completable
import rx.Observable
import rx.Single
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author danser on 06.11.2018.
 */
@RunWith(AllureRunner::class) class TabbarInteractorStackTest {

    private val favoriteInteractor: IFavoriteInteractor<Offer> = mock()

    private val savedSearchNewCountEmitter: ISavedSearchNewCountEmmitter = mock()
    private val userRepository: IUserRepository = mock()
    private val userOffersRepository: IUserOffersRepository = mock()
    private val tabbarRepository: ITabbarRepository = mock()
    private val dialogsRepository: IDialogsObserveRepository = mock {
        on { observeDialogs() } doReturn (Observable.empty())
    }
    private val carPresentationDotInteractor: ICarPresentationDotInteractor = mock()
    private val yaPlusInteractor: IYaPlusInteractor = mock()

    private lateinit var tabbarInteractor: TabbarInteractor

    @Before
    fun setUp() {
        whenever(favoriteInteractor.observeFavoriteOfferPoint()).thenReturn(getObservable())
        whenever(savedSearchNewCountEmitter.observeNewOffersPoint()).thenReturn(getObservable())
        whenever(userRepository.observeUser()).thenReturn(Observable.just(USER_DEALER))
        whenever(userRepository.user).thenReturn(USER_DEALER)
        whenever(carPresentationDotInteractor.canShowStory()).thenReturn(Observable.never())
        whenever(userOffersRepository.observeOffers()).thenReturn(Observable.just(emptyList()))
        whenever(tabbarRepository.getOffersTabType()).thenReturn(Single.just(MainTabbarTab.TabType.ADD))
        whenever(tabbarRepository.defaultOffersTabType).thenReturn(MainTabbarTab.TabType.ADD)
        whenever(tabbarRepository.saveOffersTabType(MainTabbarTab.TabType.ADD)).thenReturn(Completable.complete())
        whenever(yaPlusInteractor.getLoginInfo()).thenReturn(Single.just(YaPlusLoginInfo.NotAuthorized))
        whenever(yaPlusInteractor.isPromotionAvailable()).thenReturn(Single.just(YaPlusPromoAvailability.Unavailable))
        whenever(yaPlusInteractor.observePromotionAvailabilityOnUserEvents())
            .thenReturn(Observable.just(YaPlusPromoAvailability.Unavailable))

        tabbarInteractor = TabbarInteractor(
            userRepository = userRepository,
            userOffersRepository = userOffersRepository,
            favoriteOfferInteractor = favoriteInteractor,
            dialogsRepo = dialogsRepository,
            savedSearchNewCountEmitter = savedSearchNewCountEmitter,
            logError = { _, _ -> },
            carPresentationDotInteractor = carPresentationDotInteractor,
            tabbarRepository = tabbarRepository,
            yaPlusInteractor = yaPlusInteractor,
        )
    }

    @Test
    fun assert_first_tab_is_search() {
        assertEquals(tabbarInteractor.getCurrentTab(), MainTabbarTab.TabType.SEARCH)
    }

    @Test
    fun assert_current_tab_after_go_to_other_is_other() {
        tabbarInteractor.setCurrentTab(MainTabbarTab.TabType.FAVORITE)
        tabbarInteractor.setCurrentTab(MainTabbarTab.TabType.GARAGE)

        assertEquals(tabbarInteractor.getCurrentTab(), MainTabbarTab.TabType.GARAGE)
    }

    @Test
    fun assert_can_go_back() {
        assertFalse(tabbarInteractor.canGoBack())
        tabbarInteractor.setCurrentTab(MainTabbarTab.TabType.MESSAGES)
        assertTrue(tabbarInteractor.canGoBack())
        tabbarInteractor.goToPreviousTab()
        assertFalse(tabbarInteractor.canGoBack())
    }

    @Test
    fun assert_tabs_are_the_same_when_go_back() {
        val commands = listOf(
            MainTabbarTab.TabType.SEARCH,
            MainTabbarTab.TabType.FAVORITE,
            MainTabbarTab.TabType.GARAGE,
            MainTabbarTab.TabType.FAVORITE,
            MainTabbarTab.TabType.FAVORITE,
            MainTabbarTab.TabType.SEARCH,
            MainTabbarTab.TabType.ADD,
            MainTabbarTab.TabType.MESSAGES
        )

        val backStack = listOf(
            MainTabbarTab.TabType.MESSAGES,
            MainTabbarTab.TabType.ADD,
            MainTabbarTab.TabType.SEARCH,
            MainTabbarTab.TabType.FAVORITE,
            MainTabbarTab.TabType.GARAGE
        )

        commands.forEach {
            tabbarInteractor.setCurrentTab(it)
        }

        val backTabs = ArrayList<MainTabbarTab.TabType>()
        while (tabbarInteractor.canGoBack()) {
            backTabs.add(tabbarInteractor.getCurrentTab())
            tabbarInteractor.goToPreviousTab()
        }
        backTabs.add(tabbarInteractor.getCurrentTab())

        assertEquals(backTabs, backStack)
    }

    private fun getObservable(items: List<RefreshState.Point?> = listOf(null)): Observable<RefreshState.Point?> =
        Observable.from(items)

    companion object {
        private val USER_DEALER = User.Authorized(
            id = "id00000000",
            userProfile = UserProfile(AutoruUserProfile("")),
            phones = listOf(UserPhone("+712345678")),
            balance = 0
        )
    }
}
