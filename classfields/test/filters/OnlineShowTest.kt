package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author solovevai on 2020-04-29.
 */
class OnlineShowTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAllOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            PropertyType.APARTMENT,
            OfferCategory.ANY
        )
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            PropertyType.APARTMENT,
            OfferCategory.SECONDARY
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeSellHouseAllOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            PropertyType.HOUSE,
            OfferCategory.ANY
        )
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            PropertyType.HOUSE,
            OfferCategory.SECONDARY
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeSellLotAllOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            PropertyType.LOT,
            OfferCategory.ANY
        )
    }

    @Test
    fun shouldChangeSellLotSecondaryOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            PropertyType.LOT,
            OfferCategory.SECONDARY
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenOnlineShowSet() {
        shouldChangeOffersCountWhenOnlineShowSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    private fun shouldChangeOffersCountWhenOnlineShowSet(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldOnlineShow()).tapOn()

                isChecked(lookup.matchesOnlineShowValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "onlineShow" to "YES"
            )
        )
    }

    private fun shouldChangeOffersCountWhenOnlineShowSet(
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory
    ) {
        val offerCategory = offerCategoryFactory.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldOnlineShow()).tapOn()

                isChecked(lookup.matchesOnlineShowValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                "onlineShow" to "YES",
                *offerCategory.params
            )
        )
    }
}
