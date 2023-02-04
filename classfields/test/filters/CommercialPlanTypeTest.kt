package com.yandex.mobile.realty.test.filters

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnCommercialPanTypeDialog
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

/**
 * @author andrikeev on 14/09/2020.
 */
@LargeTest
@RunWith(Parameterized::class)
class CommercialPlanTypeTest(
    private val dealType: DealType,
    private val commercialType: EnumSet<CommercialType>,
    private val planType: CommercialPlanType,
    @Suppress("unused") private val name: String
) : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenCommercialPlanTypeSet() {
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

                scrollToPosition(lookup.matchesFieldCommercialPlanType()).tapOn()
                performOnCommercialPanTypeDialog {
                    onView(planType.matcher.invoke(lookup)).tapOn()
                }

                isCommercialPlanTypeEquals(planType.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *commercialType.map(CommercialType::param).toTypedArray(),
                planType.param
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
                    CommercialPlanType.CABINET,
                    "OFFICE with CABINET plan type"
                ),
                arrayOf(
                    DealType.SELL,
                    EnumSet.of(CommercialType.FREE_PURPOSE),
                    CommercialPlanType.CORRIDOR,
                    "FREE_PURPOSE with CORRIDOR plan type"
                ),
                arrayOf(
                    DealType.SELL,
                    CommercialType.sellNonLandTypes,
                    CommercialPlanType.OPEN_SPACE,
                    "all non land types with OPEN_SPACE plan type"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.OFFICE),
                    CommercialPlanType.CORRIDOR,
                    "OFFICE with CORRIDOR plan type"
                ),
                arrayOf(
                    DealType.RENT,
                    EnumSet.of(CommercialType.FREE_PURPOSE),
                    CommercialPlanType.OPEN_SPACE,
                    "FREE_PURPOSE with OPEN_SPACE plan type"
                ),
                arrayOf(
                    DealType.RENT,
                    CommercialType.rentNonLandTypes,
                    CommercialPlanType.CABINET,
                    "all non land types with CABINET plan type"
                )
            )
        }
    }
}
