package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.ModelType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 05.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SharedOffersFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.OFFER,
        objectIds = listOf(OFFER_ID),
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
        testOfferSnippetFavoriteButton<SharedFavoritesScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = { registerOffers() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<SharedFavoritesScreen> {
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "на экране расшаренного избранного"
        )
    }

    private fun DispatcherRegistry.registerOffers() {
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

    companion object {
        private const val OFFER_ID = "0"
    }
}
