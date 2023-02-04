package com.yandex.mobile.realty.test.site

import com.yandex.mobile.realty.activity.SiteResellerOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnSiteResellerOffersScreen
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
import org.junit.runners.Parameterized

/**
 * Created by Alena Malchikhina on 08.05.2020
 */
@RunWith(Parameterized::class)
class SiteResellerOffersSortingTest(private val sorting: Sorting) {

    private var activityTestRule = SiteResellerOffersTestRule(
        siteId = SITE_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldChangeSearchListWhenSortingSelected() {
        configureWebServer {
            registerOfferWithSiteSearchSorting(
                Sorting.PRICE,
                "offerWithSiteSearchDefaultSorting.json"
            )
            registerOfferWithSiteSearchSorting(
                sorting,
                "offerWithSiteSearchSelectedSorting.json"
            )
        }

        activityTestRule.launchActivity()

        performOnSiteResellerOffersScreen {
            waitUntil { containsOffer("1") }
            tapOn(lookup.matchesSortingButton())
        }
        performOnSortingDialog {
            scrollToPosition(sorting.matcher.invoke()).tapOn()
        }
        performOnSiteResellerOffersScreen {
            containsOffer("0")
            doesNotContainsOffer("1")
        }
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearchSorting(
        sorting: Sorting,
        responseFileName: String
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
                queryParam("page", "0")
                queryParam("sort", sorting.value)
                queryParam("primarySale", "NO")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {
        private const val SITE_ID = "1"

        @JvmStatic
        @Parameterized.Parameters(name = "sort by {0}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(Sorting.PRICE),
                arrayOf(Sorting.AREA_DESC),
                arrayOf(Sorting.PRICE_PER_SQUARE),
                arrayOf(Sorting.PRICE_DESC),
                arrayOf(Sorting.AREA)
            )
        }
    }
}
