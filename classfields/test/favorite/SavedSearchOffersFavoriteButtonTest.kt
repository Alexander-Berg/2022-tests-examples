package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SavedSearchOfferListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddSavedSearchesEntryStatement
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.SavedSearchOffersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createStoredSavedSearch
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 28.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedSearchOffersFavoriteButtonTest : FavoriteButtonTest() {

    private val search = createStoredSavedSearch(SEARCH_ID)

    private val activityTestRule = SavedSearchOfferListActivityTestRule(
        SEARCH_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        DatabaseRule(createAddSavedSearchesEntryStatement(search)),
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeOfferFavoriteState() {
        testOfferSnippetFavoriteButton<SavedSearchOffersScreen>(
            offerId = OFFER_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SavedSearchOffersScreen> {
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            webServerConfiguration = { registerOffer() },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "в сохраненном поиске"
        )
    }

    @Test
    fun shouldChangeSiteFavoriteState() {
        testSiteSnippetFavoriteButton<SavedSearchOffersScreen>(
            siteId = SITE_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SavedSearchOffersScreen> {
                    siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SITE_ID).view },
            webServerConfiguration = { registerSite() },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "в сохраненном поиске"
        )
    }

    @Test
    fun shouldChangeVillageFavoriteState() {
        testVillageSnippetFavoriteButton<SavedSearchOffersScreen>(
            villageId = VILLAGE_ID,
            webServerConfiguration = { registerVillage() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SavedSearchOffersScreen> {
                    villageSnippet(VILLAGE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { villageSnippet(VILLAGE_ID).view },
            villageCategories = arrayListOf("Village_Sell", "Sell"),
            metricaSource = "в сохраненном поиске"
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

        private const val SEARCH_ID = "a"
        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"
    }
}
