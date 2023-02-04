package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonPrimitive
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
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
class SiteCardFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
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
        testSiteFavoriteButton<SiteCardScreen>(
            siteId = SITE_ID,
            webServerConfiguration = { registerSiteWithOfferStat() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SiteCardScreen> {
                    favoriteButton.waitUntil { isCompletelyDisplayed() }
                }
            },
            buttonViewSelector = { favoriteButton },
            favAddedScreenshot = WHITE_TOOLBAR_FAVORITE_BUTTON_ADDED,
            favRemovedScreenshot = WHITE_TOOLBAR_FAVORITE_BUTTON_REMOVED,
            siteCategories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = JsonPrimitive("карточка новостройки")
        )
    }

    @Test
    fun shouldChangeFavoriteStateWhenGalleryFavoriteButtonPressed() {
        testSiteFavoriteButton<GalleryScreen>(
            siteId = SITE_ID,
            webServerConfiguration = { registerSiteWithOfferStat() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SiteCardScreen> {
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
            siteCategories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = JsonPrimitive("галерея у новостройки")
        )
    }

    @Test
    fun shouldChangeSimilarSiteFavoriteState() {
        testSiteSnippetFavoriteButton<SiteCardScreen>(
            siteId = SNIPPET_SITE_ID,
            webServerConfiguration = {
                registerSiteWithOfferStat()
                registerSimilarSites()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SiteCardScreen> {
                    appBar.collapse()
                    siteSnippet(SNIPPET_SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SNIPPET_SITE_ID).view },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "в блоке похожих ЖК"
        )
    }

    @Test
    fun shouldChangeDeveloperSiteFavoriteState() {
        testSiteSnippetFavoriteButton<SiteCardScreen>(
            siteId = SNIPPET_SITE_ID,
            webServerConfiguration = {
                registerSiteWithOfferStat()
                registerDeveloperSites()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SiteCardScreen> {
                    appBar.collapse()
                    siteSnippet(SNIPPET_SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SNIPPET_SITE_ID).view },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "в блоке других объектов от застройщика"
        )
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat() {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteCardFavoriteButtonTest/siteWithOfferStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarSites() {
        register(
            request {
                path("1.0/newbuilding/siteLikeSearch")
            },
            response {
                assetBody("SiteCardFavoriteButtonTest/siteLikeSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerDeveloperSites() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("SiteCardFavoriteButtonTest/offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "1"
        private const val SNIPPET_SITE_ID = "2"
    }
}
