package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.VillageOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.VillageOffersScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 16.10.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class VillageOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = VillageOffersTestRule(VILLAGE_ID)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "VILLAGE_CARD_LISTING",
            currentScreen = "VILLAGE_OFFERS"
        )
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = "листинг внутри КП",
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<VillageOffersScreen> {
            waitUntil { callButton.isCompletelyDisplayed() }

            callButton.click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    companion object {

        private const val VILLAGE_ID = "1"
    }
}
