package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import com.yandex.mobile.realty.core.robot.performOnCommissionDialog
import org.junit.Test

/**
 * @author andrikeev on 21/09/2020.
 */
class CommercialCommissionTest : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            emptySet(),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeOfficeOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.OFFICE),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeWarehouseOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.WAREHOUSE),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeFreePurposeOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.FREE_PURPOSE),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeAutoRepairOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.AUTO_REPAIR),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeBusinessOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.BUSINESS),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeManufacturingOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.MANUFACTURING),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangePublicCateringOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.PUBLIC_CATERING),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeRetailOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.RETAIL),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeHotelOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.HOTEL),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeLegalAddressOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.LEGAL_ADDRESS),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeLandOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.LAND),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeNonLandTypesOffersCountWhenWithoutCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            CommercialType.rentNonLandTypes,
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            emptySet(),
            Commission.WITHOUT_COMMISSION
        )
    }

    @Test
    fun shouldChangeOfficeOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.OFFICE),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeWarehouseOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.WAREHOUSE),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeFree_purposeOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.FREE_PURPOSE),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeAuto_repairOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.AUTO_REPAIR),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeBusinessOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.BUSINESS),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeManufacturingOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.MANUFACTURING),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangePublic_cateringOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.PUBLIC_CATERING),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeRetailOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.RETAIL),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeHotelOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.HOTEL),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeLegal_addressOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.LEGAL_ADDRESS),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeLandOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            setOf(CommercialType.LAND),
            Commission.WITH_COMMISSION
        )
    }

    @Test
    fun shouldChangeNonLandTypesOffersCountWhenWithCommissionSet() {
        shouldChangeOffersCountWhenCommissionSet(
            CommercialType.rentNonLandTypes,
            Commission.WITH_COMMISSION
        )
    }

    private fun shouldChangeOffersCountWhenCommissionSet(
        commercialType: Set<CommercialType>,
        commission: Commission
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                onView(lookup.matchesDealTypeSelector()).tapOn()
                onView(DealType.RENT.matcher.invoke(lookup)).tapOn()

                onView(lookup.matchesPropertyTypeSelector()).tapOn()
                onView(PropertyType.COMMERCIAL.matcher.invoke(lookup)).tapOn()

                tapOn(lookup.matchesFieldCommercialType())
                performOnCommercialTypeScreen {
                    commercialType.forEach { scrollTo(it.matcher.invoke(lookup)).tapOn() }
                    tapOn(lookup.matchesApplyButton())
                }

                scrollToPosition(lookup.matchesFieldCommercialCommission()).tapOn()

                performOnCommissionDialog {
                    onView(commission.matcher.invoke(lookup)).tapOn()
                }

                isCommissionEquals(commission.expected)
            },
            params = arrayOf(
                DealType.RENT.param,
                PropertyType.COMMERCIAL.param,
                *commercialType.map(CommercialType::param).toTypedArray(),
                commission.param
            )
        )
    }
}
