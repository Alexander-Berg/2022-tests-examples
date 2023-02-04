package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createSecondaryFlatProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 02.08.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageCardHideSearchOffersTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createSecondaryFlatProgram(),
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
            registerCalculatorConfig()
            registerCalculatorResult()
            registerOffersWithSiteSearch()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .menuButton
                .click()
            onScreen<OfferMenuDialogScreen> {
                hideButton.waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches("HideOfferTest/confirmationDialog")
                confirmButton.click()
            }
            waitUntil { listView.doesNotContain(offerSnippet(OFFER_ID)) }
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("hideOfferTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorConfig() {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/calculator")
            },
            response {
                assetBody("mortgage/calculatorConfig.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorResult() {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/calculator")
            },
            response {
                assetBody("mortgage/calculatorResultDefault.json")
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
        private const val PROGRAM_ID = "1"
        private const val OFFER_ID = "1"
    }
}
