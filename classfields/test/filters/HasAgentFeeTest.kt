package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 02.07.2019.
 */
class HasAgentFeeTest : FilterParamTest() {

    @Test
    fun shouldChangeRentApartmentOffersCountWhenHasAgentFeeSet() {
        shouldChangeOffersCountWhenHasAgentFeeSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenHasAgentFeeSet() {
        shouldChangeOffersCountWhenHasAgentFeeSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHasAgentFeeSet() {
        shouldChangeOffersCountWhenHasAgentFeeSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE
        )
    }

    private fun shouldChangeOffersCountWhenHasAgentFeeSet(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldHasAgentFee()).tapOn()

                isChecked(lookup.matchesHasAgentFeeValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "hasAgentFee" to "NO"
            )
        )
    }
}
