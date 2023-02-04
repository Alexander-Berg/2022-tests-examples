package com.yandex.mobile.realty.test.offer.businesscenter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 10/11/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CommercialBcTest {

    private val activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowCommercialBusinessCenterInfo() {
        configureWebServer {
            registerOffer()
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { isFloatingCallButtonShown() }

            isCommercialBcDetailsMatches(
                "Бизнес-центр «Vavilov Tower»",
                "/CommercialBcTest/shouldShowCommercialBusinessCenterInfo/detailsCollapsed",
                8
            )

            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesShowMoreButton("ещё 12 особенностей"))

            isCommercialBcDetailsMatches(
                "Бизнес-центр «Vavilov Tower»",
                "/CommercialBcTest/shouldShowCommercialBusinessCenterInfo/detailsExpanded",
                20
            )

            isCommercialBcFacilitiesMatches(
                "/CommercialBcTest/shouldShowCommercialBusinessCenterInfo/facilitiesCollapsed",
                6
            )

            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesShowMoreButton("ещё 19 удобств"))

            isCommercialBcFacilitiesMatches(
                "/CommercialBcTest/shouldShowCommercialBusinessCenterInfo/facilitiesExpanded",
                24
            )
        }
    }

    @Test
    fun shouldShowCommercialBusinessCenterOffers() {
        configureWebServer {
            registerOffer()
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("type", "SELL")
                    queryParam("category", "COMMERCIAL")
                    queryParam("commercialBuildingId", "2255907")
                    queryParam("rgid", "165705")
                    queryParam("commercialBuildingType", "BUSINESS_CENTER")
                    excludeQueryParamKey("countOnly")
                },
                response {
                    assetBody("CommercialBcTest/offerWithSiteSearchBc.json")
                }
            )
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { isFloatingCallButtonShown() }

            scrollToPosition(lookup.matchesCommercialBcOffersButton())
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesCommercialBcOffersButton())
        }

        onScreen<SearchListScreen> {
            offerSnippet("1").waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("CommercialBcTest/cardWithViews.json")
            }
        )
    }
}
