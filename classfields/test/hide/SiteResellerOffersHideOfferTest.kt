package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SiteResellerOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.robot.performOnSiteResellerOffersScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
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
 * @author andrikeev on 19/01/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteResellerOffersHideOfferTest {

    private val activityTestRule = SiteResellerOffersTestRule(
        siteId = "1",
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldHideOfferWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerOffersWithSiteSearch()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        performOnSiteResellerOffersScreen {
            waitUntil { containsOffer(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
                performOnOfferMenuDialog {
                    isHideButtonShown()
                    tapOn(lookup.matchesHideButton())
                    performOnConfirmationDialog {
                        isTitleEquals(getResourceString(R.string.hide_offer_confirmation_title))
                        isMessageEquals(getResourceString(R.string.hide_offer_confirmation))
                        confirm()
                    }
                }
            }
            waitUntil { doesNotContainsOffer(OFFER_ID) }
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("hideOfferTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerHideOffer() {
        register(
            request {
                path("1.0/user/me/personalization/hideOffers")
                queryParam("offerId", OFFER_ID)
            },
            response {
                setBody("{}")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "1"
    }
}
