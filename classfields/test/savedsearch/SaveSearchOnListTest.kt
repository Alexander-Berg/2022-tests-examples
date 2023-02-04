package com.yandex.mobile.realty.test.savedsearch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
import com.yandex.mobile.realty.core.robot.performOnSaveSearchResultScreen
import com.yandex.mobile.realty.core.robot.performOnSaveSearchScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 26/06/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SaveSearchOnListTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
    )

    @Test
    fun shouldSaveSearchOnSearchList() {
        val dispatcher = DispatcherRegistry()
        val expectedSaveRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1")
            },
            response {
                assetBody("savedSearchesTest/saveSearchOnList.json")
            }
        )
        val expectedSubscribeRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1/subscription/push")
            },
            response {
                assetBody("savedSearchesTest/subscribeSavedSearchOnList.json")
            }
        )
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            subscribeButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnSaveSearchScreen {
            isNameEquals("Поиск по области карты")
            isGeoRegionButtonShown()
            isGeoDescriptionHidden()

            tapOn(lookup.matchesSaveSearchButton())
        }

        performOnSaveSearchResultScreen {
            waitUntil { isSuccessViewShown() }

            tapOn(lookup.matchesProceedToSavedSearchesButton())
        }

        performOnFavoritesScreen {
            isToolbarTitleShown()
            isSubscriptionsTabSelected()

            waitUntil { expectedSaveRequest.isOccured() }
            waitUntil { expectedSubscribeRequest.isOccured() }
        }
    }
}
