package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author andrikeev on 19/10/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ExcerptBriefReportTest {

    private val activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldShowUnauthorizedExcerptBriefReportBlock() {
        configureWebServer {
            registerSellApartmentOffer()
        }
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptBriefReportBlock() }
            isExcerptBriefReportBlockMatches("/ExcerptBriefReportTest/unauthorized")
        }
    }

    @Test
    fun shouldShowExcerptBriefReportBlock() {
        configureWebServer {
            registerSellApartmentOffer()
            registerExcerptBriefReport()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptBriefReportBlock() }
            isExcerptBriefReportBlockMatches("/ExcerptBriefReportTest/authorized")
        }
    }

    @Test
    fun shouldShowExcerptBriefReportBlockError() {
        configureWebServer {
            registerSellApartmentOffer()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptBriefReportBlock() }
            isExcerptBriefReportBlockMatches("/ExcerptBriefReportTest/error")
        }
    }

    @Test
    fun shouldShowExpandedExcerptReportBlock() {
        configureWebServer {
            registerSellApartmentOffer()
            registerExcerptBriefReport()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptBriefReportBlock() }
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesExcerptBriefReportButton())
            waitUntil { containsExpandedExcerptReportBlock() }
            isExpandedExcerptBriefReportBlockMatches("/ExcerptBriefReportTest/expanded", 6)
        }
    }

    @Test
    fun shouldShowExpandedExcerptReportForRentApartment() {
        configureWebServer {
            registerRentApartmentOffer()
            registerExcerptBriefReport()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsExcerptBriefReportBlock() }
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesExcerptBriefReportButton())
            waitUntil { containsExpandedExcerptReportBlock() }
            isExpandedExcerptBriefReportBlockMatches(
                "/ExcerptBriefReportTest/expandedRentApartment",
                5
            )
        }
    }

    private fun DispatcherRegistry.registerSellApartmentOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("excerpt/cardWithViewsSellApartment.json")
            }
        )
    }

    private fun DispatcherRegistry.registerRentApartmentOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("excerpt/cardWithViewsRentApartment.json")
            }
        )
    }

    private fun DispatcherRegistry.registerExcerptBriefReport() {
        register(
            request {
                path("2.0/excerpts/offer/0")
            },
            response {
                assetBody("excerpt/excerptBriefReport.json")
            }
        )
    }
}
