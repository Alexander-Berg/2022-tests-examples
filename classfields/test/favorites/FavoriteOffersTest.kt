package com.yandex.mobile.realty.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BottomNavMenu
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author scrooge on 21.04.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteOffersTest {

    private var activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowEmptyFavoritesList() {
        configureWebServer {
            registerFavoriteIds()
            registerFavoriteIds()
        }

        activityTestRule.launchActivity()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }

            favItemView.click(true)
        }

        performOnFavoritesScreen {
            isToolbarTitleShown()
            isFavoriteOffersTabSelected()

            performOnFavoriteOffersScreen {
                waitUntil { isEmptyViewShown() }
                isEmptyViewImageShown()
                isEmptyViewTitleShown()
                isEmptyViewDescriptionShown()
                isEmptyViewLoginButtonShown()
            }
        }
    }

    private fun DispatcherRegistry.registerFavoriteIds() {
        register(
            request {
                path("1.0/favorites.json")
            },
            response {
                setBody("{\"response\": {\"actual\": [],\"outdated\": [],\"relevant\": []}}")
            }
        )
    }
}
