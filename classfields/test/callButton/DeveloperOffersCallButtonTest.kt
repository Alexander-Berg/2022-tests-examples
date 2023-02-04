package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.DeveloperOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.DeveloperOffersScreen
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
import org.junit.runner.RunWith

/**
 * @author merionkov on 07.04.2022.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = DeveloperOffersActivityTestRule(
        siteId = SITE_ID,
        siteName = SITE_NAME,
        launchActivity = false,
    )

    @JvmField
    @Rule
    val rule = baseChainOf(
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value),
    )

    @Test
    fun shouldStartCall() {
        configureWebServer {
            registerOfferStat()
            registerOffersSearch()
            registerSitePhone(SITE_ID)
        }
        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = "листинг внутри новостройки",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )
        activityTestRule.launchActivity()
        registerCallIntent()
        onScreen<DeveloperOffersScreen> {
            callButton.waitUntil { isCompletelyDisplayed() }.click()
            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
            },
            response {
                assetBody("DeveloperOffersCallButtonTest/offerStat.json")
            },
        )
    }

    private fun DispatcherRegistry.registerOffersSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("DeveloperOffersCallButtonTest/offersSearch.json")
            },
        )
    }

    private companion object {
        const val SITE_ID = "0"
        const val SITE_NAME = "stub"
    }
}
