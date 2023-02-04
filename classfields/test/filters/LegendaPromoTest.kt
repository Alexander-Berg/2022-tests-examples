package com.yandex.mobile.realty.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author scrooge on 22.03.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LegendaPromoTest {

    private var activityTestRule = FilterActivityTestRule()
    private val regionParams = RegionParams(
        0,
        0,
        "в Москве и МО",
        RegionParamsConfigImpl.DEFAULT.heatMapTypes,
        RegionParamsConfigImpl.DEFAULT.filters,
        RegionParamsConfigImpl.DEFAULT.schoolInfo,
        true,
        true,
        true,
        true,
        false,
        true,
        false,
        0,
        null
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(regionParams = regionParams),
        activityTestRule
    )

    @Test
    fun shouldShowLegendPromo() {
        onScreen<FiltersScreen> {
            promoListView.scrollTo(legendaPromoItem).click()
        }
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("https://10yrs.legenda-dom.ru/?only-content=true")
            }
        }
    }
}
