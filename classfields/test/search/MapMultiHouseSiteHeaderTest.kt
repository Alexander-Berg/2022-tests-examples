package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchBottomSheetScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.search.Filter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 8/5/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapMultiHouseSiteHeaderTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(filter = Filter.SellApartment()),
        activityTestRule
    )

    @Test
    fun shouldOpenSiteWhenHeaderPressed() {
        configureWebServer {
            registerMapSearchMultiHouse()
            registerSiteInfo("siteInfo.json")
            registerOffersWithSiteSearch()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(SNIPPET_OFFER_ID) }
            mapView.clickOnPlacemark(SNIPPET_OFFER_ID)

            val prefix = "MapMultiHouseSiteHeaderTest/shouldOpenSiteWhenHeaderPressed"
            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }

                catalogFullHeaderView
                    .waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches("$prefix/siteHeader")
                    .click()
            }
            onScreen<SiteCardScreen> {
                titleView.waitUntil { isTextEquals(SITE_NAME) }
                pressBack()
            }
            onScreen<SearchBottomSheetScreen> {
                headerSiteInfoButton.click()
            }
            onScreen<SiteCardScreen> {
                titleView.waitUntil { isTextEquals(SITE_NAME) }
            }
        }
    }

    @Test
    fun shouldShowMinimalSiteHeader() {
        configureWebServer {
            registerMapSearchMultiHouse()
            registerSiteInfo("siteInfoMinimal.json")
            registerOffersWithSiteSearch()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(SNIPPET_OFFER_ID) }
            mapView.clickOnPlacemark(SNIPPET_OFFER_ID)

            val prefix = "MapMultiHouseSiteHeaderTest/shouldShowMinimalSiteHeader"
            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }

                catalogFullHeaderView
                    .waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches("$prefix/siteHeader")
                    .click()
            }
        }
    }

    @Test
    fun shouldShowErrors() {
        configureWebServer {
            registerMapSearchMultiHouse()
            registerSiteInfoError()
            registerSiteInfo("siteInfo.json")
            registerOffersWithSiteSearchError()
            registerOffersWithSiteSearch()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(SNIPPET_OFFER_ID) }
            mapView.clickOnPlacemark(SNIPPET_OFFER_ID)

            val prefix = "MapMultiHouseSiteHeaderTest/shouldShowErrors"
            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }
                bottomSheet.expand()

                fullscreenErrorView.waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches("$prefix/errorState")
                    .retryButton
                    .click()

                waitUntil { catalogFullHeaderView.isCompletelyDisplayed() }
            }
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("MapMultiHouseSiteHeaderTest/offersWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearchError() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerSiteInfo(responseFile: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("MapMultiHouseSiteHeaderTest/$responseFile")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteInfoError() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerMapSearchMultiHouse() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("MapMultiHouseSiteHeaderTest/multiHouseStatisticSearch.json")
            }
        )
    }

    companion object {
        private const val SNIPPET_OFFER_ID = "1"
        private const val SITE_ID = "10"
        private const val SITE_NAME = "ЖК «Русский дом»"
        private const val LATITUDE = 67.43049
        private const val LONGITUDE = 32.70827
    }
}
