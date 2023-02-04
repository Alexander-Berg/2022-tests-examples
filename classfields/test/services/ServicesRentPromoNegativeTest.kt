package com.yandex.mobile.realty.test.services

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 25.08.2021
 */
@LargeTest
class ServicesRentPromoNegativeTest {

    private val activityTestRule = ServicesActivityTestRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            regionParams = RegionParams(
                0,
                0,
                "в Москве и МО",
                emptyMap(),
                emptyMap(),
                RegionParamsConfigImpl.DEFAULT.schoolInfo,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                null
            )
        ),
        activityTestRule
    )

    @Test
    fun shouldNotShowPromoWhenRegionParamsNotAllowIt() {
        onScreen<ServicesScreen> {
            servicesTitleItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("ServicesRentPromoNegativeTest/title")
            listView.doesNotContain(rentPromoItem)
        }
    }
}
