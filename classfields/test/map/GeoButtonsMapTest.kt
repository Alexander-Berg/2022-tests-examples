package com.yandex.mobile.realty.test.map

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnDrawPolygonsScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrikeev on 27/02/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class GeoButtonsMapTest {

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MainActivityTestRule(),
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)
    )

    @Test
    fun shouldOpenDrawPolygonsScreen() {
        performOnSearchMapScreen {
            waitUntil { isPolygonsButtonShown() }

            tapOn(lookup.matchesPolygonsButton())
        }

        performOnDrawPolygonsScreen {
            waitUntil { isMapViewShown() }
        }
    }

    @Test
    fun shouldOpenCommuteAddressSelectScreen() {
        performOnSearchMapScreen {
            waitUntil { isCommuteButtonShown() }

            tapOn(lookup.matchesCommuteButton())
        }

        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
        }
    }
}
