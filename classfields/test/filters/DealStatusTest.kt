package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.screen.DealStatusDialogScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author merionkov 26.05.2022
 */
@RunWith(Parameterized::class)
class DealStatusTest(private val dealStatus: DealStatus) : FilterParamTest() {

    @Test
    fun shouldChangeOffersCountWhenRenovationTypeSet() {
        shouldChangeOffersCount(
            actionConfiguration = {
                onScreen<FiltersScreen> {
                    dealTypeSelector.click()
                    dealTypePopupBuy.click()
                    propertyTypeSelector.click()
                    propertyTypePopupApartment.click()
                    listView.scrollTo(dealStatusItem).click()
                    onScreen<DealStatusDialogScreen> {
                        listView.scrollTo(dealStatus.matcher.invoke()).click()
                    }
                    dealStatusValue.isTextEquals(dealStatus.expected)
                }
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                dealStatus.param,
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "SELL APARTMENT with deal status {0}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(DealStatus.COUNTERSALE),
                arrayOf(DealStatus.REASSIGNMENT),
            )
        }
    }
}
