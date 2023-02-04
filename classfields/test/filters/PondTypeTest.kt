package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.PondTypeDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TTextView
import org.junit.Test

/**
 * @author pvl-zolotov on 30.10.2021
 */
class PondTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAnyOffersCountWhenPondTypeRiver() {
        selectSellPondType(PropertyType.APARTMENT, OfferCategory.ANY, PondType.RIVER)
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenPondTypeSea() {
        selectSellPondType(PropertyType.APARTMENT, OfferCategory.PRIMARY, PondType.SEA)
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenPondTypeBay() {
        selectSellPondType(PropertyType.APARTMENT, OfferCategory.SECONDARY, PondType.BAY)
    }

    @Test
    fun shouldChangeSellHouseAnyOffersCountWhenPondTypeStrait() {
        selectSellPondType(PropertyType.HOUSE, OfferCategory.ANY, PondType.STRAIT)
    }

    @Test
    fun shouldChangeSellHouseVillageOffersCountWhenPondTypeLake() {
        selectVillagePondType(PropertyType.HOUSE, PondType.LAKE)
    }

    @Test
    fun shouldChangeSellHouseSecondaryOffersCountWhenPondTypePond() {
        selectSellPondType(PropertyType.HOUSE, OfferCategory.SECONDARY, PondType.POND)
    }

    @Test
    fun shouldChangeSellLotAnyOffersCountWhenPondTypeRiver() {
        selectSellPondType(PropertyType.LOT, OfferCategory.ANY, PondType.RIVER)
    }

    @Test
    fun shouldChangeSellLotVillageOffersCountWhenPondTypeSea() {
        selectVillagePondType(PropertyType.LOT, PondType.SEA)
    }

    @Test
    fun shouldChangeSellLotSecondaryOffersCountWhenPondTypeBay() {
        selectSellPondType(PropertyType.LOT, OfferCategory.SECONDARY, PondType.BAY)
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenPondTypeStrait() {
        selectPondType(DealType.SELL, PropertyType.ROOM, PondType.STRAIT)
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenPondTypeLake() {
        selectPondType(DealType.RENT, PropertyType.APARTMENT, PondType.LAKE)
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenPondTypePond() {
        selectPondType(DealType.RENT, PropertyType.ROOM, PondType.POND)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenPondTypeRiver() {
        selectPondType(DealType.RENT, PropertyType.HOUSE, PondType.RIVER)
    }

    private fun selectSellPondType(
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory,
        pondType: PondType
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
                    listView.scrollTo(pondTypeField)
                        .click()

                    onScreen<PondTypeDialogScreen> {
                        listView.scrollTo(pondType.matcher.invoke()).click()
                        okButton.click()
                    }

                    pondTypeValue.isTextEquals(pondType.expected)
                }
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                pondType.param,
                *offerCategory.params
            )
        )
    }

    private fun selectPondType(dealType: DealType, propertyType: PropertyType, pondType: PondType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    TTextView(dealType.matcher.invoke(lookup)).click()
                    propertyTypeSelector.click()
                    TTextView(propertyType.matcher.invoke(lookup)).click()
                    listView.scrollTo(pondTypeField)
                        .click()

                    onScreen<PondTypeDialogScreen> {
                        listView.scrollTo(pondType.matcher.invoke()).click()
                        okButton.click()
                    }

                    pondTypeValue.isTextEquals(pondType.expected)
                }
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                pondType.param
            )
        )
    }

    private fun selectVillagePondType(propertyType: PropertyType, pondType: PondType) {
        val offerCategory = OfferCategory.PRIMARY.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    dealTypePopupBuy.click()
                    propertyTypeSelector.click()
                    TTextView(propertyType.matcher.invoke(lookup)).click()
                    houseCategorySelectorVillage.click()

                    listView.scrollTo(pondTypeField)
                        .click()

                    onScreen<PondTypeDialogScreen> {
                        listView.scrollTo(pondType.matcher.invoke()).click()
                        okButton.click()
                    }

                    pondTypeValue.isTextEquals(pondType.expected)
                }
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                pondType.param,
                *offerCategory.params
            )
        )
    }
}
