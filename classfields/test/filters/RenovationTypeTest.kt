package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnRenovationTypeDialog
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author scrooge on 02.07.2019.
 */
@RunWith(Parameterized::class)
class RenovationTypeTest(
    private val dealType: DealType,
    private val propertyType: PropertyType,
    private val renovationType: RenovationType,
) : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenRenovationTypeSet() {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldRenovationType()).tapOn()

                performOnRenovationTypeDialog {
                    tapOn(renovationType.matcher)
                    tapOn(lookup.matchesPositiveButton())
                }

                isRenovationTypeEquals(renovationType.expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                renovationType.param
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1} with renovation type {2}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(DealType.SELL, PropertyType.APARTMENT, RenovationType.COSMETIC_DONE),
                arrayOf(DealType.RENT, PropertyType.APARTMENT, RenovationType.DESIGNER_RENOVATION),
                arrayOf(DealType.RENT, PropertyType.APARTMENT, RenovationType.NON_GRANDMOTHER),
                arrayOf(DealType.SELL, PropertyType.ROOM, RenovationType.EURO),
                arrayOf(DealType.RENT, PropertyType.ROOM, RenovationType.NEEDS_RENOVATION),
                arrayOf(DealType.RENT, PropertyType.ROOM, RenovationType.NON_GRANDMOTHER),
                arrayOf(DealType.SELL, PropertyType.HOUSE, RenovationType.NEEDS_RENOVATION),
                arrayOf(DealType.RENT, PropertyType.HOUSE, RenovationType.COSMETIC_DONE),
                arrayOf(DealType.RENT, PropertyType.HOUSE, RenovationType.NON_GRANDMOTHER),
            )
        }
    }
}
