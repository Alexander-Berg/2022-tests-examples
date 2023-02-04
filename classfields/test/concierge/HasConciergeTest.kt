package com.yandex.mobile.realty.test.concierge

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 10.03.2021
 */
@RunWith(AndroidJUnit4::class)
class HasConciergeTest {
    val activityTestRule = FilterActivityTestRule(launchActivity = false)
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
        false,
        false,
        0,
        null
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(regionParams = regionParams, listMode = true),
        activityTestRule,
    )

    @Test
    fun shouldNotShowConciergeSnippetIfHasConciergeFalse() {
        configureWebServer {
            registerOfferWithSiteSearch()
        }

        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorAny.click()

            submitButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil {
                listView.contains(offerSnippet("1"))
            }
            listView.doesNotContain(conciergeSnippetItem)
        }
    }

    @Test
    fun shouldNotShowConciergeFilterBannerIfHasConciergeFalse() {
        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorAny.click()

            listView.doesNotContain(conciergeFilterBannerItem)
        }
    }

    @Test
    fun shouldNotShowPresetsConciergePromoIfHasConciergeFalse() {
        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupBuy.click()
            propertyTypeSelector.click()
            propertyTypePopupApartment.click()
            apartmentCategorySelectorAny.click()

            promoListView.doesNotContain(conciergePromoItem)
        }
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")

                queryParam("page", "0")
                queryParam("category", "APARTMENT")
                queryParam("type", "SELL")
                queryParam("objectType", "OFFER")
            },
            response {
                assetBody("conciergeTest/offerWithSiteSearch.json")
            }
        )
    }
}
