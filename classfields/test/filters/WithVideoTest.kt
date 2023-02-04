package com.yandex.mobile.realty.test.filters

import org.junit.Test

/**
 * @author solovevai on 2020-05-09.
 */
class WithVideoTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAllOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            PropertyType.APARTMENT,
            OfferCategory.ANY
        )
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            PropertyType.APARTMENT,
            OfferCategory.SECONDARY
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM
        )
    }

    @Test
    fun shouldChangeSellHouseAllOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            PropertyType.HOUSE,
            OfferCategory.ANY
        )
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            PropertyType.HOUSE,
            OfferCategory.SECONDARY
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE
        )
    }

    @Test
    fun shouldChangeSellLotAllOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            PropertyType.LOT,
            OfferCategory.ANY
        )
    }

    @Test
    fun shouldChangeSellLotSecondaryOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            PropertyType.LOT,
            OfferCategory.SECONDARY
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenWithVideoSet() {
        shouldChangeOffersCountWhenWithVideoSet(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL
        )
    }

    private fun shouldChangeOffersCountWhenWithVideoSet(
        dealType: DealType,
        propertyType: PropertyType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldWithVideo()).tapOn()

                isChecked(lookup.matchesWithVideoValue())
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "videoUrl" to "YES"
            )
        )
    }

    private fun shouldChangeOffersCountWhenWithVideoSet(
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
                scrollToPosition(lookup.matchesFieldWithVideo()).tapOn()

                isChecked(lookup.matchesWithVideoValue())
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                "videoUrl" to "YES",
                *offerCategory.params
            )
        )
    }
}
