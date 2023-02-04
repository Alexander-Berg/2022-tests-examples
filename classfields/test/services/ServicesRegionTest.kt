package com.yandex.mobile.realty.test.services

import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BottomNavMenu
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.PriceDialogScreen
import com.yandex.mobile.realty.core.screen.RegionSuggestScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.GeoObject
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.geo.GeoType
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test

/**
 * @author merionkov on 21.04.2022.
 */
class ServicesRegionTest : BaseTest() {

    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    private val appStateRule = SetupDefaultAppStateRule(
        regionParams = RegionParams(
            regionId = DEFAULT_REGION_ID,
            geoId = 0,
            regionNameLocative = "в Москве и МО",
            heatMapTypes = RegionParamsConfigImpl.DEFAULT.heatMapTypes,
            filters = RegionParamsConfigImpl.DEFAULT.filters,
            schoolInfo = RegionParamsConfigImpl.DEFAULT.schoolInfo,
            hasVillages = true,
            hasMetro = true,
            hasDistricts = true,
            hasCommercialBuildings = true,
            hasConcierge = true,
            hasLegendaPromo = true,
            hasPaidSites = true,
            timestamp = 0,
            subjectFederationId = null,
        ),
        region = GeoRegion(
            id = 0,
            geo = GeoObject(
                type = GeoType.CITY,
                fullName = DEFAULT_REGION_NAME,
                shortName = DEFAULT_REGION_NAME,
                scope = DEFAULT_REGION_NAME,
                point = GeoPoint(55.75322, 37.62251),
                leftTop = GeoPoint(56.02137, 36.803265),
                rightBottom = GeoPoint(55.14263, 37.96769),
                colors = emptyList(),
                mapOf("rgid" to listOf("0"))
            ),
        ),
    )

    @JvmField
    @Rule
    val ruleChain = baseChainOf(
        activityTestRule,
        appStateRule,
    )

