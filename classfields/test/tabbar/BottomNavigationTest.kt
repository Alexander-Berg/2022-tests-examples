package com.yandex.mobile.realty.test.tabbar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnBottomNavigationMenu
import com.yandex.mobile.realty.core.robot.performOnCommunicationScreen
import com.yandex.mobile.realty.core.robot.performOnExtraScreen
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 18.12.2019
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BottomNavigationTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldOpenScreensOnBottomBarTap() {
        activityTestRule.launchActivity()

        performOnBottomNavigationMenu {
            waitUntil { isBottomNavigationViewShown() }

            isSearchTabSelected()

            tapOn(lookup.matchesFavoritesItem())
            isFavouriteTabSelected()
        }
        performOnFavoritesScreen { isToolbarTitleShown() }

        performOnBottomNavigationMenu {
            tapOn(lookup.matchesCommunicationItem())
            isCommunicationTabSelected()
        }
        performOnCommunicationScreen {
            isToolbarTitleShown()
        }

        performOnBottomNavigationMenu {
            tapOn(lookup.matchesServicesItem())
            isServicesTabSelected()
        }
        onScreen<ServicesScreen> {
            listView.isCompletelyDisplayed()
        }

        performOnBottomNavigationMenu {
            tapOn(lookup.matchesExtraItem())
            isExtraTabSelected()
        }
        performOnExtraScreen {
            isToolbarTitleShown()
        }

        performOnBottomNavigationMenu {
            tapOn(lookup.matchesSearchItem())
            isSearchTabSelected()
        }
        onScreen<SearchListScreen> {
            listView.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldScrollSearchListToTop() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("offerWithSiteSearchPage0.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet("4").waitUntil { listView.contains(this) }

            listView.isUpScrollable()
        }

        performOnBottomNavigationMenu {
            tapOn(lookup.matchesSearchItem())
        }

        onScreen<SearchListScreen> {
            listView.waitUntil { isNotUpScrollable() }
        }
    }
}
