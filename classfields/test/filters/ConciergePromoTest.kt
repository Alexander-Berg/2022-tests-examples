package com.yandex.mobile.realty.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConciergeProposalScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 6/21/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ConciergePromoTest {

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
        true,
        false,
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
    fun shouldShowConciergeFilterBanner() {
        onScreen<FiltersScreen> {
            val bannerKey = "ConciergePromoTest/shouldShowConciergeFilterBanner/promoState"
            listView.scrollTo(conciergeFilterBannerItem)
                .isViewStateMatches(bannerKey)

            listView.scrollToTop()
            apartmentCategorySelectorNew.click()

            listView.scrollTo(conciergeFilterBannerItem)
                .isViewStateMatches(bannerKey)

            listView.scrollToTop()
            apartmentCategorySelectorSecondary.click()

            listView.scrollTo(conciergeFilterBannerItem)
                .isViewStateMatches(bannerKey)

            listView.scrollToTop()
            propertyTypeSelector.click()
            propertyTypePopupRoom.click()

            listView.scrollTo(conciergeFilterBannerItem)
                .isViewStateMatches(bannerKey)
                .click()
        }
        onScreen<ConciergeProposalScreen> {
            phoneInputView.waitUntil { isCompletelyDisplayed() }
            isViewStateMatches("ConciergePromoTest/formState")
        }
    }

    @Test
    fun shouldNotShowConciergeFilterBannerRent() {
        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupRent.click()
            listView.doesNotContain(conciergeFilterBannerItem)

            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupHouse.click()
            listView.doesNotContain(conciergeFilterBannerItem)
        }
    }

    @Test
    fun shouldShowPresetsConciergePromo() {
        onScreen<FiltersScreen> {
            promoListView.scrollTo(conciergePromoItem)
                .also { view ->
                    view.isViewStateMatches(
                        "ConciergePromoTest/shouldShowPresetsConciergePromo/promoState"
                    )
                }
                .click()
        }
        onScreen<ConciergeProposalScreen> {
            phoneInputView.waitUntil { isCompletelyDisplayed() }
            isViewStateMatches("ConciergePromoTest/formState")
        }
    }
}