    @Test
    fun shouldUpdateYandexRentPromoOnRegionChanged() {
        configureWebServer {
            registerRegionList()
            registerUpdatedRegionInfo()
            registerRegionList()
            registerDefaultRegionInfo()
        }
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }.click()
        }
        onScreen<RegionSuggestScreen> {
            regionSuggest(UPDATED_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.doesNotContain(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(UPDATED_REGION_NAME) }.click()
        }
        onScreen<RegionSuggestScreen> {
            regionSuggest(DEFAULT_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }
        }
    }

    @Test
    fun shouldStartSearchFromYandexRentPromoOnRegionChanged() {
        configureWebServer {
            registerRegionList()
            registerUpdatedRegionInfoWithYandexRent()
            registerYandexRentSearch()
        }

        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }.click()
        }

        onScreen<RegionSuggestScreen> {
            regionSuggest(UPDATED_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<ServicesScreen> {
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(UPDATED_REGION_NAME) }
            rentPromoItem.waitUntil { listView.contains(this) }
            rentPromoRentButton.click()
        }

        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
            geoFieldView.click()
        }

        onScreen<GeoIntentScreen> {
            regionButton
                .waitUntil { isCompletelyDisplayed() }
                .waitUntil { isTextEquals("Ваш регион $UPDATED_REGION_NAME") }
        }
    }

    @Test
    fun shouldUpdateConciergeServiceOnRegionChanged() {
        configureWebServer {
            registerRegionList()
            registerUpdatedRegionInfo()
            registerRegionList()
            registerDefaultRegionInfo()
        }
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            conciergeServiceView.waitUntil { listView.contains(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }.click()
        }
        onScreen<RegionSuggestScreen> {
            regionSuggest(UPDATED_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<ServicesScreen> {
            conciergeServiceView.waitUntil { listView.doesNotContain(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(UPDATED_REGION_NAME) }.click()
        }
        onScreen<RegionSuggestScreen> {
            regionSuggest(DEFAULT_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<ServicesScreen> {
            conciergeServiceView.waitUntil { listView.contains(this) }
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }
        }
    }

    @Test
    fun shouldUpdateSearchFilterOnRegionChanged() {
        configureWebServer {
            registerRegionList()
            registerUpdatedRegionInfo()
        }
        activityTestRule.launchActivity()
        onScreen<BottomNavMenu> {
            searchItemView.click()
        }
        onScreen<SearchMapScreen> {
            geoFieldView.click()
        }
        onScreen<GeoIntentScreen> {
            regionButton
                .waitUntil { isCompletelyDisplayed() }
                .waitUntil { isTextEquals("Ваш регион $DEFAULT_REGION_NAME") }
            closeKeyboard()
            pressBack()
        }
        onScreen<SearchMapScreen> {
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            dealTypeSelector.click()
            dealTypePopupRent.click()
            propertyTypeSelector.click()
            propertyTypePopupRoom.click()
            rentTimeSelectorShort.click()
            priceValue.click()
        }
        onScreen<PriceDialogScreen> {
            valueFrom.replaceText("10000")
            valueTo.replaceText("50000")
            okButton.click()
        }
        onScreen<FiltersScreen> {
            submitButton.click()
        }
        onScreen<BottomNavMenu> {
            servicesItemView.click()
        }
        onScreen<ServicesScreen> {
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }.click()
        }
        onScreen<RegionSuggestScreen> {
            regionSuggest(UPDATED_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<BottomNavMenu> {
            searchItemView.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<SearchMapScreen> {
            geoFieldView.click()
        }
        onScreen<GeoIntentScreen> {
            regionButton
                .waitUntil { isCompletelyDisplayed() }
                .waitUntil { isTextEquals("Ваш регион $UPDATED_REGION_NAME") }
            closeKeyboard()
            pressBack()
        }
        onScreen<SearchMapScreen> {
            filterButton.click()
        }
        onScreen<FiltersScreen> {
            isRentSelected()
            isRoomSelected()
            rentTimeSelectorShort.isChecked()
            priceValue.isTextEquals("10 – 50 тыс. ₽")
        }
    }

    @Test
    fun shouldUpdateRegionOnSearchFilterChanged() {
        configureWebServer {
            registerRegionList()
            registerUpdatedRegionInfo()
        }
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(DEFAULT_REGION_NAME) }
        }
        onScreen<BottomNavMenu> {
            searchItemView.click()
        }
        onScreen<SearchMapScreen> {
            geoFieldView.click()
        }
        onScreen<GeoIntentScreen> {
            regionButton
                .waitUntil { isCompletelyDisplayed() }
                .waitUntil { isTextEquals("Ваш регион $DEFAULT_REGION_NAME") }
                .click()
        }
        onScreen<RegionSuggestScreen> {
            regionSuggest(UPDATED_REGION_NAME)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<GeoIntentScreen> {
            regionButton
                .waitUntil { isCompletelyDisplayed() }
                .waitUntil { isTextEquals("Ваш регион $UPDATED_REGION_NAME") }
            submitButton.click()
        }
        onScreen<BottomNavMenu> {
            servicesItemView.click()
        }
        onScreen<ServicesScreen> {
            regionItem.waitUntil { listView.contains(this) }
            regionButton.waitUntil { isTextEquals(UPDATED_REGION_NAME) }
        }
    }

    private fun DispatcherRegistry.registerRegionList() {
        register(
            request {
                path("1.0/regionList.json")
            },
            response {
                assetBody("ServicesRegionTest/regionList.json")
            }
        )
    }

    private fun DispatcherRegistry.registerDefaultRegionInfo() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", DEFAULT_REGION_ID.toString())
            },
            response {
                assetBody("ServicesRegionTest/regionInfoDefault.json")
            },
        )
    }

    private fun DispatcherRegistry.registerUpdatedRegionInfo() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", UPDATED_REGION_ID.toString())
            },
            response {
                assetBody("ServicesRegionTest/regionInfoUpdated.json")
            },
        )
    }

    private fun DispatcherRegistry.registerUpdatedRegionInfoWithYandexRent() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", UPDATED_REGION_ID.toString())
            },
            response {
                assetBody("ServicesRegionTest/regionInfoUpdatedWIthYandexRent.json")
            },
        )
    }

    private fun DispatcherRegistry.registerYandexRentSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("rentTime", "LARGE")
                queryParam("yandexRent", "YES")
                queryParam("rgid", UPDATED_REGION_ID.toString())
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchPage0.json")
            }
        )
    }

    private companion object {
        const val DEFAULT_REGION_ID = 0
        const val UPDATED_REGION_ID = 1
        const val DEFAULT_REGION_NAME = "Москва и МО"
        const val UPDATED_REGION_NAME = "Санкт-Петербург и ЛО"
    }
}
