package ru.auto.ara.presentation.presenter

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.only
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.AutoApplication.COMPONENT_MANAGER
import ru.auto.ara.RxTest
import ru.auto.ara.di.ComponentManager
import ru.auto.ara.di.component.main.IMainTabbarProvider
import ru.auto.ara.presentation.presenter.tabbar.MainTabbarPresenter
import ru.auto.ara.presentation.viewstate.tabbar.MainTabbarViewState
import ru.auto.ara.router.Navigator
import ru.auto.ara.router.tab.ITabNavigation
import ru.auto.ara.router.tab.ITabRouter
import ru.auto.ara.util.error.ErrorFactory
import ru.auto.ara.viewmodel.tabbar.MainTabState
import ru.auto.ara.viewmodel.tabbar.MainTabbarViewModel
import ru.auto.data.interactor.IYaPlusInteractor
import ru.auto.data.interactor.TabbarInteractor
import ru.auto.data.interactor.YaPlusLoginInfo
import ru.auto.data.interactor.YaPlusPromoAvailability
import ru.auto.data.model.ITab
import ru.auto.data.model.tabbar.MainTabbarTab
import rx.Observable
import rx.Single

/**
 * @author danser on 20/11/2018.
 */
@RunWith(AllureRunner::class) class MainTabbarPresenterTest : RxTest() {

    private val viewState: MainTabbarViewState = mock()
    private val router: Navigator = mock()
    private val errorFactory: ErrorFactory = mock()
    private val tabNavigation: ITabNavigation = mock()
    private val tabRouter: ITabRouter = mock()
    private val tabbarInteractor: TabbarInteractor = mock()
    private val yaPlusInteractor: IYaPlusInteractor = mock()

    private lateinit var presenter: MainTabbarPresenter
    private var currentTabType = MainTabbarTab.TabType.SEARCH


    @Before
    fun setUp() {
        whenever(errorFactory.createSnackError(any())).thenThrow(AssertionError("error appeared"))
        whenever(tabbarInteractor.tabsChanges).thenReturn(Observable.just(TabbarInteractor.GARAGE_USER_TABS))
        whenever(tabbarInteractor.getTabs()).thenReturn(TabbarInteractor.GARAGE_USER_TABS)
        whenever(tabNavigation.tabs<ITab>(any())).thenReturn(Observable.empty())
        whenever(tabbarInteractor.getCurrentTab()).thenReturn(currentTabType)
        whenever(yaPlusInteractor.getLoginInfo()).thenReturn(Single.just(YaPlusLoginInfo.NotAuthorized))
        whenever(yaPlusInteractor.isPromotionAvailable()).thenReturn(Single.just(YaPlusPromoAvailability.Unavailable))
        whenever(yaPlusInteractor.observePromotionAvailabilityOnUserEvents())
            .thenReturn(Observable.just(YaPlusPromoAvailability.Unavailable))
        COMPONENT_MANAGER = ComponentManager(mock())
    }

    @Test
    fun check_presenter_updates_view_model_with_start_tab_on_start() {
        MainTabbarTab.TabType.values().forEach { tabType ->
            currentTabType = tabType

            whenever(tabbarInteractor.getCurrentTab()).thenReturn(currentTabType)
            presenter = getPresenter()

            val tabs = TabbarInteractor.GARAGE_USER_TABS.map(MainTabState::Tab)

            verify(viewState, atLeastOnce()).setModel(
                MainTabbarViewModel(tabs, tabType)
            )
        }
    }

    @Test
    fun check_tab_router_clears_state_on_presenters_on_destroyed_call() {
        IMainTabbarProvider.ref = COMPONENT_MANAGER.mainTabbarRef
        presenter = getPresenter()
        presenter.onDestroyed()
        verify(tabRouter, only()).clearState()
    }

    @Test
    fun check_update_current_tab_on_tab_selected() {
        TabbarInteractor.GARAGE_USER_TABS.forEachIndexed { pos, tab ->
            presenter = getPresenter()
            presenter.onTabSelected(pos)
            verify(tabbarInteractor, atLeastOnce()).setCurrentTab(tab.type)
        }
    }

    private fun getPresenter() = MainTabbarPresenter(
        viewState = viewState,
        router = router,
        viewErrorFactory = errorFactory,
        tabNavigation = tabNavigation,
        tabRouter = tabRouter,
        tabbarInteractor = tabbarInteractor,
        startTabType = currentTabType,
        analystManager = mock(),
        yaPlusInteractor = yaPlusInteractor,
        yaPlusAnalyst = mock(),
    )
}
