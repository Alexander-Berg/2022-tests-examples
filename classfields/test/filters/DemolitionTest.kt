package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnDemolitionDialog
import org.junit.Test

/**
 * @author scrooge on 08.07.2019.
 */
class DemolitionTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenDemolitionYesSet() {
        selectDemolition(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT,
            demolition = DemolitionType.YES
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenDemolitionYesSet() {
        selectDemolition(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM,
            demolition = DemolitionType.YES
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenDemolitionNoSet() {
        selectDemolition(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT,
            demolition = DemolitionType.NO
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenDemolitionNoSet() {
        selectDemolition(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM,
            demolition = DemolitionType.NO
        )
    }

    private fun selectDemolition(
        dealType: DealType,
        propertyType: PropertyType,
        demolition: DemolitionType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldDemolition())
                    .tapOn()
                performOnDemolitionDialog {
                    tapOn(demolition.matcher.invoke(lookup))
                }
                isDemolitionEquals(demolition.expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                demolition.param
            )
        )
    }
}
