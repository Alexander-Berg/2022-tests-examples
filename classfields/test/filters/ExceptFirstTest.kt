package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 28.06.2019.
 */
class ExceptFirstTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenExceptFirstSet() {
        shouldChangeOffersCountWhenExceptFirstSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenExceptFirstSet() {
        shouldChangeOffersCountWhenExceptFirstSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenExceptFirstSet() {
        shouldChangeOffersCountWhenExceptFirstSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenExceptFirstSet() {
        shouldChangeOffersCountWhenExceptFirstSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    private fun shouldChangeOffersCountWhenExceptFirstSet(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldExceptFirst()).tapOn()

                isChecked(lookup.matchesExceptFirstValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "floorExceptFirst" to "YES"
            )
        )
    }
}
