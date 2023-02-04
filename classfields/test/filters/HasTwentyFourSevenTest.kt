package com.yandex.mobile.realty.test.filters

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

/**
 * @author andrikeev on 16/09/2020.
 */
@LargeTest
@RunWith(Parameterized::class)
class HasTwentyFourSevenTest(
    private val dealType: DealType,
    private val commercialType: EnumSet<CommercialType>,
    @Suppress("unused") private val name: String
) : FilterParamTest() {

    @Test
    fun shouldChangeCommercialOffersCountWhenHasTwentyFourSevenSet() {
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

                scrollToPosition(lookup.matchesFieldHasTwentyFourSeven()).tapOn()

                isHasTwentyFourSevenSelected()
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *commercialType.map(CommercialType::param).toTypedArray(),
                "hasTwentyFourSeven" to "YES"
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} COMMERCIAL {2} with twenty four seven access")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.OFFICE),
                    "OFFICE"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.RETAIL),
                    "RETAIL"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.FREE_PURPOSE),
                    "FREE_PURPOSE"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.WAREHOUSE),
                    "WAREHOUSE"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.MANUFACTURING),
                    "MANUFACTURING"
                ),
                arrayOf(
                    DealType.SELL,
                    CommercialType.sellNonLandTypes,
                    "all non land types"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.OFFICE),
                    "OFFICE"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.RETAIL),
                    "RETAIL"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.FREE_PURPOSE),
                    "FREE_PURPOSE"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.WAREHOUSE),
                    "WAREHOUSE"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.MANUFACTURING),
                    "MANUFACTURING"
                ),
                arrayOf(
                    DealType.RENT,
                    CommercialType.rentNonLandTypes,
                    "all non land types"
                )
            )
        }
    }
}
