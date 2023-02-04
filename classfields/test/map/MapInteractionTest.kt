package com.yandex.mobile.realty.test.map

import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.view.TMapView
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author merionkov on 08.11.2021.
 */
class MapInteractionTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldUpdateMapAfterMove() {
        testInteraction("mapSearchMoved") { moveTo(10.0, 10.0) }
    }

    @Test
    fun shouldUpdateMapAfterZoomIn() {
        testInteraction("mapSearchZoomedIn") { zoomTo(10f) }
    }

    @Test
    fun shouldUpdateMapAfterZoomOut() {
        testInteraction("mapSearchZoomedOut") { zoomTo(8f) }
    }

    private fun testInteraction(
        updatedSearchResponseFileName: String,
        interaction: TMapView.() -> Unit,
    ) {
        configureWebServer {
            registerMapSearchResult("mapSearchDefault")
            registerMapSearchResult(updatedSearchResponseFileName)
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            with(mapView) {
                waitUntil { showsDefaultSearch() }
                waitUntil { offersCountEquals(5, 5) }
                interaction.invoke(this)
                waitUntil { showsUpdatedSearch() }
                waitUntil { offersCountEquals(4, 4) }
            }
        }
    }

    private fun TMapView.showsDefaultSearch() {
        for (i in 1..5) containsPlacemark(i.toString())
    }

    private fun TMapView.showsUpdatedSearch() {
        for (i in 6..9) containsPlacemark(i.toString())
    }

    private fun DispatcherRegistry.registerMapSearchResult(responseFileName: String) {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("MapInteractionTest/$responseFileName.json")
            },
        )
    }
}
