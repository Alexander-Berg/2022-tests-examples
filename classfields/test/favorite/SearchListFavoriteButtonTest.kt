package com.yandex.mobile.realty.test.favorite

import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author shpigun on 22.04.2021
 */
class SearchListFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeOfferFavoriteState() {
        testOfferSnippetFavoriteButton<SearchListScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = { registerOffer() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchListScreen> {
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "в листинге"
        )
    }

    @Test
    fun shouldChangeSiteFavoriteState() {
        testSiteSnippetFavoriteButton<SearchListScreen>(
            siteId = SITE_ID,
            webServerConfiguration = { registerSite() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchListScreen> {
                    siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SITE_ID).view },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "в листинге"
        )
    }

    @Test
    fun shouldChangeVillageFavoriteState() {
        testVillageSnippetFavoriteButton<SearchListScreen>(
            villageId = VILLAGE_ID,
            webServerConfiguration = { registerVillage() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SearchListScreen> {
                    villageSnippet(VILLAGE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { villageSnippet(VILLAGE_ID).view },
            villageCategories = arrayListOf("Village_Sell", "Sell"),
            metricaSource = "в листинге"
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    private fun DispatcherRegistry.registerVillage() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"
    }
}
