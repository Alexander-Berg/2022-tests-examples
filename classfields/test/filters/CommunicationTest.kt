package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommunicationDialog
import org.junit.Test

/**
 * @author scrooge on 10.07.2019.
 */
class CommunicationTest : FilterParamTest() {

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenElectricitySelected() {
        selectSellHouseCommunication(OfferCategory.ANY, HouseCommunication.ELECTRICITY)
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenGasSelected() {
        selectSellHouseCommunication(OfferCategory.ANY, HouseCommunication.GAS)
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenWaterSelected() {
        selectSellHouseCommunication(OfferCategory.ANY, HouseCommunication.WATER)
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenSewerageSelected() {
        selectSellHouseCommunication(OfferCategory.ANY, HouseCommunication.SEWERAGE)
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenHeatingSelected() {
        selectSellHouseCommunication(OfferCategory.ANY, HouseCommunication.HEATING)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenElectricitySelected() {
        selectSellHouseCommunication(OfferCategory.SECONDARY, HouseCommunication.ELECTRICITY)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenGasSelected() {
        selectSellHouseCommunication(OfferCategory.SECONDARY, HouseCommunication.GAS)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenWaterSelected() {
        selectSellHouseCommunication(OfferCategory.SECONDARY, HouseCommunication.WATER)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenSewerageSelected() {
        selectSellHouseCommunication(OfferCategory.SECONDARY, HouseCommunication.SEWERAGE)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenHeatingSelected() {
        selectSellHouseCommunication(OfferCategory.SECONDARY, HouseCommunication.HEATING)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenWaterSelected() {
        selectGarageCommunication(DealType.SELL, GarageCommunication.WATER)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenHeatingSelected() {
        selectGarageCommunication(DealType.SELL, GarageCommunication.HEATING)
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenElectricitySelected() {
        selectGarageCommunication(DealType.SELL, GarageCommunication.ELECTRICITY)
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenWaterSelected() {
        selectGarageCommunication(DealType.RENT, GarageCommunication.WATER)
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenHeatingSelected() {
        selectGarageCommunication(DealType.RENT, GarageCommunication.HEATING)
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenElectricitySelected() {
        selectGarageCommunication(DealType.RENT, GarageCommunication.ELECTRICITY)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenElectricitySelected() {
        selectSellHouseCommunication(OfferCategory.PRIMARY, HouseCommunication.ELECTRICITY)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenGasSelected() {
        selectSellHouseCommunication(OfferCategory.PRIMARY, HouseCommunication.GAS)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenWaterSelected() {
        selectSellHouseCommunication(OfferCategory.PRIMARY, HouseCommunication.WATER)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenSewerageSelected() {
        selectSellHouseCommunication(OfferCategory.PRIMARY, HouseCommunication.SEWERAGE)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenHeatingSelected() {
        selectSellHouseCommunication(OfferCategory.PRIMARY, HouseCommunication.HEATING)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenElectricitySelected() {
        selectSellLotCommunication(HouseCommunication.ELECTRICITY)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenGasSelected() {
        selectSellLotCommunication(HouseCommunication.GAS)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenWaterSelected() {
        selectSellLotCommunication(HouseCommunication.WATER)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenSewerageSelected() {
        selectSellLotCommunication(HouseCommunication.SEWERAGE)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenHeatingSelected() {
        selectSellLotCommunication(HouseCommunication.HEATING)
    }

    private fun selectSellHouseCommunication(
        offerCategoryFactory: OfferCategoryFactory,
        communication: HouseCommunication
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.HOUSE)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.HOUSE.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFiledCommunication()).tapOn()
                performOnCommunicationDialog {
                    tapOn(communication.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }
                isCommunicationEquals(communication.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.HOUSE.param,
                communication.param,
                *offerCategory.params
            )
        )
    }

    private fun selectSellLotCommunication(communication: HouseCommunication) {
        val offerCategory = OfferCategory.PRIMARY.invoke(PropertyType.LOT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.LOT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFiledCommunication()).tapOn()
                performOnCommunicationDialog {
                    tapOn(communication.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }
                isCommunicationEquals(communication.expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.LOT.param,
                communication.param,
                *offerCategory.params
            )
        )
    }

    private fun selectGarageCommunication(dealType: DealType, communication: GarageCommunication) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.GARAGE.matcher.invoke(lookup))
                scrollToPosition(communication.fieldMatcher.invoke(lookup))
                    .tapOn()

                isChecked(communication.valueMatcher.invoke(lookup))
            },
            params = arrayOf(
                dealType.param,
                PropertyType.GARAGE.param,
                communication.param
            )
        )
    }
}
