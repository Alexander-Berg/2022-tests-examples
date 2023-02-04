package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonPrimitive
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchBottomSheetScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 28.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapOffersFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeMapOfferFavoriteState() {
        testOfferSnippetFavoriteButton<SearchBottomSheetScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = {
                registerMapSearchWithOneOffer()
                registerOffer()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchMapScreen> {
                    waitUntil { mapView.isCompletelyDisplayed() }
                    mapView.moveTo(LATITUDE, LONGITUDE)
                    waitUntil { mapView.containsPlacemark(OFFER_ID) }
                    mapView.clickOnPlacemark(OFFER_ID)
                }
                onScreen<SearchBottomSheetScreen> {
                    waitUntil { bottomSheet.isCollapsed() }
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "на карте"
        )
    }

    @Test
    fun shouldChangeMultiHouseOfferFavoriteState() {
        testOfferSnippetFavoriteButton<SearchBottomSheetScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = {
                registerMapSearchWithMultiHouse()
                registerOffer()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchMapScreen> {
                    waitUntil { mapView.isCompletelyDisplayed() }
                    mapView.moveTo(LATITUDE, LONGITUDE)
                    waitUntil { mapView.containsPlacemark(MULTI_HOUSE_ID) }
                    mapView.clickOnPlacemark(MULTI_HOUSE_ID)
                }
                onScreen<SearchBottomSheetScreen> {
                    waitUntil { bottomSheet.isCollapsed() }
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(sortingButtonItem) }
                    bottomSheet.expand()
                    waitUntil { bottomSheet.isExpanded() }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "на карте"
        )
    }

    @Test
    fun shouldChangeMultiHouseSiteHeaderFavoriteState() {
        val prefix = "MapOffersFavoriteButtonTest/shouldChangeMultiHouseSiteHeaderFavoriteState"
        testSiteFavoriteButton<SearchBottomSheetScreen>(
            siteId = SITE_ID,
            webServerConfiguration = {
                registerMapSearchMultiHouseWithSite()
                registerSiteInfo()
                registerOffer()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchMapScreen> {
                    waitUntil { mapView.isCompletelyDisplayed() }
                    mapView.moveTo(LATITUDE, LONGITUDE)
                    waitUntil { mapView.containsPlacemark(MULTI_HOUSE_ID) }
                    mapView.clickOnPlacemark(MULTI_HOUSE_ID)
                    onScreen<SearchBottomSheetScreen> {
                        waitUntil { bottomSheet.isCollapsed() }
                        waitUntil { headerSiteFavoriteButton.isCompletelyDisplayed() }
                    }
                }
            },
            buttonViewSelector = { headerSiteFavoriteButton },
            favAddedScreenshot = "$prefix/added",
            favRemovedScreenshot = "$prefix/removed",
            siteCategories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = JsonPrimitive("шапка многодома на карте")
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithOneOffer() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("MapOffersFavoriteButtonTest/pointStatisticSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithMultiHouse() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("MapOffersFavoriteButtonTest/pointStatisticSearchMultiHouse.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteInfo() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("MapOffersFavoriteButtonTest/siteInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchMultiHouseWithSite() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                val prefix = "MapOffersFavoriteButtonTest"
                assetBody("$prefix/pointStatisticSearchMultiHouseWithSite.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SITE_ID = "10"
        private const val MULTI_HOUSE_ID = "3"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
