package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PlanOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.PlanOffersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
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
class PlanOffersFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = PlanOffersActivityTestRule(
        siteId = SITE_ID,
        planId = PLAN_ID,
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
    fun shouldChangeOfferFavoriteState() {
        testOfferFavoriteButton<PlanOffersScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = {
                registerOfferStat()
                registerOffersCount()
                registerOfferWithSiteSearchPlanOffers()
            },
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<PlanOffersScreen> {
                    planOfferSnippet(OFFER_ID).waitUntil { listView.contains(this) }

                    // scroll to prevent favorite button from overlapping by snackbar
                    listView.scrollVerticallyBy(300)
                }
            },
            buttonViewSelector = { planOfferSnippet(OFFER_ID).view.favoriteButton },
            favAddedScreenshot = "PlanOffersFavoriteButtonTest/" +
                "shouldChangeOfferFavoriteState/added",
            favRemovedScreenshot = "PlanOffersFavoriteButtonTest/" +
                "shouldChangeOfferFavoriteState/removed",
            offerCategories = jsonArrayOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = jsonObject {
                "сниппет объявления" to "на экране планировки"
            }
        )
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                assetBody("offerStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffersCount() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("countOnly", "true")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                setBody("{\"response\":{\"total\":3}}")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearchPlanOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SITE_ID = "2"
        private const val PLAN_ID = "6"
    }
}
