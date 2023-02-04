package com.yandex.mobile.realty.test.village

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.VillageOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.VillageOffersScreen
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
class VillageOffersTest {

    private val activityTestRule = VillageOffersTestRule("0")

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldShowEmptyView() {
        configureWebServer {
            registerEmptyOffers()
        }

        activityTestRule.launchActivity()

        onScreen<VillageOffersScreen> {
            waitUntil { listView.contains(emptyItem) }

            root.isViewStateMatches("VillageOffersTest/shouldShowEmptyView/emptyList")
        }
    }

    @Test
    fun shouldShowErrorView() {
        activityTestRule.launchActivity()

        onScreen<VillageOffersScreen> {
            waitUntil { listView.contains(errorItem) }

            root.isViewStateMatches("VillageOffersTest/shouldShowErrorView/errorScreen")
        }
    }

    @Test
    fun shouldLoadSecondPage() {
        configureWebServer {
            registerFirstPage()
            registerSecondPage()
        }

        activityTestRule.launchActivity()

        onScreen<VillageOffersScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.scrollTo(offerSnippet("3"))

            waitUntil { listView.contains(offerSnippet("4")) }
        }
    }

    private fun DispatcherRegistry.registerEmptyOffers() {
        register(
            request {
                path("2.0/village/0/offers")
            },
            response {
                setBody(
                    """
                                {
                                  "response": {
                                    "offers": [],
                                    "slicing": {
                                      "total": 0,
                                      "page": {
                                        "num": 0,
                                        "size": 0
                                      }
                                    },
                                    "logQueryId": "b09c30e7b40e40a021ff7eefdde06f16",
                                    "url" : "offerSearchV2.json"
                                  }
                                }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFirstPage() {
        register(
            request {
                path("2.0/village/0/offers")
                queryParam("page", "0")
            },
            response {
                assetBody("villageOffersTest/firstPage.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSecondPage() {
        register(
            request {
                path("2.0/village/0/offers")
                queryParam("page", "1")
            },
            response {
                assetBody("villageOffersTest/secondPage.json")
            }
        )
    }
}
