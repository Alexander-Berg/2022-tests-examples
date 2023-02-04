package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FavoriteListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.FavoriteOffersScreen
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
 * @author shpigun on 27.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteListFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = FavoriteListActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldRemoveOfferFromFavorite() {
        val dispatcher = DispatcherRegistry()
        val expectedRemoveFavoritesRequest = dispatcher.registerOfferFavoritesPatch(
            removedId = OFFER_ID
        )
        dispatcher.registerOfferFavorites()
        dispatcher.registerOfferFavorites()
        dispatcher.registerOffer()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<FavoriteOffersScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .favoriteButton
                .click()

            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(
                    "FavoriteListFavoriteButtonTest/shouldRemoveOfferFromFavorite/confirmation"
                )
                confirmButton.click()
            }
            waitUntil { expectedRemoveFavoritesRequest.isOccured() }
            offerSnippet(OFFER_ID).waitUntil { listView.doesNotContain(this) }
        }
    }

    @Test
    fun shouldRemoveSiteFromFavorite() {
        val dispatcher = DispatcherRegistry()
        val expectedRemoveFavoritesRequest = dispatcher.registerSiteFavoritesPatch(
            removedId = SITE_ID
        )
        dispatcher.registerSiteFavorites()
        dispatcher.registerSiteFavorites()
        dispatcher.registerSiteFavorites()
        dispatcher.registerSite()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<FavoriteOffersScreen> {
            selectorListView
                .waitUntil { listView.contains(this) }
                .scrollTo(siteSelectorItem)
                .click()
            siteSnippet(SITE_ID)
                .waitUntil { listView.contains(this) }
                .favoriteButton
                .click()
            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(
                    "FavoriteListFavoriteButtonTest/shouldRemoveSiteFromFavorite/confirmation"
                )
                confirmButton.click()
            }
            waitUntil { expectedRemoveFavoritesRequest.isOccured() }
            siteSnippet(SITE_ID).waitUntil { listView.doesNotContain(this) }
        }
    }

    @Test
    fun shouldRemoveVillageFromFavorite() {
        val screenshotPath = "FavoriteListFavoriteButtonTest/shouldRemoveVillageFromFavorite/" +
            "confirmation"
        val dispatcher = DispatcherRegistry()
        val expectedRemoveFavoritesRequest = dispatcher.registerVillageFavoritesPatch(
            removedId = VILLAGE_ID
        )
        dispatcher.registerVillageFavorites()
        dispatcher.registerVillageFavorites()
        dispatcher.registerVillageFavorites()
        dispatcher.registerVillage()
        configureWebServer(dispatcher)
        activityTestRule.launchActivity()
        onScreen<FavoriteOffersScreen> {
            selectorListView
                .waitUntil { listView.contains(this) }
                .scrollTo(villageSelectorItem)
                .click()
            villageSnippet(VILLAGE_ID)
                .waitUntil { listView.contains(this) }
                .favoriteButton
                .click()
            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches(screenshotPath)
                confirmButton.click()
            }
            waitUntil { expectedRemoveFavoritesRequest.isOccured() }
            villageSnippet(VILLAGE_ID).waitUntil { listView.doesNotContain(this) }
        }
    }

    private fun DispatcherRegistry.registerOfferFavorites() {
        register(
            request {
                path("1.0/favorites.json")
            },
            response {
                setBody("{\"response\": {\"relevant\": [\"$OFFER_ID\"]}}")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteFavorites() {
        register(
            request {
                path("1.0/favorites.json")
            },
            response {
                setBody("{\"response\": {\"relevant\": [\"site_$SITE_ID\"]}}")
            }
        )
    }

    private fun DispatcherRegistry.registerVillageFavorites() {
        register(
            request {
                path("1.0/favorites.json")
            },
            response {
                setBody("{\"response\": {\"relevant\": [\"village_$VILLAGE_ID\"]}}")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
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
