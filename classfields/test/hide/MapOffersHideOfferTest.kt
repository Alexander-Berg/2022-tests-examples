package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.robot.performOnSearchBottomSheetScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
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
class MapOffersHideOfferTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldHideBottomSheetWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerMapSearchWithOneOffer()
            registerOffer()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(OFFER_ID) }
            tapOnPin(OFFER_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsOfferSnippet(OFFER_ID) }
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
            waitUntil { isHidden() }
        }
    }

    @Test
    fun shouldHideMultiHouseOfferWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerMapSearchWithMultiHouse()
            registerOffer()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(MULTI_HOUSE_ID) }
            tapOnPin(MULTI_HOUSE_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsOfferSnippet(OFFER_ID) }
            expand()
            waitUntil { isExpanded() }
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
            waitUntil { doesNotContainsOfferSnippet(OFFER_ID) }
        }
    }

    private fun DispatcherRegistry.registerMapSearchWithOneOffer() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("hideOfferTest/pointStatisticSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithMultiHouse() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("hideOfferTest/pointStatisticSearchMultiHouse.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
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
        private const val MULTI_HOUSE_ID = "3"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
