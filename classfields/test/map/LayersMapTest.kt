package com.yandex.mobile.realty.test.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author scrooge on 26.03.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LayersMapTest {

    private var activityTestRule = MainActivityTestRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldOpenEducationLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerEducationItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerEducationTitleShown()
            isLayerEducationDescriptionShown()
        }
    }

    @Test
    fun shouldOpenPriceSellLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerPriceSellItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerPriceSellTitleShown()
            isLayerPriceSellDescriptionShown()
        }
    }

    @Test
    fun shouldOpenPriceRentLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerPriceRentItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerPriceRentTitleShown()
            isLayerPriceRentDescriptionShown()
        }
    }

    @Test
    fun shouldOpenTransportLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerTransportItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerTransportTitleShown()
            isLayerTransportDescriptionShown()
        }
    }

    @Test
    fun shouldOpenInfrastructureLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerInfrastructureItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerInfrastructureTitleShown()
            isLayerInfrastructureDescriptionShown()
        }
    }

    // temporarily turned off, see https://st.yandex-team.ru/VSAPPS-5066
/*    @Test
    fun shouldOpenEcologyLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerEcologyItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerEcologyTitleShown()
            isLayerEcologyDescriptionShown()
        }
    }*/

    @Test
    fun shouldOpenProfitabilityLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerProfitabilityItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerProfitabilityTitleShown()
            isLayerProfitabilityDescriptionShown()
        }
    }

    @Test
    fun shouldOpenCarsharingLayerInfo() {
        performOnSearchMapScreen {
            waitUntil { isLayerButtonShown() }

            tapOn(lookup.matchesLayerButton())
            tapOn(lookup.matchesLayerCarsharingItem())
            tapOn(lookup.matchesLayerInfoButton())
            isLayerCarsharingTitleShown()
            isLayerCarsharingDescriptionShown()
        }
    }
}
