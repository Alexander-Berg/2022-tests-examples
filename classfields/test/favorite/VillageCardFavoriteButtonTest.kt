package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonPrimitive
import com.yandex.mobile.realty.activity.VillageCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.VillageCardScreen
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
 * @author shpigun on 11.05.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VillageCardFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = VillageCardActivityTestRule(
        villageId = VILLAGE_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val rule: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeFavoriteStateWhenToolbarButtonClicked() {
        testVillageFavoriteButton<VillageCardScreen>(
            villageId = VILLAGE_ID,
            webServerConfiguration = { registerVillageCard() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<VillageCardScreen> {
                    favoriteButton.waitUntil { isCompletelyDisplayed() }
                }
            },
            buttonViewSelector = { favoriteButton },
            favAddedScreenshot = "FavoriteButtonTest/testWhiteToolbarFavoriteButton/added",
            favRemovedScreenshot = "FavoriteButtonTest/testWhiteToolbarFavoriteButton/removed",
            villageCategories = jsonArrayOf("Village_Sell", "Sell"),
            metricaSource = JsonPrimitive("карточка КП")
        )
    }

    @Test
    fun shouldChangeFavoriteStateWhenGalleryFavoriteButtonPressed() {
        testVillageFavoriteButton<GalleryScreen>(
            villageId = VILLAGE_ID,
            webServerConfiguration = { registerVillageCard() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<VillageCardScreen> {
                    galleryItem.waitUntil { listView.contains(this) }
                    gallery.click()
                }
                onScreen<GalleryScreen> {
                    favoriteButton.waitUntil { isCompletelyDisplayed() }
                }
            },
            buttonViewSelector = { favoriteButton },
            favAddedScreenshot = TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_ADDED,
            favRemovedScreenshot = TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_REMOVED_WITHOUT_SHADOW,
            villageCategories = jsonArrayOf("Village_Sell", "Sell"),
            metricaSource = JsonPrimitive("галерея у КП")
        )
    }

    @Test
    fun shouldChangeDeveloperVillagesFavoriteState() {
        testVillageSnippetFavoriteButton<VillageCardScreen>(
            villageId = VILLAGE_SNIPPET_ID,
            webServerConfiguration = {
                registerVillageCard()
                registerDeveloperVillages()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<VillageCardScreen> {
                    appBar.collapse()
                    villageSnippet(VILLAGE_SNIPPET_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { villageSnippet(VILLAGE_SNIPPET_ID).view },
            villageCategories = arrayListOf("Village_Sell", "Sell"),
            metricaSource = "в блоке других объектов от застройщика"
        )
    }

    private fun DispatcherRegistry.registerVillageCard() {
        register(
            request {
                path("2.0/village/$VILLAGE_ID/card")
            },
            response {
                assetBody("villageCard.json")
            }
        )
    }

    private fun DispatcherRegistry.registerDeveloperVillages() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {

        private const val VILLAGE_ID = "0"
        private const val VILLAGE_SNIPPET_ID = "2"
    }
}
