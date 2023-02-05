package com.edadeal.android.model.calibrator

import com.edadeal.android.model.calibrator.features.FeatureReaderManager
import com.edadeal.android.util.setupMoshi
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureReaderTest {
    private val featureReaderManager = FeatureReaderManager.create(Moshi.Builder().setupMoshi().build())

    @Test
    fun `feature reader should read correct values`() {
        val json = """{
"config":{
 "features": [
  { "name": "scannerBarcodeAccumulate", "value": [8] },
  { "name": "storiesEnabled", "value": [ true ] },
  { "name": "unknown", "value": [0, "1", 2.0] },
  { "value": [true], "name": "wsBridgeEnabled" },
  { "value": [ "@edadeal/lompakko", "@edadeal/indashop"], "name": "wsBridgeWebAppEnabled" },
  { "value": [ "cart", "@edadeal/cb", "cart", "@edadeal/coupons"], "name": "tabsWithNativeBadges" },
  { "value": [ "@edadeal/cb", "@edadeal/search", "@edadeal/coupons"], "name": "webappsWithMargin" },
  { "name": "reloginConfig", "value": ["{\"reloginEnabled\": false,\"reloginSessionsCount\": 2,\"reloginNumberOfDays\": 6,\"reloginDismissEnabled\": false}"] }
 ]
}
}""".trimIndent()

        val features = Buffer().writeUtf8(json).use { featureReaderManager.readFeatures(it) }

        assertTrue(features.storiesEnabled)
        assertTrue(features.wsBridgeEnabled)
        assertEquals(8, features.scannerAccumulatorCapacity)
        assertEquals(setOf("@edadeal/lompakko", "@edadeal/indashop"), features.wsBridgeWebAppEnabled)
        assertEquals(setOf("cart", "@edadeal/cb", "@edadeal/coupons"), features.tabsWithNativeBadges)
        assertEquals(setOf("@edadeal/cb", "@edadeal/search", "@edadeal/coupons"), features.webAppsWithMargins)
        assertEquals(features.reloginConfig.reloginEnabled, false)
        assertEquals(features.reloginConfig.reloginSessionsCount, 2)
        assertEquals(features.reloginConfig.reloginNumberOfDays, 6)
        assertEquals(features.reloginConfig.reloginDismissEnabled, false)
    }
}
