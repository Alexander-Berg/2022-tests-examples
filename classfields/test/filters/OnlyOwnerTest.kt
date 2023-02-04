package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 26.06.2019.
 */
class OnlyOwnerTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.LOT
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenOnlyOwnerSet() {
        shouldChangeOffersCountWhenOnlyOwnerSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE
        )
    }

    private fun shouldChangeOffersCountWhenOnlyOwnerSet(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldOnlyOwner()).tapOn()

                isChecked(lookup.matchesOnlyOwnerValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "agents" to "NO"
            )
        )
    }
}
