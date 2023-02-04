package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnBusinessCenterClassDialog
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author scrooge on 31.07.2019.
 */
class BusinessCenterClassTest : FilterParamTest() {

    @Test
    fun shouldChangeSellOfficeOffersCountWhenClassASet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.OFFICE,
            BusinessCenterClass.A
        )
    }

    @Test
    fun shouldChangeRentOfficeOffersCountWhenClassASet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.RETAIL,
            BusinessCenterClass.A
        )
    }

    @Test
    fun shouldChangeSellRetailOffersCountWhenClassAPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.RETAIL,
            BusinessCenterClass.A_PLUS
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenClassAPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.OFFICE,
            BusinessCenterClass.A_PLUS
        )
    }

    @Test
    fun shouldChangeSellFreePurposeOffersCountWhenClassBSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.FREE_PURPOSE,
            BusinessCenterClass.B
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenClassBSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.FREE_PURPOSE,
            BusinessCenterClass.B
        )
    }

    @Test
    fun shouldChangeSellPublicCateringOffersCountWhenClassBPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.PUBLIC_CATERING,
            BusinessCenterClass.B_PLUS
        )
    }

    @Test
    fun shouldChangeRentPublicCateringOffersCountWhenClassBPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.PUBLIC_CATERING,
            BusinessCenterClass.B_PLUS
        )
    }

    @Test
    fun shouldChangeSellHotelOffersCountWhenClassCSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.HOTEL,
            BusinessCenterClass.C
        )
    }

    @Test
    fun shouldChangeRentHotelOffersCountWhenClassCSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.HOTEL,
            BusinessCenterClass.C
        )
    }

    @Test
    fun shouldChangeSellAutoRepairOffersCountWhenClassCPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.AUTO_REPAIR,
            BusinessCenterClass.C_PLUS
        )
    }

    @Test
    fun shouldChangeRentAutoRepairOffersCountWhenClassCPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.AUTO_REPAIR,
            BusinessCenterClass.C_PLUS
        )
    }

    @Test
    fun shouldChangeSellWarehouseOffersCountWhenClassASet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.WAREHOUSE,
            BusinessCenterClass.A
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenClassASet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.WAREHOUSE,
            BusinessCenterClass.A
        )
    }

    @Test
    fun shouldChangeSellManufacturingOffersCountWhenClassAPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.MANUFACTURING,
            BusinessCenterClass.A_PLUS
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenClassAPlusSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.MANUFACTURING,
            BusinessCenterClass.A_PLUS
        )
    }

    @Test
    fun shouldChangeSellBusinessOffersCountWhenClassBSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.SELL,
            CommercialType.BUSINESS,
            BusinessCenterClass.B
        )
    }

    @Test
    fun shouldChangeRentBusinessOffersCountWhenClassBSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.BUSINESS,
            BusinessCenterClass.B
        )
    }

    @Test
    fun shouldChangeRentLegalAddressOffersCountWhenClassBSet() {
        shouldChangeOffersCountWhenBusinessCenterClassSet(
            DealType.RENT,
            CommercialType.LEGAL_ADDRESS,
            BusinessCenterClass.B_PLUS
        )
    }

    private fun shouldChangeOffersCountWhenBusinessCenterClassSet(
        dealType: DealType,
        commercialType: CommercialType,
        businessCenterClass: BusinessCenterClass

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

                scrollToPosition(lookup.matchesFieldBusinessCenterClassType()).tapOn()

                performOnBusinessCenterClassDialog {
                    tapOn(businessCenterClass.matcher.invoke(lookup))
                    tapOn(lookup.matchesPositiveButton())
                }

                isCommercialBusinessCenterClassEquals(businessCenterClass.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                CommercialBuildingType.BUSINESS_CENTER.param,
                businessCenterClass.param
            )
        )
    }
}
