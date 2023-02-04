package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnTimeToMetroDialog
import org.junit.Test

/**
 * @author rogovalex on 09/06/2019.
 */
class TimeToMetroTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentOffersCountWhenTimeToMetroOnFootMinutes5Selected() {
        selectSellApartmentTimeToMetro(
            OfferCategory.ANY,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_5
        )
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenTimeToMetroOnTransportMinutes10Selected() {
        selectSellApartmentTimeToMetro(
            OfferCategory.ANY,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_10
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenTimeToMetroOnFootMinutes15Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.APARTMENT,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_15
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenTimeToMetroOnTransportMinutes20Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.APARTMENT,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_20
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenTimeToMetroOnFootMinutes25Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.ROOM,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_25
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenTimeToMetroOnTransportMinutes30Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.ROOM,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_30
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenTimeToMetroOnFootMinutes45Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.ROOM,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_45
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenTimeToMetroOnTransportMinutes60Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.ROOM,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_60
        )
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenTimeToMetroOnFootMinutes10Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.HOUSE,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_10
        )
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenTimeToMetroOnTransportMinutes5Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.HOUSE,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_5
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenTimeToMetroOnFootMinutes15Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.HOUSE,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_20
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenTimeToMetroOnTransportMinutes10Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.HOUSE,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_15
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenTimeToMetroOnFootMinutes30Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.LOT,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_30
        )
    }

    @Test
    fun shouldChangeSellLotOffersCountWhenTimeToMetroOnTransportMinutes25Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.LOT,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_25
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenTimeToMetroOnFootMinutes60Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.COMMERCIAL,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_60
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenTimeToMetroOnTransportMinutes45Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.COMMERCIAL,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_45
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenTimeToMetroOnFootMinutes5Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.COMMERCIAL,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_5
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenTimeToMetroOnTransportMinutes10Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.COMMERCIAL,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_10
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenTimeToMetroOnFootMinutes15Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.GARAGE,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_15
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenTimeToMetroOnTransportMinutes20Selected() {
        selectTimeToMetro(
            DealType.SELL,
            PropertyType.GARAGE,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_20
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenTimeToMetroOnFootMinutes25Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.GARAGE,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_25
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenTimeToMetroOnTransportMinutes30Selected() {
        selectTimeToMetro(
            DealType.RENT,
            PropertyType.GARAGE,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_30
        )
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenTimeToMetroOnFootMinutes5Selected() {
        selectSellApartmentTimeToMetro(
            OfferCategory.PRIMARY,
            TimeToMetroTransport.ON_FOOT,
            TimeToMetro.MINUTES_45
        )
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenTimeToMetroOnTransportMinutes10Selected() {
        selectSellApartmentTimeToMetro(
            OfferCategory.PRIMARY,
            TimeToMetroTransport.ON_TRANSPORT,
            TimeToMetro.MINUTES_60
        )
    }

    private fun selectSellApartmentTimeToMetro(
        offerCategoryFactory: OfferCategoryFactory,
        metroTransport: TimeToMetroTransport,
        timeToMetro: TimeToMetro
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldTimeToMetro()).tapOn()
                performOnTimeToMetroDialog {
                    tapOn(metroTransport.matcher.invoke(lookup))
                    scrollToPosition(timeToMetro.matcher.invoke(lookup)).tapOn()
                    tapOn(lookup.matchesPositiveButton())
                }
                timeToMetroEquals(timeToMetro.expected + metroTransport.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                metroTransport.param,
                timeToMetro.param,
                *offerCategory.params
            )
        )
    }

    private fun selectTimeToMetro(
        dealType: DealType,
        propertyType: PropertyType,
        metroTransport: TimeToMetroTransport,
        timeToMetro: TimeToMetro
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldTimeToMetro()).tapOn()
                performOnTimeToMetroDialog {
                    tapOn(metroTransport.matcher.invoke(lookup))
                    scrollToPosition(timeToMetro.matcher.invoke(lookup)).tapOn()
                    tapOn(lookup.matchesPositiveButton())
                }
                timeToMetroEquals(timeToMetro.expected + metroTransport.expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                metroTransport.param,
                timeToMetro.param
            )
        )
    }
}
