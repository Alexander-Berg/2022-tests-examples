package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test

/**
 * @author andrikeev on 16/09/2020.
 */
class CommercialHasVentilationTest : FilterParamTest() {

    @Test
    fun shouldChangeSellOfficeOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.SELL,
            setOf(CommercialType.OFFICE)
        )
    }

    @Test
    fun shouldChangeSellRetailOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.SELL,
            setOf(CommercialType.RETAIL)
        )
    }

    @Test
    fun shouldChangeSellFreePurposeOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.SELL,
            setOf(CommercialType.FREE_PURPOSE)
        )
    }

    @Test
    fun shouldChangeSellWarehouseOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.SELL,
            setOf(CommercialType.WAREHOUSE)
        )
    }

    @Test
    fun shouldChangeSellManufacturingOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.SELL,
            setOf(CommercialType.MANUFACTURING)
        )
    }

    @Test
    fun shouldChangeSellNonLandTypesOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.SELL,
            CommercialType.sellNonLandTypes
        )
    }

    @Test
    fun shouldChangeRentOfficeOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.RENT,
            setOf(CommercialType.OFFICE)
        )
    }

    @Test
    fun shouldChangeRentRetailOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.RENT,
            setOf(CommercialType.RETAIL)
        )
    }

    @Test
    fun shouldChangeRentFreePurposeOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.RENT,
            setOf(CommercialType.FREE_PURPOSE)
        )
    }

    @Test
    fun shouldChangeRentWarehouseOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.RENT,
            setOf(CommercialType.WAREHOUSE)
        )
    }

    @Test
    fun shouldChangeRentManufacturingOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.RENT,
            setOf(CommercialType.MANUFACTURING)
        )
    }

    @Test
    fun shouldChangeRentNonLandTypesOffersCountWhenHasVentilationSet() {
        shouldChangeOffersCountWhenHasVentilationSet(
            DealType.RENT,
            CommercialType.rentNonLandTypes
        )
    }

    private fun shouldChangeOffersCountWhenHasVentilationSet(
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

                scrollToPosition(lookup.matchesFieldHasVentilation()).tapOn()

                isHasVentilationSelected()
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *commercialType.map(CommercialType::param).toTypedArray(),
                "hasVentilation" to "YES"
            )
        )
    }
}
