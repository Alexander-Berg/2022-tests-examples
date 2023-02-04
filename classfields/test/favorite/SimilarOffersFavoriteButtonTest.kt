package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SimilarOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimilarOffersScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 12.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SimilarOffersFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = SimilarOffersActivityTestRule(
        offerId = OFFER_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeOfferFavoriteState() {
        testOfferSnippetFavoriteButton<SimilarOffersScreen>(
            offerId = SIMILAR_OFFER_ID,
            webServerConfiguration = {
                registerOffer()
                registerSimilarOffers()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SimilarOffersScreen> {
                    offerSnippet(SIMILAR_OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(SIMILAR_OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "в листинге похожих объявлений"
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
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
