package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.LastFloorDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import org.junit.Test

/**
 * @author Filipp Besiadovskii on 10.09.21
 */
class LastFloorTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentWhenOnlyLastFloorSelected() {
        changeLastFloor(DealType.SELL, OfferCategory.ANY, LastFloor.ONLY_LAST_FLOOR)
    }

    @Test
    fun shouldChangeSellApartmentWnenExceptLastFloorSelected() {
        changeLastFloor(DealType.SELL, OfferCategory.ANY, LastFloor.EXCEPT_LAST_FLOOR)
    }

    @Test
    fun shouldChangeRentApartmentWhenOnlyLastFloorSelected() {
        changeLastFloor(DealType.RENT, OfferCategory.ANY, LastFloor.ONLY_LAST_FLOOR)
    }

    @Test
    fun shouldChangeRentApartmentWnenExceptLastFloorSelected() {
        changeLastFloor(DealType.RENT, OfferCategory.ANY, LastFloor.EXCEPT_LAST_FLOOR)
    }

    @Test
    fun shouldChangeSiteApartmentWhenOnlyLastFloorSelected() {
        changeLastFloor(DealType.SELL, OfferCategory.PRIMARY, LastFloor.ONLY_LAST_FLOOR)
    }

    @Test
    fun shouldChangeSiteApartmentWhenExceptLastFloorSelected() {
        changeLastFloor(DealType.SELL, OfferCategory.PRIMARY, LastFloor.EXCEPT_LAST_FLOOR)
    }

    fun changeLastFloor(
        dealType: DealType,
        offerCategoryFactory: OfferCategoryFactory,
        lastFloor: LastFloor
    ) {

        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        var params: Array<Pair<String, String?>?> = arrayOf(
            dealType.param,
            PropertyType.APARTMENT.param,
            lastFloor.param
        )
        if (offerCategoryFactory == OfferCategory.PRIMARY) {
            params = params.plus(offerCategory.params)
        }
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    tapOn(dealType.matcher.invoke(lookup))
                    propertyTypeSelector.click()
                    propertyTypePopupApartment.click()
                    if (dealType == DealType.SELL) {
                        tapOn(offerCategory.matcher.invoke(lookup))
                    }

                    listView.scrollTo(lastFloorItem)
                        .click()

                    onScreen<LastFloorDialogScreen> {
                        listView.scrollTo(lastFloor.matcher).click()
                    }

                    lastFloorValue.isTextEquals(lastFloor.expected)
                }
            },
            params = params
        )
    }
}
