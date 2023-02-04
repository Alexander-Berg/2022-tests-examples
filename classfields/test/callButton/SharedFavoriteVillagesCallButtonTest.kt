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
class SharedFavoriteVillagesCallButtonTest : CallButtonTest() {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.VILLAGE,
        objectIds = listOf(VILLAGE_ID),
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
    fun shouldStartCallWhenVillageCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "SHARED_FAVORITES",
            currentScreen = "SHARED_FAVORITES"
        )
        dispatcher.registerVillages()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = villageSnippet("на экране расшаренного избранного"),
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SharedFavoritesScreen> {
            villageSnippet(VILLAGE_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    callButton.click()
                }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerVillages() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("villageId", VILLAGE_ID)
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {
        private const val VILLAGE_ID = "2"
    }
}
