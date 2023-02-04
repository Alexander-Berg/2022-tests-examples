package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnAgencySuggestScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Test

/**
 * @author andrey-bgm on 27/11/2020.
 */
class AgencyTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAllOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.APARTMENT, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.APARTMENT, OfferCategory.SECONDARY)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenAgencySet() {
        selectAgency(DealType.RENT, PropertyType.APARTMENT)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenAgencySet() {
        selectAgency(DealType.RENT, PropertyType.ROOM)
    }

    @Test
    fun shouldChangeSellHouseAllOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.HOUSE, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.HOUSE, OfferCategory.SECONDARY)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenAgencySet() {
        selectAgency(DealType.RENT, PropertyType.HOUSE)
    }

    @Test
    fun shouldChangeSellLotAllOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.LOT, OfferCategory.ANY)
    }

    @Test
    fun shouldChangeSellLotSecondaryOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.LOT, OfferCategory.SECONDARY)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.GARAGE)
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenAgencySet() {
        selectAgency(DealType.RENT, PropertyType.GARAGE)
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenAgencySet() {
        selectAgency(DealType.SELL, PropertyType.COMMERCIAL)
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenAgencySet() {
        selectAgency(DealType.RENT, PropertyType.COMMERCIAL)
    }

    private fun selectAgency(
        dealType: DealType,
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory? = null
    ) {
        val offerCategory = offerCategoryFactory?.invoke(propertyType)
        val offerCategoryParams = offerCategory?.params ?: emptyArray()
        shouldChangeOffersCount(
            { registerAgencySuggest() },
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                if (offerCategory != null) {
                    tapOn(offerCategory.matcher.invoke(lookup))
                }

                scrollToPosition(lookup.matchesFieldAgency()).tapOn()

                performOnAgencySuggestScreen {
                    typeSearchText("sk")
                    waitUntil { containsSuggest("Sky Estate") }
                    tapOn(lookup.matchesAgencySuggest("Sky Estate"))
                }

                hasAgency("Sky Estate")
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "uid" to "4045447503",
                *offerCategoryParams
            )
        )
    }

    private fun DispatcherRegistry.registerAgencySuggest() {
        register(
            request {
                path("2.0/agencies/active")
                queryParam("page", "0")
                queryParam("pageSize", "10")
                queryParam("userType", "AGENCY")
                queryParam("userType", "AGENT")
                queryParam("text", "sk")
            },
            response {
                assetBody("agencyTest/agencySuggest.json")
            }
        )
    }
}
