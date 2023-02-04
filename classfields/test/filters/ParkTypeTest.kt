package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.ParkTypeDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TTextView
import org.junit.Test

/**
 * @author pvl-zolotov on 30.10.2021
 */
class ParkTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAnyOffersCountWhenParkTypeForest() {
        selectSellParkType(PropertyType.APARTMENT, OfferCategory.ANY, ParkType.FOREST)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenParkTypePark() {
        selectSellParkType(PropertyType.APARTMENT, OfferCategory.PRIMARY, ParkType.PARK)
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenParkTypeGarden() {
        selectSellParkType(PropertyType.APARTMENT, OfferCategory.SECONDARY, ParkType.GARDEN)
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenParkTypeForest() {
        selectSellParkType(PropertyType.HOUSE, OfferCategory.ANY, ParkType.FOREST)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenParkTypePark() {
        selectVillageParkType(PropertyType.HOUSE, ParkType.PARK)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenParkTypeGarden() {
        selectSellParkType(PropertyType.HOUSE, OfferCategory.SECONDARY, ParkType.GARDEN)
    }

    @Test
    fun shouldChangeSellLotAnyOffersCountWhenParkTypeForest() {
        selectSellParkType(PropertyType.LOT, OfferCategory.ANY, ParkType.FOREST)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenParkTypePark() {
        selectVillageParkType(PropertyType.LOT, ParkType.PARK)
    }

    @Test
    fun shouldChangeSellLotSecondaryOffersCountWhenParkTypeGarden() {
        selectSellParkType(PropertyType.LOT, OfferCategory.SECONDARY, ParkType.GARDEN)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenParkTypeForest() {
        selectParkType(DealType.SELL, PropertyType.ROOM, ParkType.FOREST)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenParkTypePark() {
        selectParkType(DealType.RENT, PropertyType.APARTMENT, ParkType.PARK)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenParkTypeGarden() {
        selectParkType(DealType.RENT, PropertyType.ROOM, ParkType.GARDEN)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenParkTypeForest() {
        selectParkType(DealType.RENT, PropertyType.HOUSE, ParkType.FOREST)
    }

    private fun selectSellParkType(
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory,
        parkType: ParkType
    ) {
        val offerCategory = offerCategoryFactory.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    dealTypePopupBuy.click()
                    propertyTypeSelector.click()
                    TTextView(propertyType.matcher.invoke(lookup)).click()
                    TTextView(offerCategory.matcher.invoke(lookup)).click()
                    listView.scrollTo(parkTypeField)
                        .click()

                    onScreen<ParkTypeDialogScreen> {
                        listView.scrollTo(parkType.matcher.invoke()).click()
                        okButton.click()
                    }

                    parkTypeValue.isTextEquals(parkType.expected)
                }
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                parkType.param,
                *offerCategory.params
            )
        )
    }

    private fun selectParkType(dealType: DealType, propertyType: PropertyType, parkType: ParkType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    TTextView(dealType.matcher.invoke(lookup)).click()
                    propertyTypeSelector.click()
                    TTextView(propertyType.matcher.invoke(lookup)).click()
                    listView.scrollTo(parkTypeField)
                        .click()

                    onScreen<ParkTypeDialogScreen> {
                        listView.scrollTo(parkType.matcher.invoke()).click()
                        okButton.click()
                    }

                    parkTypeValue.isTextEquals(parkType.expected)
                }
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                parkType.param
            )
        )
    }

    private fun selectVillageParkType(propertyType: PropertyType, parkType: ParkType) {
        val offerCategory = OfferCategory.PRIMARY.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    dealTypePopupBuy.click()
                    propertyTypeSelector.click()
                    TTextView(propertyType.matcher.invoke(lookup)).click()
                    houseCategorySelectorVillage.click()
                    listView.scrollTo(parkTypeField)
                        .click()

                    onScreen<ParkTypeDialogScreen> {
                        listView.scrollTo(parkType.matcher.invoke()).click()
                        okButton.click()
                    }

                    parkTypeValue.isTextEquals(parkType.expected)
                }
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                parkType.param,
                *offerCategory.params
            )
        )
    }
}
