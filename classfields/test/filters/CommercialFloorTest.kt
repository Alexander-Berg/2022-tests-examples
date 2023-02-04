package com.yandex.mobile.realty.test.filters

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnFirstFloorDialog
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author andrikeev on 08/09/2020.
 */
@LargeTest
@RunWith(Parameterized::class)
class CommercialFloorTest(
    private val dealType: DealType,
    private val floor: FirstFloorRestriction
) : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenCommercialFloorSet() {
        shouldChangeOffersCount(
            actionConfiguration = {
                onView(lookup.matchesDealTypeSelector()).tapOn()
                onView(dealType.matcher.invoke(lookup)).tapOn()
                onView(lookup.matchesPropertyTypeSelector()).tapOn()
                onView(PropertyType.COMMERCIAL.matcher.invoke(lookup)).tapOn()
                scrollToPosition(lookup.matchesFieldCommercialFloor()).tapOn()
                performOnFirstFloorDialog {
                    onView(floor.matcher.invoke(lookup)).tapOn()
                }
                isCommercialFloorEquals(floor.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                *floor.params
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} COMMERCIAL {1} floor")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(
                    DealType.SELL,
                    FirstFloorRestriction.ONLY_FIRST
                ),
                arrayOf(
                    DealType.SELL,
                    FirstFloorRestriction.ABOVE_FIRST
                ),
                arrayOf(
                    DealType.RENT,
                    FirstFloorRestriction.ONLY_FIRST
                ),
                arrayOf(
                    DealType.RENT,
                    FirstFloorRestriction.ABOVE_FIRST
                )
            )
        }
    }
}
