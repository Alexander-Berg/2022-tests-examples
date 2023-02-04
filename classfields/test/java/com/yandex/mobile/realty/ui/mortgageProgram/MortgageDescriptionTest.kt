package com.yandex.mobile.realty.ui.mortgageProgram

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.mortgageprogram.model.Bank
import com.yandex.mobile.realty.domain.mortgageprogram.model.FlatType
import com.yandex.mobile.realty.domain.mortgageprogram.model.HousingType
import com.yandex.mobile.realty.domain.mortgageprogram.model.MortgageProgram
import com.yandex.mobile.realty.domain.mortgageprogram.model.MortgageProgramType
import com.yandex.mobile.realty.ui.mortgageprogram.presenter.description
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author sorokinandrei on 6/22/21.
 */
@RunWith(RobolectricTestRunner::class)
class MortgageDescriptionTest : RobolectricTest() {

    @Test
    fun shouldFormatNewFlat() {
        checkDescription(
            setOf(FlatType.NEW_FLAT),
            setOf(HousingType.FLAT),
            "на квартиры в новостройках"
        )
    }

    @Test
    fun shouldFormatSecondaryFlat() {
        checkDescription(
            setOf(FlatType.SECONDARY),
            setOf(HousingType.FLAT),
            "на квартиры на вторичном рынке"
        )
    }

    @Test
    fun shouldFormatNewFlatAndApartment() {
        checkDescription(
            setOf(FlatType.NEW_FLAT),
            setOf(HousingType.FLAT, HousingType.APARTMENT),
            "на квартиры и апартаменты в новостройках"
        )
    }

    @Test
    fun shouldFormatSecondaryFlatAndApartment() {
        checkDescription(
            setOf(FlatType.SECONDARY),
            setOf(HousingType.FLAT, HousingType.APARTMENT),
            "на квартиры и апартаменты на вторичном рынке"
        )
    }

    @Test
    fun shouldFormatNewApartment() {
        checkDescription(
            setOf(FlatType.NEW_FLAT),
            setOf(HousingType.APARTMENT),
            "на апартаменты в новостройках"
        )
    }

    @Test
    fun shouldFormatSecondaryApartment() {
        checkDescription(
            setOf(FlatType.SECONDARY),
            setOf(HousingType.APARTMENT),
            "на апартаменты на вторичном рынке"
        )
    }

    @Test
    fun shouldFormatAnyApartment() {
        checkDescription(
            setOf(FlatType.NEW_FLAT, FlatType.SECONDARY),
            setOf(HousingType.APARTMENT),
            "на апартаменты в новостройках и на вторичном рынке"
        )
    }

    @Test
    fun shouldFormatAnyFlat() {
        checkDescription(
            setOf(FlatType.NEW_FLAT, FlatType.SECONDARY),
            setOf(HousingType.FLAT),
            "на квартиры в новостройках и на вторичном рынке"
        )
    }

    @Test
    fun shouldFormatAny() {
        checkDescription(
            setOf(FlatType.NEW_FLAT, FlatType.SECONDARY),
            setOf(HousingType.FLAT, HousingType.APARTMENT),
            "на квартиры и апартаменты в новостройках и на вторичном рынке"
        )
    }

    private fun checkDescription(
        flatType: Set<FlatType>,
        housingType: Set<HousingType>,
        expected: String
    ) {
        val program = createProgram(flatType, housingType)
        val actual = program.description
        assertEquals(expected, actual.substringAfter("Ипотечная программа "))
    }

    private fun createProgram(
        flatType: Set<FlatType>,
        housingType: Set<HousingType>
    ): MortgageProgram {
        return MortgageProgram(
            id = "1",
            bank = createBank(),
            name = "Ипотека на строящееся жильё",
            type = MortgageProgramType.STANDARD,
            rate = Range.LowerBound(8.29),
            rateDescription = null,
            rateWithDiscount = null,
            initialPayment = Range.LowerBound(10),
            amount = Range.valueOf(600_000, 50_000_000),
            period = Range.valueOf(3, 30),
            monthPayment = 19_936,
            age = null,
            totalExperience = null,
            lastExperience = null,
            flatType = flatType,
            integrationType = null,
            discountRate = null,
            specialCondition = null,
            increasingFactor = emptyList(),
            reducingFactor = emptyList(),
            housingType = housingType,
            incomeConfirmation = emptySet(),
            borrowerCategory = emptySet(),
            documents = emptyList(),
            requirements = emptyList(),
            citizenship = emptySet(),
            additionalDescription = emptyList(),
            trackingUrl = null,
        )
    }

    private fun createBank(): Bank {
        return Bank(
            id = "1",
            name = "Банк",
            genitiveName = null,
            logo = null,
            darkLogo = null,
            legalName = null,
            licenseNumber = null,
            licenseDate = null,
            headOfficeAddress = null,
            policyUrl = null,
        )
    }
}
