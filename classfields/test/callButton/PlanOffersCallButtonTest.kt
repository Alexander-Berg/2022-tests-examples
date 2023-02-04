package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.PlanOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnPlanOffersScreen
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 16.10.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlanOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = PlanOffersActivityTestRule(
        siteId = SITE_ID,
        planId = PLAN_ID,
        isPaid = true,
        launchActivity = false
    )

    @JvmField
    @Rule
    val rule: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenFloatingCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        dispatcher.registerSitePhone(SITE_ID)
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_OFFER_LISTING_PLAN",
            currentScreen = "NEWBUILDING_OFFER_LISTING_PLAN"
        )
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = "экран планировки",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell", "ZhkNewbuilding_Sell_Paid")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnPlanOffersScreen {
            waitUntil { isFloatingCallButtonShown() }

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    companion object {
        private const val SITE_ID = "2"
        private const val PLAN_ID = "6"
    }
}
