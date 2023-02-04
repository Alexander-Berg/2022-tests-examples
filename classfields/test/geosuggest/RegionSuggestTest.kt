package com.yandex.mobile.realty.test.geosuggest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.RegionSuggestActivityTestRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RegionSuggestScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
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
 * @author misha-kozlov on 3/15/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RegionSuggestTest {

    private val activityTestRule = RegionSuggestActivityTestRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(activityTestRule)

    @Test
    fun shouldShowEmptySuggest() {
        configureWebServer {
            registerRegionList()
            registerEmptySuggest()
        }

        activityTestRule.launchActivity()

        onScreen<RegionSuggestScreen> {
            waitUntil { listView.contains(regionSuggest("Москва")) }

            searchView.typeText(TEXT)

            waitUntil { emptyView.isCompletelyDisplayed() }
            isRegionSuggestStateMatches("RegionSuggestTest/shouldShowEmptySuggest/emptySuggest")
        }
    }

    private fun DispatcherRegistry.registerRegionList() {
        register(
            request {
                path("1.0/regionList.json")
                queryParam("text", "")
            },
            response {
                assetBody("geoSuggestTest/regionList.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptySuggest() {
        register(
            request {
                path("1.0/regionList.json")
                queryParam("text", TEXT)
            },
            response {
                setBody(
                    """{
                                "response" : {
                                    "predefinedRegions" : [],
                                    "items" : [],
                                    "sort" : "RELEVANCE",
                                    "pager" : {
                                        "page" : 0,
                                        "pageSize" : 10,
                                        "totalPages" : 1,
                                        "totalItems" : 0
                                    }
                                }   
                            }"""
                )
            }
        )
    }

    companion object {

        private const val TEXT = "text"
    }
}
