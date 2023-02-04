package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.HouseMaterialDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import org.junit.Test

/**
 * @author Filipp Besiadovskii on 30.08.2021
 */
class HouseMaterialTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypeBrickSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.BRICK)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypeMonolitSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.MONOLIT)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypeMonolitBrickSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.MONOLIT_BRICK)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypePanelSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.PANEL)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypeWoodSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.WOOD)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypeBlockSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.BLOCK)
    }

    @Test
    fun shouldChangeSellHouseOffersCountWhenHouseMaterialTypeFerroConcreteSelected() {
        selectHouseMaterialType(DealType.SELL, HouseMaterialType.FERROCONCRETE)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypeBrickSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.BRICK)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypeMonolitSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.MONOLIT)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypeMonolitBrickSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.MONOLIT_BRICK)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypePanelSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.PANEL)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypeWoodSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.WOOD)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypeBlockSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.BLOCK)
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenHouseMaterialTypeFerroConcreteSelected() {
        selectHouseMaterialType(DealType.RENT, HouseMaterialType.FERROCONCRETE)
    }

    private fun selectHouseMaterialType(dealType: DealType, houseMaterialType: HouseMaterialType) {
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    if (dealType == DealType.RENT) {
                        dealTypePopupRent.click()
                    } else {
                        dealTypePopupBuy.click()
                    }
                    propertyTypeSelector.click()
                    propertyTypePopupHouse.click()

                    listView.scrollTo(houseMaterialTypeItem)
                        .click()

                    onScreen<HouseMaterialDialogScreen> {
                        listView.scrollTo(houseMaterialType.matcher.invoke()).click()
                        okButton.click()
                    }

                    houseMaterialTypeValue.isTextEquals(houseMaterialType.expected)
                }
            },
            params = arrayOf(
                dealType.param,
                PropertyType.HOUSE.param,
                houseMaterialType.param
            )
        )
    }
}
