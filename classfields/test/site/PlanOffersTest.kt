package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PlanOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnPlanOffersScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.PlanOffersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 09.10.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlanOffersTest : BaseTest() {

    private val activityTestRule = PlanOffersActivityTestRule(
        siteId = SITE_ID,
        planId = PLAN_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val rule: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldOpenOfferCardWhenSnippetPressed() {
        configureWebServer {
            registerOfferStat()
            registerOffersCount()
            registerOfferWithSiteSearchPlanOffers()
            registerCardWithViews()
        }

        activityTestRule.launchActivity()

        performOnPlanOffersScreen {
            collapseAppBar()
            waitUntil { containsOfferSnippet("8") }

            tapOn(lookup.matchesSnippetView("8"))
        }

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("1\u00a0531\u00a0231\u00a0\u20BD") }
        }
    }

    @Test
    fun shouldDisplayVirtualTourBadge() {
        configureWebServer {
            registerOfferWithSiteSearchPlanOffers(
                response = "PlanOffersTest/offersWithVirtualTours.json",
            )
        }

        activityTestRule.launchActivity()

        onScreen<PlanOffersScreen> {
            planOfferSnippet("0")
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("snippetWithVirtualTourBadge"))
            planOfferSnippet("1")
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("snippetWithoutVirtualTourBadge"))
        }
    }

    private fun DispatcherRegistry.registerOfferStat(response: String = "offerStat.json") {
        register(
            request {
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                assetBody("siteCardTest/$response")
            }
        )
    }

    private fun DispatcherRegistry.registerOffersCount() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("showOnMobile", "YES")
                queryParam("showSimilar", "NO")
                queryParam("currency", "RUR")
                queryParam("clusterHead", "NO")
                queryParam("countOnly", "true")
                queryParam("siteFlatPlanId", PLAN_ID)
                queryParam("type", "SELL")
                queryParam("category", "APARTMENT")
                queryParam("objectType", "OFFER")
                queryParam("priceType", "PER_OFFER")
            },
            response {
                setBody("{\"response\":{\"total\":3}}")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearchPlanOffers(
        response: String = "siteCardTest/offerWithSiteSearchPlanOffers.json",
        buildings: Set<String>? = null
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("sort", "PRICE")
                queryParam("category", "APARTMENT")
                queryParam("clusterHead", "NO")
                queryParam("showOnMobile", "YES")
                queryParam("showSimilar", "NO")
                queryParam("type", "SELL")
                queryParam("pageSize", "10")
                queryParam("siteFlatPlanId", PLAN_ID)
                buildings?.forEach { id -> queryParam("houseId", id) }
            },
            response {
                assetBody(response)
            }
        )
    }

    private fun DispatcherRegistry.registerCardWithViews() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("cardWithViews.json")
            }
        )
    }

    companion object {
        private const val SITE_ID = "2"
        private const val PLAN_ID = "6"
    }
}
