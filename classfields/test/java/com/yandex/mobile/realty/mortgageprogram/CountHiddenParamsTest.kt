package com.yandex.mobile.realty.mortgageprogram

import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.mortgageprogram.model.Filter
import com.yandex.mobile.realty.domain.mortgageprogram.model.MortgageRegion
import com.yandex.mobile.realty.ui.presenter.countFormHiddenParams
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author shpigun on 19.04.2021
 */
class CountHiddenParamsTest {

    @Test
    fun testCountFlatTypeAny() {
        val filter = getFilter()
        assertEquals(0, filter.countFormHiddenParams())
    }

    @Test
    fun testCountFlatTypeNew() {
        val filter = getFilter(flatType = Filter.FlatType.NEW)
        assertEquals(0, filter.countFormHiddenParams())
    }

    @Test
    fun testCountFlatTypeSecondary() {
        val filter = getFilter(flatType = Filter.FlatType.SECONDARY)
        assertEquals(0, filter.countFormHiddenParams())
    }

    @Test
    fun testCountRateLowerBound() {
        val filter = getFilter(rate = Range.valueOf(42, null))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountRateUpperBound() {
        val filter = getFilter(rate = Range.valueOf(null, 42))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountRateClosed() {
        val filter = getFilter(rate = Range.valueOf(2, 42))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeYoungFamily() {
        val filter = getFilter(mortgageType = listOf(Filter.MortgageType.YOUNG_FAMILY))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeTarget() {
        val filter = getFilter(mortgageType = listOf(Filter.MortgageType.TARGET))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeStateSupport() {
        val filter = getFilter(mortgageType = listOf(Filter.MortgageType.STATE_SUPPORT))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeStandard() {
        val filter = getFilter(mortgageType = listOf(Filter.MortgageType.STANDARD))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeMilitary() {
        val filter = getFilter(mortgageType = listOf(Filter.MortgageType.MILITARY))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeEastern() {
        val filter = getFilter(mortgageType = listOf(Filter.MortgageType.EASTERN))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMortgageTypeAll() {
        val filter = getFilter(mortgageType = Filter.MortgageType.values().toList())
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountBorrowerCategoryBusinessOwner() {
        val filter = getFilter(borrowerCategory = listOf(Filter.BorrowerCategory.BUSINESS_OWNER))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountBorrowerCategoryEmployee() {
        val filter = getFilter(borrowerCategory = listOf(Filter.BorrowerCategory.EMPLOYEE))
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountBorrowerCategoryIndividualEnterpreneur() {
        val filter = getFilter(
            borrowerCategory = listOf(Filter.BorrowerCategory.INDIVIDUAL_ENTREPRENEUR)
        )
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountBorrowerCategoryAll() {
        val filter = getFilter(borrowerCategory = Filter.BorrowerCategory.values().toList())
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountIncomeConfirmationBankReference() {
        val filter = getFilter(incomeConfirmation = Filter.IncomeConfirmation.BANK_REFERENCE)
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountIncomeConfirmationPfr() {
        val filter = getFilter(incomeConfirmation = Filter.IncomeConfirmation.PFR)
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountIncomeConfirmation2Ndfl() {
        val filter = getFilter(incomeConfirmation = Filter.IncomeConfirmation.REFERENCE_2NDFL)
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountIncomeConfirmationWithoutProof() {
        val filter = getFilter(incomeConfirmation = Filter.IncomeConfirmation.WITHOUT_PROOF)
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountMaternityFunds() {
        val filter = getFilter(maternityFunds = Unit)
        assertEquals(1, filter.countFormHiddenParams())
    }

    @Test
    fun testCountFullyPropagatedFilter() {
        val filter = getFilter(
            rate = Range.valueOf(2, 42),
            mortgageType = listOf(Filter.MortgageType.YOUNG_FAMILY),
            borrowerCategory = listOf(Filter.BorrowerCategory.BUSINESS_OWNER),
            incomeConfirmation = Filter.IncomeConfirmation.BANK_REFERENCE,
            maternityFunds = Unit
        )
        assertEquals(5, filter.countFormHiddenParams())
    }

    private fun getFilter(
        flatType: Filter.FlatType = Filter.FlatType.ANY,
        rate: Range<Long>? = null,
        mortgageType: List<Filter.MortgageType>? = null,
        borrowerCategory: List<Filter.BorrowerCategory>? = null,
        incomeConfirmation: Filter.IncomeConfirmation? = null,
        maternityFunds: Unit? = null
    ): Filter {
        return Filter(
            region = MortgageRegion(GeoRegion.DEFAULT, 0),
            price = 1,
            initialPayment = 1,
            period = 1,
            flatType = flatType,
            rate = rate,
            mortgageType = mortgageType,
            borrowerCategory = borrowerCategory,
            incomeConfirmation = incomeConfirmation,
            maternityFunds = maternityFunds
        )
    }
}
