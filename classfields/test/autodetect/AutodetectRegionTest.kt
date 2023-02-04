package com.yandex.mobile.realty.test.autodetect

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SessionStartupRequiredRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test

/**
 * @author rogovalex on 30.07.2021.
 */
@LargeTest
class AutodetectRegionTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain = baseChainOf(
        activityTestRule,
        SetupDefaultAppStateRule(filterInitialized = false, regionAutodetectPerformed = false),
        SessionStartupRequiredRule()
    )

    @Test
    fun shouldAutodetectRegionWithPaidSites() {
        configureWebServer {
            registerRegionAutodetect()
            registerRegionInfo("AutodetectRegionTest/regionInfoSPBHasPaidSites.json")
            registerSitePointSearch()
            registerSiteOffersCount()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            mapView.waitUntil { isCompletelyDisplayed() }
            waitUntil { offersCountEquals(1, 2) }
        }
    }

    @Test
    fun shouldAutodetectRegionWithoutPaidSites() {
        configureWebServer {
            registerRegionAutodetect()
            registerRegionInfo("AutodetectRegionTest/regionInfoSPBHasNotPaidSites.json")
            register(
                request {
                    path("2.0/offers/number")
                    queryParam("objectType", "OFFER")
                },
                response {
                    setBody("{\"response\": {\"number\": 2}}")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            submitButton.waitUntil { containsText(" 2 ") }
        }
    }

    @Test
    fun shouldFallbackAutodetectRegionToDefaults() {
        configureWebServer {
            registerSitePointSearch()
            registerSiteOffersCount()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            mapView.waitUntil { isCompletelyDisplayed() }
            waitUntil { offersCountEquals(1, 2) }
        }
    }

    private fun DispatcherRegistry.registerRegionAutodetect() {
        register(
            request {
                path("1.0/regionAutodetect.json")
            },
            response {
                assetBody("AutodetectRegionTest/regionAutodetectSPB.json")
            }
        )
    }

    private fun DispatcherRegistry.registerRegionInfo(fileName: String) {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", "741965")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    private fun DispatcherRegistry.registerSitePointSearch() {
        register(
            request {
                path("2.0/newbuilding/pointSearch")
            },
            response {
                setBody(
                    """
                            {
                              "response": {
                                "slicing": {
                                  "total": 1
                                },
                                "logQueryId": "b7128fa3fad87a0c",
                                "url" : "offerSearchV2.json"
                              }
                            }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerSiteOffersCount() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("countOnly", "true")
                queryParam("objectType", "NEWBUILDING")
            },
            response {
                setBody("{\"response\": {\"total\": 2}}")
            }
        )
    }
}
