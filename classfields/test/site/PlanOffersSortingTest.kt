package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PlanOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnPlanOffersScreen
import com.yandex.mobile.realty.core.robot.performOnSortingDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.search.Sorting
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 05.11.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlanOffersSortingTest {
    private val activityTestRule = PlanOffersActivityTestRule(
        siteId = SITE_ID,
        planId = PLAN_ID,
        launchActivity = false
    )

    private val sortValues = listOf(
        Sorting.PRICE,
        Sorting.PRICE_DESC,
        Sorting.FLOOR,
        Sorting.FLOOR_DESC,
        Sorting.COMMISSIONING_DATE,
        Sorting.COMMISSIONING_DATE_DESC,
    )

    @JvmField
    @Rule
    val rule: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldChangeOffersListWhenSortingSelected() {
        configureWebServer {
            registerOfferStat()
            registerOffersCount()

            for (sort in sortValues) {
                registerPlanOffers(sort)
            }
        }

        activityTestRule.launchActivity()

        performOnPlanOffersScreen {
            collapseAppBar()

            for (sort in sortValues) {
                tapOn(lookup.matchesSortingButton(), true)

                performOnSortingDialog {
                    scrollToPosition(sort.matcher.invoke()).tapOn()
                }
                waitUntil { containsOfferSnippet(sort.ordinal.toString()) }
            }
        }
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                assetBody("siteCardTest/offerStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerPlanOffers(sorting: Sorting) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("sort", sorting.value)
                queryParam("category", "APARTMENT")
                queryParam("clusterHead", "NO")
                queryParam("showOnMobile", "YES")
                queryParam("showSimilar", "NO")
                queryParam("type", "SELL")
                queryParam("pageSize", "10")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                setBody(createResponse(sorting.ordinal))
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
                setBody("{\"response\":{\"total\":1}}")
            }
        )
    }

    private fun createResponse(id: Int): String {
        return """
            {
                "response": {
                    "searchQuery": {
                        "logQueryId": "e",
                        "url" : "offerSearchV2.json"
                    },
                    "offers": {
                        "items": [
                            {
                                "offerId": $id,
                                "offerType": "SELL",
                                "offerCategory": "APARTMENT",
                                "price": {
                                    "price": {
                                        "value": 1531231,
                                        "currency": "RUB",
                                        "priceType": "PER_OFFER",
                                        "pricingPeriod": "WHOLE_LIFE"
                                    },
                                    "pricePerPart": {
                                        "value": 113700,
                                        "currency": "RUB",
                                        "priceType": "PER_METER",
                                        "pricingPeriod": "WHOLE_LIFE"
                                    }
                                }
                            }
                        ]
                    },
                    "timeStamp": "2020-10-09T16:56:56.844Z",
                    "pager": {
                        "totalItems": 1
                    }
                }
            }
        """.trimIndent()
    }

    companion object {
        private const val SITE_ID = "2"
        private const val PLAN_ID = "6"
    }
}
