package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.VillageOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.VillageOffersScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 28.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VillageOffersFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = VillageOffersTestRule(VILLAGE_ID)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeOfferFavoriteState() {
        testOfferFavoriteButton<VillageOffersScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = { registerOffers() },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<VillageOffersScreen> {
                    offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
                }
            },
            buttonViewSelector = { offerSnippet(OFFER_ID).view.favoriteButton },
            favAddedScreenshot = "VillageOffersFavoriteButtonTest/" +
                "shouldChangeOfferFavoriteState/added",
            favRemovedScreenshot = "VillageOffersFavoriteButtonTest/" +
                "shouldChangeOfferFavoriteState/removed",
            offerCategories = jsonArrayOf("LotInVillage_Sell", "Sell"),
            metricaSource = jsonObject {
                "сниппет объявления" to "в листинге внутри КП"
            }
        )
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("2.0/village/$VILLAGE_ID/offers")
                queryParam("page", "0")
            },
            response {
                assetBody("VillageOffersFavoriteButtonTest/villageOffer.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val VILLAGE_ID = "2"
    }
}
