package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author scrooge on 01.08.2019.
 */
class DealParamsTest : FilterParamTest() {

    @Test
    fun shouldChangeRentOfficeOffersCountWhenDealParamsUtilitiesSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.RETAIL,
            DealParams.UTILITIES
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenDealParamsElectricitySet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.OFFICE,
            DealParams.ELECTRICITY
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenDealParamsCleaningSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.FREE_PURPOSE,
            DealParams.CLEANING
        )
    }

    @Test
    fun shouldChangeRentPublicCateringOffersCountWhenDealParamsNdsSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.PUBLIC_CATERING,
            DealParams.NDS
        )
    }

    @Test
    fun shouldChangeRentHotelOffersCountWhenDealParamsUsnSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.HOTEL,
            DealParams.USN
        )
    }

    @Test
    fun shouldChangeRentAutoRepairOffersCountWhenDealParamsUtilitiesSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.AUTO_REPAIR,
            DealParams.UTILITIES
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenDealParamsElectricitySet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.WAREHOUSE,
            DealParams.ELECTRICITY
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenDealParamsNdsSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.MANUFACTURING,
            DealParams.NDS
        )
    }

    @Test
    fun shouldChangeRentBusinessOffersCountWhenDealParamsUsnSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.BUSINESS,
            DealParams.USN
        )
    }

    @Test
    fun shouldChangeRentLegalAddressOffersCountWhenDealParamsNdsSet() {
        shouldChangeOffersCountWhenDealParamsSet(
            CommercialType.LEGAL_ADDRESS,
            DealParams.NDS
        )
    }

    private fun shouldChangeOffersCountWhenDealParamsSet(
        commercialType: CommercialType,
        dealParams: DealParams

    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.RENT.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))
                tapOn(lookup.matchesFieldCommercialType())

                performOnCommercialTypeScreen {
                    scrollTo(commercialType.matcher.invoke(lookup))
                        .tapOn()
                    tapOn(lookup.matchesApplyButton())
                }

                scrollToPosition(dealParams.fieldMatcher.invoke(lookup))
                    .tapOn()

                isChecked(dealParams.valueMatcher.invoke(lookup))
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.COMMERCIAL.param,
                dealParams.param
            )
        )
    }
}
