package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnSearchListScreen
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
 * Created by Alena Malchikhina on 02.03.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListSnippetStatusTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun testSearchListSiteSnippetStatusSoonAvailable() {
        configureWebServer {
            registerOfferWithSiteSearch("offerWithSiteSearchSoonAvailable.json")
        }

        activityTestRule.launchActivity()

        performOnSearchListScreen {
            waitUntil { containsSiteSnippet("0") }

            isSnippetFlatStatusShown("Скоро в продаже")
        }
    }

    @Test
    fun testSearchListSiteSnippetStatusInProject() {
        configureWebServer {
            registerOfferWithSiteSearch("offerWithSiteSearchInProject.json")
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            siteSnippet("0")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches("SearchListSnippetStatusTest/statusInProject")
                }
        }
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearch(responseFileName: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("searchListSnippetStatusTest/$responseFileName")
            }
        )
    }
}
