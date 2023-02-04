package com.yandex.mobile.realty.test.map

import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
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
 * @author sorokinandrei on 4/13/22.
 */
class GeoJsonLayerTest : BaseTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldShowMapPoints() {
        configureWebServer {
            registerPointSearchDefault()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            waitUntil {
                mapView.containsGeoJsonPoint(
                    LAYER_ID,
                    DEFAULT_POINT_LAT,
                    DEFAULT_POINT_LON
                )
                mapView.containsGeoJsonPoint(
                    LAYER_ID,
                    NEWFLAT_POINT_LAT,
                    NEWFLAT_POINT_LON
                )
            }
        }
    }

    private fun DispatcherRegistry.registerPointSearchDefault() {
        register(
            request {
                path("1.0/point/simplePointSearch")
                queryParam("x", "308")
                queryParam("y", "160")
            },
            response {
                assetBody("GeoJsonLayerTest/pointSearchDefault.json")
            }
        )
    }

    private companion object {

        const val LAYER_ID = "search_layer"
        const val DEFAULT_POINT_LAT = 55.585
        const val DEFAULT_POINT_LON = 37.385
        const val NEWFLAT_POINT_LAT = 55.835
        const val NEWFLAT_POINT_LON = 37.385
    }
}
