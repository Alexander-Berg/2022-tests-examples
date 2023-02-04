package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
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
import com.yandex.mobile.realty.domain.model.search.Filter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 28.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapSiteFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(filter = Filter.SiteApartment()),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeSiteFavoriteState() {
        testSiteSnippetFavoriteButton<SearchBottomSheetScreen>(
            siteId = SITE_ID,
            webServerConfiguration = {
                registerMapSearchWithOneSite()
                registerSite()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchMapScreen> {
                    waitUntil { mapView.isCompletelyDisplayed() }
                    mapView.moveTo(LATITUDE, LONGITUDE)
                    waitUntil { mapView.containsPlacemark(SITE_ID) }
                    mapView.clickOnPlacemark(SITE_ID)
                }
                onScreen<SearchBottomSheetScreen> {
                    waitUntil { bottomSheet.isCollapsed() }
                    siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SITE_ID).view },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "на карте"
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithOneSite() {
        register(
            request {
                path("2.0/newbuilding/pointSearch")
            },
            response {
                assetBody("MapSiteFavoriteButtonTest/newbuildingPointSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "1"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
