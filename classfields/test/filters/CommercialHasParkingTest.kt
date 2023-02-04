package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author andrikeev on 17/09/2020.
 */
class CommercialHasParkingTest : FilterParamTest() {

    @Test
    fun shouldChangeSellOfficeOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.SELL,
            setOf(CommercialType.OFFICE)
        )
    }

    @Test
    fun shouldChangeSellRetailOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.SELL,
            setOf(CommercialType.RETAIL)
        )
    }

    @Test
    fun shouldChangeSellFreePurposeOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.SELL,
            setOf(CommercialType.FREE_PURPOSE)
        )
    }

    @Test
    fun shouldChangeSellWarehouseOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.SELL,
            setOf(CommercialType.WAREHOUSE)
        )
    }

    @Test
    fun shouldChangeSellManufacturingOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.SELL,
            setOf(CommercialType.MANUFACTURING)
        )
    }

    @Test
    fun shouldChangeSellNonLandTypesOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.SELL,
            CommercialType.sellNonLandTypes
        )
    }

    @Test
    fun shouldChangeRentOfficeOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.RENT,
            setOf(CommercialType.OFFICE)
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.RENT,
            setOf(CommercialType.RETAIL)
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.RENT,
            setOf(CommercialType.FREE_PURPOSE)
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.RENT,
            setOf(CommercialType.WAREHOUSE)
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.RENT,
            setOf(CommercialType.MANUFACTURING)
        )
    }

    @Test
    fun shouldChangeRentNonLandTypesOffersCountWhenHasParkingSet() {
        shouldChangeOffersCountWhenHasParkingSet(
            DealType.RENT,
            CommercialType.rentNonLandTypes
        )
    }

    private fun shouldChangeOffersCountWhenHasParkingSet(
        dealType: DealType,
        commercialType: Set<CommercialType>
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                onView(lookup.matchesDealTypeSelector()).tapOn()
                onView(dealType.matcher.invoke(lookup)).tapOn()

                onView(lookup.matchesPropertyTypeSelector()).tapOn()
                onView(PropertyType.COMMERCIAL.matcher.invoke(lookup)).tapOn()

                tapOn(lookup.matchesFieldCommercialType())
                performOnCommercialTypeScreen {
                    commercialType.forEach { scrollTo(it.matcher.invoke(lookup)).tapOn() }
                    tapOn(lookup.matchesApplyButton())
                }

                scrollToPosition(lookup.matchesFieldHasParking()).tapOn()

                isHasParkingSelected()
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *commercialType.map(CommercialType::param).toTypedArray(),
                "hasParking" to "YES"
            )
        )
    }
}
