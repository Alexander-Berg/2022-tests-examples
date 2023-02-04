package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnHouseTypeDialog
import org.junit.Test

/**
 * @author scrooge on 09.07.2019.
 */
class HouseTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellHoseOffersCountWhenHouseTypeTownhouseSelected() {
        selectHouseType(DealType.SELL, HouseType.TOWNHOUSE)
    }

    @Test
    fun shouldChangeSellHoseOffersCountWhenHouseTypeDuplexSelected() {
        selectHouseType(DealType.SELL, HouseType.DUPLEX)
    }

    @Test
    fun shouldChangeRentHoseOffersCountWhenHouseTypePartHouseSelected() {
        selectHouseType(DealType.RENT, HouseType.PART_HOUSE)
    }

    @Test
    fun shouldChangeRentHoseOffersCountWhenHouseTypeHouseSelected() {
        selectHouseType(DealType.RENT, HouseType.HOUSE)
    }

    private fun selectHouseType(
        dealType: DealType,
        houseType: HouseType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.HOUSE.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldHouseType())
                    .tapOn()
                performOnHouseTypeDialog {
                    tapOn(houseType.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }
                isHouseTypeEquals(houseType.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.HOUSE.param,
                houseType.param
            )
        )
    }
}
