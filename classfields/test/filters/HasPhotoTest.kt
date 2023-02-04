package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author scrooge on 26.06.2019.
 */
class HasPhotoTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.LOT
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenHasPhotoSet() {
        shouldChangeOffersCountWhenHasPhotoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    private fun shouldChangeOffersCountWhenHasPhotoSet(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldHasPhoto()).tapOn()

                isChecked(lookup.matchesHasPhotoValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "hasPhoto" to "YES"
            )
        )
    }
}
