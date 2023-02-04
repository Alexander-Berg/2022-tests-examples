package com.yandex.mobile.realty.test.filters

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnCommercialRenovationDialog
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

/**
 * @author andrikeev on 09/09/2020.
 */
@LargeTest
@RunWith(Parameterized::class)
class CommercialRenovationTest(
    private val dealType: DealType,
    private val commercialType: EnumSet<CommercialType>,
    private val renovation: EnumSet<CommercialRenovation>,
    @Suppress("unused") private val name: String
) : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenCommercialRenovationSet() {
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

                scrollToPosition(lookup.matchesFieldCommercialRenovation()).tapOn()
                performOnCommercialRenovationDialog {
                    renovation.forEach { onView(it.matcher.invoke(lookup)).tapOn() }
                    onView(lookup.matchesPositiveButton()).tapOn()
                }

                isCommercialRenovationEquals(renovation.joinToString { it.expected })
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *commercialType.map(CommercialType::param).toTypedArray(),
                *renovation.map(CommercialRenovation::param).toTypedArray()
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} COMMERCIAL {3}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.OFFICE),
                    EnumSet.of(CommercialRenovation.NEEDS_RENOVATION),
                    "OFFICE with NEEDS_RENOVATION"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.RETAIL),
                    EnumSet.of(CommercialRenovation.COSMETIC_DONE),
                    "RETAIL with COSMETIC_DONE"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.FREE_PURPOSE),
                    EnumSet.of(CommercialRenovation.DESIGNER_RENOVATION),
                    "FREE_PURPOSE with DESIGNER_RENOVATION"
                ),
                arrayOf(
                    DealType.SELL,
                    CommercialType.sellNonLandTypes,
                    CommercialRenovation.types,
                    "all non land types with all renovation types"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.RETAIL),
                    EnumSet.of(CommercialRenovation.DESIGNER_RENOVATION),
                    "RETAIL with DESIGNER_RENOVATION"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.FREE_PURPOSE),
                    EnumSet.of(CommercialRenovation.NEEDS_RENOVATION),
                    "FREE_PURPOSE with NEEDS_RENOVATION"
                ),
                arrayOf(
                    DealType.RENT,
                    CommercialType.rentNonLandTypes,
                    CommercialRenovation.types,
                    "all non land types with all renovation types"
                )
            )
        }
    }
}
