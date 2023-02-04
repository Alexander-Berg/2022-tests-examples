package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.SiteResellerOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnPhonesDialog
import com.yandex.mobile.realty.core.robot.performOnSiteResellerOffersScreen
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * Created by Alena Malchikhina on 21.05.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteResellerOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = SiteResellerOffersTestRule(
        siteId = "1",
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "LISTING",
            currentScreen = "SITE_RESELLER_OFFERS_LIST"
        )
        dispatcher.registerOffersWithSiteSearch()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в листинге внутри новостройки"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteResellerOffersScreen {
            waitUntil { containsOffer(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }

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
            eventPlace = "LISTING",
            currentScreen = "SITE_RESELLER_OFFERS_LIST"
        )
        dispatcher.registerOffersWithSiteSearch()
        dispatcher.registerFewOfferPhones(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в листинге внутри новостройки"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSiteResellerOffersScreen {
            waitUntil { containsOffer(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }
        }
        performOnPhonesDialog {
            waitUntil { isPhoneShown(PHONE) }
            tapOn(lookup.matchesPhone(PHONE))

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
                assetBody("offerWithSiteSearchDefaultSorting.json")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "1"
    }
}
