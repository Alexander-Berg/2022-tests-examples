package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import com.yandex.mobile.realty.core.robot.performOnFurnitureDialog
import org.junit.Test

/**
 * @author scrooge on 09.07.2019.
 */
class FurnitureTest : FilterParamTest() {

    @Test
    fun shouldChangeRentApartmentOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenHasFurnitureNoSelected() {
        selectHasFurniture(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM,
            hasFurniture = HasFurniture.NO
        )
    }

    @Test
    fun shouldChangeSellCommercialOfficeOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.OFFICE,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeSellCommercialFreePurposeOffersCountWhenHasFurnitureNoSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.FREE_PURPOSE,
            hasFurniture = HasFurniture.NO
        )
    }

    @Test
    fun shouldChangeSellCommercialHotelOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.HOTEL,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeSellCommercialPublicCateringOffersCountWhenHasFurnitureNoSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.PUBLIC_CATERING,
            hasFurniture = HasFurniture.NO
        )
    }

    @Test
    fun shouldChangeSellCommercialRetailOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.RETAIL,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeRentCommercialOfficeOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.OFFICE,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeRentCommercialFreePurposeOffersCountWhenHasFurnitureNoSelected() {
        selectHasFurniture(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.FREE_PURPOSE,
            hasFurniture = HasFurniture.NO
        )
    }

    @Test
    fun shouldChangeRentCommercialHotelOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.HOTEL,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeRentCommercialPublicCateringOffersCountWhenHasFurnitureNoSelected() {
        selectHasFurniture(
            dealType = DealType.RENT,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.PUBLIC_CATERING,
            hasFurniture = HasFurniture.NO
        )
    }

    @Test
    fun shouldChangeRentCommercialRetailOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.COMMERCIAL,
            commercialType = CommercialType.RETAIL,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenHasFurnitureYesSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT,
            hasFurniture = HasFurniture.YES
        )
    }

    @Test
    fun shouldChangeSellApartmentOffersCountWhenHasFurnitureNoSelected() {
        selectHasFurniture(
            dealType = DealType.SELL,
            propertyType = PropertyType.APARTMENT,
            hasFurniture = HasFurniture.NO
        )
    }

    private fun selectHasFurniture(
        dealType: DealType,
        propertyType: PropertyType,
        commercialType: CommercialType? = null,
        hasFurniture: HasFurniture
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))

                commercialType?.let {
                    tapOn(lookup.matchesFieldCommercialType())
                    performOnCommercialTypeScreen {
                        scrollTo(commercialType.matcher.invoke(lookup))
                            .tapOn()
                        tapOn(lookup.matchesApplyButton())
                    }
                }

                scrollToPosition(lookup.matchesFieldFurniture())
                    .tapOn()
                performOnFurnitureDialog {
                    tapOn(hasFurniture.matcher.invoke(lookup))
                }
                isFurnitureEquals(hasFurniture.expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                commercialType?.param,
                hasFurniture.param
            )
        )
    }
}
