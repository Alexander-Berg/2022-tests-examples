package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.BuildingSeriesScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import org.junit.Test

/**
 * @author Filipp Besiadovskii on 09.09.2021.
 */
class BuildingSeriesTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenBuildingSeriesSet() {
        selectBuildingSeries(DealType.SELL, "1-510", 663_298L)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenBuildingSeriesSet() {
        selectBuildingSeries(DealType.RENT, "1-510", 663_298L)
    }

    private fun selectBuildingSeries(
        dealType: DealType,
        buildingSeriesName: String,
        buildingSeriesId: Long
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    tapOn(dealType.matcher.invoke(lookup))
                    propertyTypeSelector.click()
                    propertyTypePopupApartment.click()

                    listView.scrollTo(buildingSeriesField)
                        .click()
                }
                onScreen<BuildingSeriesScreen> {
                    searchView.typeText(buildingSeriesName)
                    buildingSeriesSuggest(buildingSeriesName)
                        .waitUntil { listView.contains(this) }
                        .click()
                }
                onScreen<FiltersScreen> {
                    waitUntil { buildingSeriesValue.isTextEquals(buildingSeriesName) }
                }
            },
            params = arrayOf(
                dealType.param,
                PropertyType.APARTMENT.param,
                "buildingSeriesId" to buildingSeriesId.toString()
            )
        )
    }
}
