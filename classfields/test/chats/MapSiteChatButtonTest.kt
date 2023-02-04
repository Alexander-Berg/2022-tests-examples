package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchBottomSheetScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.search.Filter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 3/24/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapSiteChatButtonTest : ChatButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(filter = Filter.SiteApartment()),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldStartCallWhenMapSiteCallButtonPressed() {
        configureWebServer {
            registerMapSearchWithOneSite()
            registerSite()
            registerSnippetSiteChat()
            registerSnippetSiteChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(SNIPPET_SITE_ID) }
            mapView.clickOnPlacemark(SNIPPET_SITE_ID)

            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }

                siteSnippet(SNIPPET_SITE_ID)
                    .waitUntil { listView.contains(this) }
                    .chatButton
                    .click()
            }
        }

        checkSnippetSiteChatViewState()
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("ChatButtonTest/siteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithOneSite() {
        register(
            request {
                path("2.0/newbuilding/pointSearch")
            },
            response {
                assetBody("ChatButtonTest/newbuildingPointSearch.json")
            }
        )
    }

    companion object {
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
