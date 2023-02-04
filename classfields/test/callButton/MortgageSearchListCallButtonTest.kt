package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.PhonesDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 3/23/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageSearchListCallButtonTest : CallButtonTest() {

    private val activityTestRule = MortgageProgramListActivityTestRule(
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
    fun shouldStartCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "MORTGAGE_LISTING_SUITABLE_OFFERS",
            currentScreen = "MORTGAGE_LISTING"
        )
        dispatcher.registerOffersWithSiteSearch()
        dispatcher.registerOfferPhone(OFFER_ID)
        dispatcher.registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
        dispatcher.registerMortgageProgramSearch(
            "mortgageProgramSearchDefault.json",
            rgid = RGID
        )
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в блоке подходящих квартир ипотечных программ"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<MortgageProgramListScreen> {
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
            offerId = OFFER_ID,
            eventPlace = "MORTGAGE_LISTING_SUITABLE_OFFERS",
            currentScreen = "MORTGAGE_LISTING"
        )
        dispatcher.registerOffersWithSiteSearch()
        dispatcher.registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
        dispatcher.registerMortgageProgramSearch(
            "mortgageProgramSearchDefault.json",
            rgid = RGID
        )
        dispatcher.registerFewOfferPhones(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в блоке подходящих квартир ипотечных программ"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<MortgageProgramListScreen> {
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

    @Test
    fun shouldStartCallWhenSiteCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "MORTGAGE_LISTING_SUITABLE_OFFERS",
            currentScreen = "MORTGAGE_LISTING"
        )
        dispatcher.registerSites()
        dispatcher.registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
        dispatcher.registerMortgageProgramSearch(
            "mortgageProgramSearchDefault.json",
            rgid = RGID
        )
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = siteSnippet("в блоке подходящих квартир ипотечных программ"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<MortgageProgramListScreen> {
            siteSnippet(SITE_ID)
                .waitUntil { listView.contains(this) }
                .callButton
                .click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerSites() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
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

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        page: Int = 0,
        rgid: String?,
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                rgid?.let { queryParam("rgid", it) }
                queryParam("page", page.toString())
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val RGID = "587795"
    }
}
