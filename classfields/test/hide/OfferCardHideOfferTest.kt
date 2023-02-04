package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
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
class OfferCardHideOfferTest {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldHideSimilarOfferWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerOffer()
            registerSimilarOffers()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            collapseAppBar()
            waitUntil { containsSimilarOfferSnippet(SIMILAR_OFFER_ID) }
            performOnSimilarOfferSnippet(SIMILAR_OFFER_ID) {
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
            waitUntil { doesNotContainSimilarOfferSnippet(SIMILAR_OFFER_ID) }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("hideOfferTest/cardWithViewsLarge.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("hideOfferTest/similar.json")
            }
        )
    }

    private fun DispatcherRegistry.registerHideOffer() {
        register(
            request {
                path("1.0/user/me/personalization/hideOffers")
                queryParam("offerId", SIMILAR_OFFER_ID)
            },
            response {
                setBody("{}")
            }
        )
    }

    private companion object {
        private const val OFFER_ID = "0"
        private const val SIMILAR_OFFER_ID = "1"
    }
}
