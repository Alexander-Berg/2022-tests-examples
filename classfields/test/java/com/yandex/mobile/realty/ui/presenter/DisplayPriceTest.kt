package com.yandex.mobile.realty.ui.presenter

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.common.Price
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author shpigun on 24.09.2020
 */
@RunWith(RobolectricTestRunner::class)
class DisplayPriceTest : RobolectricTest() {

    @Test
    fun priceCurrencyRub() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0₽", price.getDisplayString())
    }

    @Test
    fun priceCurrencyEur() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.EUR,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0€", price.getDisplayString())
    }

    @Test
    fun priceCurrencyUsd() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.USD,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0$", price.getDisplayString())
    }

    @Test
    fun priceUnitPerMeter() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_METER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0₽\u2009/\u2009м²", price.getDisplayString())
    }

    @Test
    fun priceUnitPerAre() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_ARE,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0₽\u2009/\u2009сот.", price.getDisplayString())
    }

    @Test
    fun priceUnitPerHectare() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_HECTARE,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0₽\u2009/\u2009га", price.getDisplayString())
    }

    @Test
    fun priceUnitPerSquareKilometer() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_SQUARE_KILOMETER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0₽\u2009/\u2009км²", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerDayUnit() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_DAY
        )
        assertEquals("1\u00a0₽\u2009/\u2009сутки", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerDayUnitPerMeter() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_METER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_DAY
        )
        assertEquals("1\u00a0₽\u2009/\u2009м²\u2009в\u2009сутки", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerDayUnitPerAre() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_ARE,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_DAY
        )
        assertEquals("1\u00a0₽\u2009/\u2009сот.\u2009в\u2009сутки", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerDayUnitPerHectare() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_HECTARE, currency = Price.Currency.RUB,

            period = Price.Period.PER_DAY
        )
        assertEquals("1\u00a0₽\u2009/\u2009га\u2009в\u2009сутки", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerDayUnitPerSquareKilometer() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_SQUARE_KILOMETER, currency = Price.Currency.RUB,
            period = Price.Period.PER_DAY
        )
        assertEquals("1\u00a0₽\u2009/\u2009км²\u2009в\u2009сутки", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerMonth() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_MONTH
        )
        assertEquals("1\u00a0₽\u2009/\u2009мес.", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerMonthUnitPerMeter() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_METER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_MONTH
        )
        assertEquals("1\u00a0₽\u2009/\u2009м²\u2009в\u2009мес.", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerMonthUnitPerAre() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_ARE,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_MONTH
        )
        assertEquals("1\u00a0₽\u2009/\u2009сот.\u2009в\u2009мес.", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerMonthUnitPerHectare() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_HECTARE,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_MONTH
        )
        assertEquals("1\u00a0₽\u2009/\u2009га\u2009в\u2009мес.", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerMonthUnitPerSquareKilometer() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_SQUARE_KILOMETER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_MONTH
        )
        assertEquals("1\u00a0₽\u2009/\u2009км²\u2009в\u2009мес.", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerYearUnit() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_YEAR
        )
        assertEquals("1\u00a0₽\u2009/\u2009год", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerYearUnitPerMeter() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_METER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_YEAR
        )
        assertEquals("1\u00a0₽\u2009/\u2009м²\u2009в\u2009год", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerYearUnitPerAre() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_ARE,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_YEAR
        )
        assertEquals("1\u00a0₽\u2009/\u2009сот.\u2009в\u2009год", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerYearUnitPerHectare() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_HECTARE,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_YEAR
        )
        assertEquals("1\u00a0₽\u2009/\u2009га\u2009в\u2009год", price.getDisplayString())
    }

    @Test
    fun pricePeriodPerYearUnitPerSquareKilometer() {
        val price = createPrice(
            value = ONE,
            unit = Price.Unit.PER_SQUARE_KILOMETER,
            currency = Price.Currency.RUB,
            period = Price.Period.PER_YEAR
        )
        assertEquals("1\u00a0₽\u2009/\u2009км²\u2009в\u2009год", price.getDisplayString())
    }

    @Test
    fun priceValueThousand() {
        val price = createPrice(
            value = THOUSAND,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0000\u00a0₽", price.getDisplayString())
    }

    @Test
    fun priceValueMillion() {
        val price = createPrice(
            value = MILLION,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0000\u00a0000\u00a0₽", price.getDisplayString())
    }

    @Test
    fun priceValueBillion() {
        val price = createPrice(
            value = BILLION,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0000\u00a0000\u00a0000\u00a0₽", price.getDisplayString())
    }

    @Test
    fun priceRangeWithLowerBound() {
        val priceRange = createPriceRange(
            lowerBound = THOUSAND,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("от 1\u00a0тыс.\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeWithUpperBound() {
        val priceRange = createPriceRange(
            upperBound = THOUSAND,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("до 1\u00a0тыс.\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeClosedOnes() {
        val priceRange = createPriceRange(
            lowerBound = ONE,
            upperBound = FIVE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1 – 5\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeClosedThousands() {
        val priceRange = createPriceRange(
            lowerBound = THOUSAND,
            upperBound = FIVE_THOUSANDS,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1 – 5\u00a0тыс.\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeClosedMillions() {
        val priceRange = createPriceRange(
            lowerBound = MILLION,
            upperBound = FIVE_MILLIONS,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1 – 5\u00a0млн\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeClosedBillions() {
        val priceRange = createPriceRange(
            lowerBound = BILLION,
            upperBound = FIVE_BILLIONS,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1 – 5\u00a0млрд\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeClosedWithDifferentBoundsOrder() {
        val priceRange = createPriceRange(
            lowerBound = MILLION,
            upperBound = BILLION,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0млн – 1\u00a0млрд\u00a0₽", priceRange.getDisplayString())
    }

    @Test
    fun priceRangeWithSameBounds() {
        val priceRange = createPriceRange(
            lowerBound = ONE,
            upperBound = ONE,
            unit = Price.Unit.PER_OFFER,
            currency = Price.Currency.RUB,
            period = Price.Period.WHOLE_LIFE
        )
        assertEquals("1\u00a0₽", priceRange.getDisplayString())
    }

    private fun createPrice(
        value: Long,
        unit: Price.Unit,
        currency: Price.Currency,
        period: Price.Period
    ): Price {
        return Price(value, unit, currency, period)
    }

    private fun createPriceRange(
        lowerBound: Long? = null,
        upperBound: Long? = null,
        unit: Price.Unit,
        currency: Price.Currency,
        period: Price.Period
    ): Range<Price> {
        return Range.valueOf(
            lowerBound?.let { Price(it, unit, currency, period) },
            upperBound?.let { Price(it, unit, currency, period) }
        )
    }

    companion object {
        private const val ONE = 1L
        private const val FIVE = 5L
        private const val THOUSAND = 1_000L
        private const val FIVE_THOUSANDS = 5_000L
        private const val MILLION = 1_000_000L
        private const val FIVE_MILLIONS = 5_000_000L
        private const val BILLION = 1_000_000_000L
        private const val FIVE_BILLIONS = 5_000_000_000L
    }
}
