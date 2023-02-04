package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class GarageTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellGarageOffersCountWhenBoxChecked() {
        selectGarageType(DealType.SELL, GarageType.BOX)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenGarageChecked() {
        selectGarageType(DealType.RENT, GarageType.GARAGE)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenParkingPlaceChecked() {
        selectGarageType(DealType.SELL, GarageType.PARKING_PLACE)
    }

    private fun selectGarageType(dealType: DealType, garageType: GarageType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.GARAGE.matcher.invoke(lookup))
                tapOn(garageType.matcher.invoke(lookup))
            },
            params = arrayOf(
                dealType.param,
                PropertyType.GARAGE.param,
                garageType.param
            )
        )
    }
}
