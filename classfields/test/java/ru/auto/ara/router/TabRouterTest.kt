package ru.auto.ara.router

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.RxTest
import ru.auto.ara.router.tab.TabNavigationPoint
import ru.auto.ara.router.tab.TabRouter
import ru.auto.ara.util.assertValuesNoErrors
import ru.auto.data.model.main.MainTab
import ru.auto.data.model.tabbar.FavoriteTab
import ru.auto.data.util.toListOrEmpty

/**
 * @author danser on 22/11/2018.
 */
@RunWith(AllureParametrizedRunner::class)
class TabRouterTest(private val points: List<TabNavigationPoint>) : RxTest() {

    private val tabRouter: TabRouter = TabRouter

    @Before
    fun setUp() {

    }

    @Test
    fun check_router_emmit_last_tab_after_calling_show() {
        tabRouter.clearState()
        points.forEach { tabRouter.show(it) }

        val mainPoint = TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.SEARCH, MainTab.Tab.TRANSPORT)
        val expectedPoints = listOf(mainPoint) + points

        val expectedTabbarTab = expectedPoints.last().mainTab
        tabRouter.tabs(TabNavigationPoint.MainTabbarTab::class.java).test().assertValuesNoErrors(
            expectedTabbarTab
        )

        val expectedMainTab = expectedPoints.last().childTab as? MainTab.Tab
        tabRouter.tabs(MainTab.Tab::class.java).test().assertValuesNoErrors(
            *expectedMainTab.toListOrEmpty().toTypedArray() //pass empty vararg if null
        )

        val expectedFavoriteTab = expectedPoints.last().childTab as? FavoriteTab.Tab
        tabRouter.tabs(FavoriteTab.Tab::class.java).test().assertValuesNoErrors(
            *expectedFavoriteTab.toListOrEmpty().toTypedArray() //pass empty vararg if null
        )

        val expectedMainPoint = expectedPoints.last() as? TabNavigationPoint.MAIN
        tabRouter.points(TabNavigationPoint.MAIN::class.java).test().assertValuesNoErrors(
            *expectedMainPoint.toListOrEmpty().toTypedArray() //pass empty vararg if null
        )

        val expectedFeedPoint = expectedPoints.last() as? TabNavigationPoint.FEED
        tabRouter.points(TabNavigationPoint.FEED::class.java).test().assertValuesNoErrors(
            *expectedFeedPoint.toListOrEmpty().toTypedArray() //pass empty vararg if null
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): Collection<Array<Any>> = listOf<Array<Any>>(
            arrayOf(
                listOf(
                    TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.SEARCH),
                    TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.FAVORITE)
                )
            ),
            arrayOf(
                listOf(
                    TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.SEARCH, MainTab.Tab.FORME),
                    TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.SEARCH, MainTab.Tab.REVIEWS)
                )
            ),
            arrayOf(
                listOf(
                    TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.FAVORITE),
                    TabNavigationPoint.MAIN(TabNavigationPoint.MainTabbarTab.FAVORITE, FavoriteTab.Tab.SEARCHES)
                )
            )
        )
    }
}
