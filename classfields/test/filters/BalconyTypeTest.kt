package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnBalconyTypeDialog
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author scrooge on 02.07.2019.
 */
@RunWith(Parameterized::class)
class BalconyTypeTest(
    private val dealType: DealType,
    private val balcony: BalconyType,
) : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenBalconyTypeSet() {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))

                scrollToPosition(lookup.matchesFieldBalconyType())
                    .tapOn()
                performOnBalconyTypeDialog {
                    tapOn(balcony.matcher.invoke(lookup))
                }
                isBalconyTypeEquals(balcony.expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.APARTMENT.param,
                balcony.param
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} APARTMENT with balcony type {1}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(DealType.SELL, BalconyType.BALCONY),
                arrayOf(DealType.SELL, BalconyType.LOGGIA),
                arrayOf(DealType.SELL, BalconyType.ANY),
                arrayOf(DealType.RENT, BalconyType.BALCONY),
                arrayOf(DealType.RENT, BalconyType.LOGGIA),
                arrayOf(DealType.RENT, BalconyType.ANY),
            )
        }
    }
}
