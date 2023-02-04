package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonPrimitive
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
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
class OfferCardFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeFavoriteStateWhenToolbarButtonClicked() {
        testOfferFavoriteButton<OfferCardScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = {
                registerOffer()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<OfferCardScreen> {
                    toolbarFavoriteButton.waitUntil { isCompletelyDisplayed() }
                }
            },
            buttonViewSelector = { toolbarFavoriteButton },
            favAddedScreenshot = TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_ADDED,
            favRemovedScreenshot = TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_REMOVED_WITH_SHADOW,
            offerCategories = jsonArrayOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = JsonPrimitive("карточка объявления")
        )
    }

    @Test
    fun shouldChangeFavoriteStateWhenContentButtonClicked() {
        testOfferFavoriteButton<OfferCardScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = { registerOffer() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<OfferCardScreen> {
                    appBar.collapse()
                    complainAndFavoriteItem.waitUntil { listView.contains(this) }
                }
            },
            buttonViewSelector = { favoriteButton },
            favAddedScreenshot = "OfferCardFavoriteButtonTest/" +
                "shouldChangeFavoriteStateWhenContentButtonClicked/added",
            favRemovedScreenshot = "OfferCardFavoriteButtonTest/" +
                "shouldChangeFavoriteStateWhenContentButtonClicked/removed",
            offerCategories = jsonArrayOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = JsonPrimitive("карточка объявления")
        )
    }

    @Test
    fun shouldChangeSimilarOfferFavoriteState() {
        testOfferSnippetFavoriteButton<OfferCardScreen>(
            offerId = SIMILAR_OFFER_ID,
            webServerConfiguration = {
                registerOffer()
                registerSimilarOffers()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<OfferCardScreen> {
                    appBar.collapse()
                    similarOfferSnippet(SIMILAR_OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { similarOfferSnippet(SIMILAR_OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "в блоке похожих объявлений"
        )
    }

    @Test
    fun shouldChangeFavoriteStateWhenGalleryFavoriteButtonPressed() {
        testOfferFavoriteButton<GalleryScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = {
                registerOfferWithImages()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<OfferCardScreen> {
                    photoCounter.waitUntil { isCompletelyDisplayed() }
                    galleryView.click()
                }
                onScreen<GalleryScreen> {
                    favoriteButton.waitUntil { isCompletelyDisplayed() }
                }
            },
            buttonViewSelector = { favoriteButton },
            favAddedScreenshot = TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_ADDED,
            favRemovedScreenshot = TRANSPARENT_TOOLBAR_FAVORITE_BUTTON_REMOVED_WITHOUT_SHADOW,
            offerCategories = jsonArrayOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = JsonPrimitive("галерея с фото")
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("OfferCardFavoriteButtonTest/cardWithViews.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithImages() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("OfferCardFavoriteButtonTest/cardWithViewsWithImages.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("OfferCardFavoriteButtonTest/similar.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SIMILAR_OFFER_ID = "1"
    }
}
