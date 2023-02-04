package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnBuildingClassDialog
import org.junit.Test

/**
 * @author scrooge on 24.07.2019.
 */
class BuildingClassTest : FilterParamTest() {

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenBuildingClassEconomySelected() {
        selectBuildingClass(BuildingClass.ECONOM)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenBuildingClassComfortSelected() {
        selectBuildingClass(BuildingClass.COMFORT)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenBuildingClassComfortPlusSelected() {
        selectBuildingClass(BuildingClass.COMFORT_PLUS)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenBuildingClassBusinessSelected() {
        selectBuildingClass(BuildingClass.BUSINESS)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenBuildingClassEliteSelected() {
        selectBuildingClass(BuildingClass.ELITE)
    }

    private fun selectBuildingClass(buildingClass: BuildingClass) {
        val offerCategory = OfferCategory.PRIMARY(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldBuildingClass()).tapOn()

                performOnBuildingClassDialog {
                    tapOn(buildingClass.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isBuildingClassEquals(buildingClass.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                *offerCategory.params,
                buildingClass.param
            )
        )
    }
}
