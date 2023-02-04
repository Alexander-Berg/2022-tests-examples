package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import com.yandex.mobile.realty.core.robot.performOnPriceDialog
import org.junit.Test

/**
 * @author scrooge on 08.05.2019.
 */
class PriceTest : FilterParamTest() {

    @Test
    fun shouldChangeSellAnyApartmentOffersCountWhenMinPriceSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.APARTMENT,
            offerCategoryFactory = OfferCategory.ANY,
            priceType = PriceType.PER_OFFER,
            priceMin = 1,
            expected = "от 1\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellSecondaryApartmentOffersCountWhenMaxPricePerMeterSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.APARTMENT,
            offerCategoryFactory = OfferCategory.SECONDARY,
            priceType = PriceType.PER_METER,
            priceMax = 10,
            expected = "до 10\u00a0\u20BD\u2009/\u2009м²"
        )
    }

    @Test
    fun shouldChangeSellNewBuildingApartmentOffersCountWhenMinMaxPricePerMeterSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.APARTMENT,
            offerCategoryFactory = OfferCategory.PRIMARY,
            priceType = PriceType.PER_OFFER,
            priceMin = 25,
            priceMax = 50,
            expected = "25 – 50\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeRentApartmentOffersCountWhenMinMaxPriceSet() {
        selectPrice(
            dealType = DealType.RENT,
            propertyType = PropertyType.APARTMENT,
            priceMin = 25,
            priceMax = 50,
            expected = "25 – 50\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenMinPriceSet() {
        selectPrice(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM,
            priceType = PriceType.PER_OFFER,
            priceMin = 1000,
            expected = "от 1 тыс.\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellRoomOffersCountWhenMaxPricePerMeterSet() {
        selectPrice(
            dealType = DealType.SELL,
            propertyType = PropertyType.ROOM,
            priceType = PriceType.PER_METER,
            priceMax = 1000,
            expected = "до 1 тыс.\u00a0\u20BD\u2009/\u2009м²"
        )
    }

    @Test
    fun shouldChangeRentRoomOffersCountWhenMinMaxPriceSet() {
        selectPrice(
            dealType = DealType.RENT,
            propertyType = PropertyType.ROOM,
            priceMin = 25000,
            priceMax = 50000,
            expected = "25 – 50 тыс.\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellAnyHouseOffersCountWhenMinPriceSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.HOUSE,
            offerCategoryFactory = OfferCategory.ANY,
            priceType = PriceType.PER_OFFER,
            priceMin = 1000000,
            expected = "от 1 млн\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellSecondaryHouseOffersCountWhenMaxPricePerMeterSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.HOUSE,
            offerCategoryFactory = OfferCategory.SECONDARY,
            priceType = PriceType.PER_METER,
            priceMax = 1000000,
            expected = "до 1 млн\u00a0\u20BD\u2009/\u2009м²"
        )
    }

    @Test
    fun shouldChangeSellVillageHouseOffersCountWhenMinMaxPriceSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.HOUSE,
            offerCategoryFactory = OfferCategory.PRIMARY,
            priceMin = 2000000,
            priceMax = 5000000,
            expected = "2 – 5 млн\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeRentHouseOffersCountWhenMinMaxPriceSet() {
        selectPrice(
            dealType = DealType.RENT,
            propertyType = PropertyType.HOUSE,
            priceMin = 2000000,
            priceMax = 5000000,
            expected = "2 – 5 млн\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellAnyLotOffersCountWhenMinPriceSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.LOT,
            offerCategoryFactory = OfferCategory.ANY,
            priceType = PriceType.PER_OFFER,
            priceMin = 1000000,
            expected = "от 1 млн\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellSecondaryLotOffersCountWhenMaxPricePerAreSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.LOT,
            offerCategoryFactory = OfferCategory.SECONDARY,
            priceType = PriceType.PER_ARE,
            priceMax = 1000000,
            expected = "до 1 млн\u00a0\u20BD\u2009/\u2009сот."
        )
    }

    @Test
    fun shouldChangeSellVillageLotOffersCountWhenMinMaxPricePerAreSet() {
        selectSellApartmentHouseLotPrice(
            propertyType = PropertyType.LOT,
            offerCategoryFactory = OfferCategory.PRIMARY,
            priceType = PriceType.PER_ARE,
            priceMin = 3000000,
            priceMax = 10000000,
            expected = "3 – 10 млн\u00a0\u20BD\u2009/\u2009сот."
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenMinMaxPriceSet() {
        selectCommercialPrice(
            dealType = DealType.SELL,
            priceType = PriceType.PER_OFFER,
            priceMin = 500000,
            priceMax = 1000000,
            expected = "500 тыс. – 1 млн\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellCommercialOffersCountWhenMinPricePerMeterSet() {
        selectCommercialPrice(
            dealType = DealType.SELL,
            priceType = PriceType.PER_METER,
            priceMin = 1000000,
            expected = "от 1 млн\u00a0\u20BD\u2009/\u2009м²"
        )
    }

    @Test
    fun shouldChangeSellCommercialLandOffersCountWhenMaxPricePerAreSet() {
        selectCommercialPrice(
            dealType = DealType.SELL,
            commercialType = CommercialType.LAND,
            priceType = PriceType.PER_ARE,
            priceMax = 1000000,
            expected = "до 1 млн\u00a0\u20BD\u2009/\u2009сот."
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenMinMaxPricePerMonthSet() {
        selectCommercialPrice(
            dealType = DealType.RENT,
            priceType = PriceType.PER_OFFER,
            pricingPeriod = PricingPeriod.PER_MONTH,
            priceMin = 500000,
            priceMax = 1000000,
            expected = "500 тыс. – 1 млн\u00a0\u20BD\u2009/\u2009мес."
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenMinPricePerYearSet() {
        selectCommercialPrice(
            dealType = DealType.RENT,
            priceType = PriceType.PER_OFFER,
            pricingPeriod = PricingPeriod.PER_YEAR,
            priceMin = 1000000,
            expected = "от 1 млн\u00a0\u20BD\u2009/\u2009год"
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenMaxPricePerMeterPerMonthSet() {
        selectCommercialPrice(
            dealType = DealType.RENT,
            priceType = PriceType.PER_METER,
            pricingPeriod = PricingPeriod.PER_MONTH,
            priceMax = 1000000,
            expected = "до 1 млн\u00a0\u20BD\u2009/\u2009м²\u2009в\u2009мес."
        )
    }

    @Test
    fun shouldChangeRentCommercialOffersCountWhenMinMaxPricePerMeterPerYearSet() {
        selectCommercialPrice(
            dealType = DealType.RENT,
            priceType = PriceType.PER_METER,
            pricingPeriod = PricingPeriod.PER_YEAR,
            priceMin = 500000,
            priceMax = 1000000,
            expected = "500 тыс. – 1 млн\u00a0\u20BD\u2009/\u2009м²\u2009в\u2009год"
        )
    }

    @Test
    fun shouldChangeRentCommercialLandOffersCountWhenMinPricePerArePerMonthSet() {
        selectCommercialPrice(
            dealType = DealType.RENT,
            commercialType = CommercialType.LAND,
            priceType = PriceType.PER_ARE,
            pricingPeriod = PricingPeriod.PER_MONTH,
            priceMin = 1000000,
            expected = "от 1 млн\u00a0\u20BD\u2009/\u2009сот.\u2009в\u2009мес."
        )
    }

    @Test
    fun shouldChangeRentCommercialLandOffersCountWhenMaxPricePerArePerYearSet() {
        selectCommercialPrice(
            dealType = DealType.RENT,
            commercialType = CommercialType.LAND,
            priceType = PriceType.PER_ARE,
            pricingPeriod = PricingPeriod.PER_YEAR,
            priceMax = 1000000,
            expected = "до 1 млн\u00a0\u20BD\u2009/\u2009сот.\u2009в\u2009год"
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenMinMaxPriceSet() {
        selectPrice(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE,
            priceType = PriceType.PER_OFFER,
            priceMin = 500000,
            priceMax = 1000000,
            expected = "500 тыс. – 1 млн\u00a0\u20BD"
        )
    }

    @Test
    fun shouldChangeSellGarageOffersCountWhenMinPricePerMeterSet() {
        selectPrice(
            dealType = DealType.SELL,
            propertyType = PropertyType.GARAGE,
            priceType = PriceType.PER_METER,
            priceMin = 1000000,
            expected = "от 1 млн\u00a0\u20BD\u2009/\u2009м²"
        )
    }

    @Test
    fun shouldChangeRentGarageOffersCountWhenMaxPriceSet() {
        selectPrice(
            dealType = DealType.RENT,
            propertyType = PropertyType.GARAGE,
            priceMax = 25000,
            expected = "до 25 тыс.\u00a0\u20BD"
        )
    }

    private fun selectSellApartmentHouseLotPrice(
        propertyType: PropertyType,
        offerCategoryFactory: OfferCategoryFactory,
        priceType: PriceType? = null,
        priceMin: Int? = null,
        priceMax: Int? = null,
        expected: String
    ) {
        val offerCategory = offerCategoryFactory.invoke(propertyType)
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(DealType.SELL.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(offerCategory.matcher.invoke(lookup))
                tapOn(lookup.matchesPriceValue())

                performOnPriceDialog {
                    waitUntilKeyboardAppear()
                    priceType?.let { tapOn(it.matcher.invoke(lookup)) }
                    priceMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    priceMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isPriceEquals(expected)
            },
            params = arrayOf(
                DealType.SELL.param,
                propertyType.param,
                *offerCategory.params,
                priceType?.param ?: PriceType.PER_OFFER.param,
                "priceMin" to priceMin?.toString(),
                "priceMax" to priceMax?.toString()
            )
        )
    }

    private fun selectPrice(
        dealType: DealType,
        propertyType: PropertyType,
        priceType: PriceType? = null,
        priceMin: Int? = null,
        priceMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                tapOn(lookup.matchesPriceValue())

                performOnPriceDialog {
                    waitUntilKeyboardAppear()
                    priceType?.let { tapOn(it.matcher.invoke(lookup)) }
                    priceMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    priceMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isPriceEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                propertyType.param,
                priceType?.param ?: PriceType.PER_OFFER.param,
                "priceMin" to priceMin?.toString(),
                "priceMax" to priceMax?.toString()
            )
        )
    }

    private fun selectCommercialPrice(
        dealType: DealType,
        commercialType: CommercialType? = null,
        priceType: PriceType? = null,
        pricingPeriod: PricingPeriod? = null,
        priceMin: Int? = null,
        priceMax: Int? = null,
        expected: String
    ) {
        shouldChangeOffersCount(
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(PropertyType.COMMERCIAL.matcher.invoke(lookup))
                commercialType?.let { commercialType ->
                    tapOn(lookup.matchesFieldCommercialType())
                    performOnCommercialTypeScreen {
                        scrollTo(commercialType.matcher.invoke(lookup))
                            .tapOn()
                        tapOn(lookup.matchesApplyButton())
                    }
                }
                tapOn(lookup.matchesPriceValue())
                performOnPriceDialog {
                    waitUntilKeyboardAppear()
                    priceType?.let { tapOn(it.matcher.invoke(lookup)) }
                    pricingPeriod?.let { tapOn(it.matcher.invoke(lookup)) }
                    priceMin?.let { typeText(lookup.matchesValueFrom(), it.toString()) }
                    priceMax?.let { typeText(lookup.matchesValueTo(), it.toString()) }
                    tapOn(lookup.matchesPositiveButton())
                }
                isPriceEquals(expected)
            },
            params = arrayOf(
                dealType.param,
                PropertyType.COMMERCIAL.param,
                commercialType?.param,
                priceType?.param ?: PriceType.PER_OFFER.param,
                pricingPeriod?.param,
                "priceMin" to priceMin?.toString(),
                "priceMax" to priceMax?.toString()
            )
        )
    }
}
