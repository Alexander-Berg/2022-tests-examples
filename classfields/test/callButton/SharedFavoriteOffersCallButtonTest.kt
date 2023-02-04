package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.ModelType
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 02.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SharedFavoriteOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.OFFER,
        objectIds = listOf(OFFER_ID),
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
            eventPlace = "SHARED_FAVORITES",
            currentScreen = "SHARED_FAVORITES"
        )
        dispatcher.registerOffers()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на экране расшаренного избранного"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SharedFavoritesScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    callButton.click()
                }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("callButtonTest/offerWithSiteSearch.json")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
    }
}
