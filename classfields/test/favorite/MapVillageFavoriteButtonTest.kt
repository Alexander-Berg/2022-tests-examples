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
class MapVillageFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(filter = Filter.VillageHouse()),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeVillageFavoriteState() {
        testVillageSnippetFavoriteButton<SearchBottomSheetScreen>(
            villageId = VILLAGE_ID,
            webServerConfiguration = {
                registerMapSearchWithOneVillage()
                registerVillage()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchMapScreen> {
                    waitUntil { mapView.isCompletelyDisplayed() }
                    mapView.moveTo(LATITUDE, LONGITUDE)
                    waitUntil { mapView.containsPlacemark(VILLAGE_ID) }
                    mapView.clickOnPlacemark(VILLAGE_ID)
                }
                onScreen<SearchBottomSheetScreen> {
                    waitUntil { bottomSheet.isCollapsed() }
                    villageSnippet(VILLAGE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { villageSnippet(VILLAGE_ID).view },
            villageCategories = arrayListOf("Village_Sell", "Sell"),
            metricaSource = "на карте"
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithOneVillage() {
        register(
            request {
                path("2.0/village/pointSearch")
            },
            response {
                assetBody("MapVillageFavoriteButtonTest/villagePointSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerVillage() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {

        private const val VILLAGE_ID = "2"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
