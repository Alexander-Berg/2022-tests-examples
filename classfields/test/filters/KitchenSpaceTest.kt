package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnKitchenSpaceDialog
import org.junit.Test

/**
 * @author scrooge on 12.06.2019.
 */
class KitchenSpaceTest : FilterParamTest() {

    @Test
    fun shouldChangeSellApartmentAnyOffersCountWhenMinKitchenSpaceSet() {
        selectSellApartmentKitchenSpace(
            OfferCategory.ANY,
            kitchenSpaceMin = 1,
            expected = "от 1 м²"
        )
    }

    @Test
    fun shouldChangeSellNewBuildingOffersCountWhenMaxKitchenSpaceSet() {
        selectSellApartmentKitchenSpace(
            OfferCategory.PRIMARY,
            kitchenSpaceMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeSellApartmentSecondaryOffersCountWhenMinMaxKitchenSpaceSet() {
        selectSellApartmentKitchenSpace(
            OfferCategory.SECONDARY,
            kitchenSpaceMin = 1,
            kitchenSpaceMax = 99,
            expected = "1 – 99 м²"
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenMaxKitchenSpaceSet() {
        selectKitchenSpace(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT,
            kitchenSpaceMax = 99,
            expected = "до 99 м²"
        )
    }

    @Test
    fun shouldChangeSellOffersCountWhenMinMaxKitchenSpaceSet() {
        selectKitchenSpace(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM,
            kitchenSpaceMin = 1,
            kitchenSpaceMax = 99,
            expected = "1 – 99 м²"
        )
    }

    private fun selectKitchenSpace(
        dealType: DealType,
        propertyType: PropertyType,
        kitchenSpaceMin: Int? = null,
        kitchenSpaceMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldKitchenSpace()).tapOn()
                performOnKitchenSpaceDialog {
                    waitUntilKeyboardAppear()
                    kitchenSpaceMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    kitchenSpaceMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isKitchenSpaceEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                "kitchenSpaceMin" to kitchenSpaceMin?.toString(),
                "kitchenSpaceMax" to kitchenSpaceMax?.toString()
            )
        )
    }

    private fun selectSellApartmentKitchenSpace(
        offerCategoryFactory: OfferCategoryFactory,
        kitchenSpaceMin: Int? = null,
        kitchenSpaceMax: Int? = null,
        expected: String
    ) {
        val offerCategory = offerCategoryFactory.invoke(PropertyType.APARTMENT)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                scrollToPosition(lookup.matchesFieldKitchenSpace()).tapOn()
                performOnKitchenSpaceDialog {
                    waitUntilKeyboardAppear()
                    kitchenSpaceMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    kitchenSpaceMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isKitchenSpaceEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                PropertyType.APARTMENT.param,
                "kitchenSpaceMin" to kitchenSpaceMin?.toString(),
                "kitchenSpaceMax" to kitchenSpaceMax?.toString(),
                *offerCategory.params
            )
        )
    }
}
