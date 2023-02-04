package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialBuildingTypeDialog
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author scrooge on 31.07.2019.
 */
class CommercialBuildingTypeTest : FilterParamTest() {

    @Test
    fun shouldChangeSellOffersCountWhenAllTypesSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.OFFICE,
            *CommercialBuildingType.values()
        )
    }

    @Test
    fun shouldChangeRentOffersCountWhenAllTypesSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.OFFICE,
            *CommercialBuildingType.values()
        )
    }

    @Test
    fun shouldChangeRentOffersCountWhenBusinessCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.OFFICE,
            CommercialBuildingType.BUSINESS_CENTER
        )
    }

    @Test
    fun shouldChangeSellOffersCountWhenWarehouseSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.OFFICE,
            CommercialBuildingType.WAREHOUSE
        )
    }

    @Test
    fun shouldChangeRentRetailCountWhenShoppingCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.RETAIL,
            CommercialBuildingType.SHOPPING_CENTER
        )
    }

    @Test
    fun shouldChangeSellRetailCountWhenDetachedBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.RETAIL,
            CommercialBuildingType.DETACHED_BUILDING
        )
    }

    @Test
    fun shouldChangeRentFreePurposeCountWhenResidentialBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.FREE_PURPOSE,
            CommercialBuildingType.RESIDENTIAL_BUILDING
        )
    }

    @Test
    fun shouldChangeSellFreePurposeCountWhenBusinessCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.FREE_PURPOSE,
            CommercialBuildingType.BUSINESS_CENTER
        )
    }

    @Test
    fun shouldChangeRentPublicCateringCountWhenWarehouseSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.PUBLIC_CATERING,
            CommercialBuildingType.WAREHOUSE
        )
    }

    @Test
    fun shouldChangeSellPublicCateringCountWhenShoppingCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.PUBLIC_CATERING,
            CommercialBuildingType.SHOPPING_CENTER
        )
    }

    @Test
    fun shouldChangeRentHotelCountWhenDetachedBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.HOTEL,
            CommercialBuildingType.DETACHED_BUILDING
        )
    }

    @Test
    fun shouldChangeSellHotelCountWhenResidentialBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.HOTEL,
            CommercialBuildingType.RESIDENTIAL_BUILDING
        )
    }

    @Test
    fun shouldChangeRentAutoRepairCountWhenBusinessCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.AUTO_REPAIR,
            CommercialBuildingType.BUSINESS_CENTER
        )
    }

    @Test
    fun shouldChangeSellAutoRepairCountWhenWarehouseSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.AUTO_REPAIR,
            CommercialBuildingType.WAREHOUSE
        )
    }

    @Test
    fun shouldChangeRentWarehouseCountWhenShoppingCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.WAREHOUSE,
            CommercialBuildingType.SHOPPING_CENTER
        )
    }

    @Test
    fun shouldChangeSellWarehouseCountWhenDetachedBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.WAREHOUSE,
            CommercialBuildingType.DETACHED_BUILDING
        )
    }

    @Test
    fun shouldChangeRentManufacturingCountWhenResidentialBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.MANUFACTURING,
            CommercialBuildingType.RESIDENTIAL_BUILDING
        )
    }

    @Test
    fun shouldChangeSellManufacturingCountWhenBusinessCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.MANUFACTURING,
            CommercialBuildingType.BUSINESS_CENTER
        )
    }

    @Test
    fun shouldChangeRentBusinessCountWhenWarehouseSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.BUSINESS,
            CommercialBuildingType.WAREHOUSE
        )
    }

    @Test
    fun shouldChangeSellBusinessCountWhenShoppingCenterSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.SELL,
            CommercialType.BUSINESS,
            CommercialBuildingType.SHOPPING_CENTER
        )
    }

    @Test
    fun shouldChangeRentLegalAddressCountWhenDetachedBuildingSet() {
        shouldChangeOffersCountWhenCommercialBuildingTypeSet(
            DealType.RENT,
            CommercialType.LEGAL_ADDRESS,
            CommercialBuildingType.DETACHED_BUILDING
        )
    }

    private fun shouldChangeOffersCountWhenCommercialBuildingTypeSet(
        dealType: DealType,
        commercialType: CommercialType,
        vararg commercialBuildingTypes: CommercialBuildingType
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))
                tapOn(lookup.matchesFieldCommercialType())

                performOnCommercialTypeScreen {
                    scrollTo(commercialType.matcher.invoke(lookup))
                        .tapOn()
                    tapOn(lookup.matchesApplyButton())
                }

                scrollToPosition(lookup.matchesFieldCommercialBuildingType()).tapOn()

                performOnCommercialBuildingTypeDialog {
                    for (commercialBuildingType in commercialBuildingTypes) {
                        tapOn(commercialBuildingType.matcher.invoke(lookup))
                    }
                    tapOn(lookup.matchesPositiveButton())
                }
                isCommercialBuildingTypeEquals(
                    commercialBuildingTypes.joinToString { it.expected }
                )
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *commercialBuildingTypes.map(CommercialBuildingType::param).toTypedArray()
            )
        )
    }
}
