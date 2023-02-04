package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.PhonesDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createSecondaryFlatProgram
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 30.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageCardSearchOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createSecondaryFlatProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            OFFER_ID,
            EVENT_PLACE,
            CURRENT_SCREEN
        )

        dispatcher.registerCalculatorConfig()
        dispatcher.registerCalculatorResult()
        dispatcher.registerOffersWithSiteSearch()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на карточке ипотечной программы"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()
        onScreen<MortgageProgramCardScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .callButton
                .click()
            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenFewPhoneNumbersExists() {
        val dispatcher = DispatcherRegistry()

        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            OFFER_ID,
            EVENT_PLACE,
            CURRENT_SCREEN
        )
        dispatcher.registerCalculatorConfig()
        dispatcher.registerCalculatorResult()
        dispatcher.registerOffersWithSiteSearch()
        dispatcher.registerFewOfferPhones(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на карточке ипотечной программы"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<MortgageProgramCardScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .callButton
                .click()
        }
        onScreen<PhonesDialogScreen> {
            phoneView(PHONE)
                .waitUntil { isCompletelyDisplayed() }
                .click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("callButtonTest/offerWithSiteSearch.json")
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

    companion object {

        private const val OFFER_ID = "0"
        private const val PROGRAM_ID = "1"
        private const val EVENT_PLACE = "MORTGAGE_CARD_SUITABLE_OFFERS"
        private const val CURRENT_SCREEN = "MORTGAGE_CARD"
    }
}
