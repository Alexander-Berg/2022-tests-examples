package ru.auto.feature.loans.domain

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.util.Clock
import ru.auto.data.model.credit.Bank
import ru.auto.data.model.credit.CreditClaim
import ru.auto.data.model.credit.UpdateStatusType
import ru.auto.data.model.data.offer.PriceInfo
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.network.scala.offer.NWSalon
import ru.auto.data.model.network.scala.offer.SalonConverter
import ru.auto.data.model.network.scala.offer.converter.OfferConverter
import ru.auto.data.network.scala.response.OfferResponse
import ru.auto.data.util.getRuLocale
import ru.auto.testextension.FileTestUtils
import java.text.SimpleDateFormat

@RunWith(AllureRunner::class) class LoanInteractorSelectBankTest {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", getRuLocale())
    private val updateDate = dateFormat.parse("2020-02-27")
    private val nextDayDate = dateFormat.parse("2020-02-28")
    private val nextWeekDate = dateFormat.parse("2020-03-07")
    private val claimTemplate = CreditClaim(
        id = "id12345",
        status = null,
        autoCredits = emptyList(),
        loanAmount = 12400000,
        loanRate = 7.7,
        loanPeriod = 5,
        monthlyPayment = 39000,
        updateDate = updateDate,
        createDate = updateDate
    )
    private val baseOffer = FileTestUtils.readJsonAsset(
        assetPath = "/assets/10448426-ce654669.json",
        classOfT = OfferResponse::class.java
    ).offer?.let { OfferConverter().fromNetwork(it, searchPosition = 0) }!!

    private val offerMoreThanMilPrivate = baseOffer.copy(
        priceInfo = PriceInfo(
            price = 1_500_000,
            priceRUR = 1_500_000f
        ),
        salon = null,
        sellerType = SellerType.PRIVATE
    )
    private val offerMoreThanMilCommercial = baseOffer.copy(
        priceInfo = PriceInfo(
            price = 1_500_000,
            priceRUR = 1_500_000f
        ),
        salon = SalonConverter.fromNetwork(NWSalon(dealer_id = "1234", is_oficial = false)),
        sellerType = SellerType.COMMERCIAL
    )
    private val offerLessThanMilPrivate = baseOffer.copy(
        priceInfo = PriceInfo(
            price = 900_000,
            priceRUR = 900_000f
        ),
        salon = null,
        sellerType = SellerType.PRIVATE
    )
    private val offerLessThanMilCommercial = baseOffer.copy(
        priceInfo = PriceInfo(
            price = 900_000,
            priceRUR = 900_000f
        ),
        salon = SalonConverter.fromNetwork(NWSalon(dealer_id = "1234", is_oficial = false)),
        sellerType = SellerType.COMMERCIAL
    )

    @Before
    fun setup() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = updateDate.time
        }
    }

    @Test
    fun `if no tinkoff claim and offer more that million and private should return ALFA`() {
        assertThat(LoanInteractor.selectBank(
            claims = emptyList(),
            experimentBank = Bank.SRAVNI,
            fallbackBank = Bank.ALFA,
            offer = offerMoreThanMilPrivate,
            experimentCommercialBank = Bank.ALFA
        )).isEqualTo(Bank.ALFA)
    }

    @Test
    fun `if no tinkoff claim and offer more that million and commercial should return ALFA`() {
        assertThat(
            LoanInteractor.selectBank(
                claims = emptyList(),
                experimentBank = Bank.SRAVNI,
                fallbackBank = Bank.ALFA,
                offer = offerMoreThanMilCommercial,
                experimentCommercialBank = Bank.SRAVNI
            )
        ).isEqualTo(Bank.ALFA)
    }

    @Test
    fun `if no tinkoff claim and offer less than million and private should return private exp bank`() {
        assertThat(LoanInteractor.selectBank(
            claims = emptyList(),
            experimentBank = Bank.SRAVNI,
            fallbackBank = Bank.ALFA,
            offer = offerLessThanMilPrivate,
            experimentCommercialBank = Bank.ALFA
        )).isEqualTo(Bank.SRAVNI)
    }

    @Test
    fun `if no tinkoff claim and offer less than million and commercial should return commercial exp bank for ALFA`() {
        assertThat(LoanInteractor.selectBank(
            claims = emptyList(),
            experimentBank = Bank.SRAVNI,
            fallbackBank = Bank.ALFA,
            offer = offerLessThanMilCommercial,
            experimentCommercialBank = Bank.ALFA
        )).isEqualTo(Bank.ALFA)
    }

    @Test
    fun `if no tinkoff claim and offer less than million and commercial should return commercial exp bank for SRAVNI`() {
        assertThat(LoanInteractor.selectBank(
            claims = emptyList(),
            experimentBank = Bank.SRAVNI,
            fallbackBank = Bank.ALFA,
            offer = offerLessThanMilCommercial,
            experimentCommercialBank = Bank.SRAVNI
        )).isEqualTo(Bank.SRAVNI)
    }

    @Test
    fun `if tinkoff claim in terminal status should return TINKOFF`() {
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.SECOND_AGREEMENT))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.TINKOFF)
    }

    @Test
    fun `if tinkoff claim in terminal status and more than 51 hours passed should return TINKOFF`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextWeekDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.SECOND_AGREEMENT))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.TINKOFF)
    }

    @Test
    fun `if tinkoff claim in immediate fallback status should return experiment`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextDayDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.REJECT))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.SRAVNI)
    }

    @Test
    fun `if tinkoff claim in immediate fallback status and no experiment bank should return fallback`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextDayDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.REJECT))
        assertThat(LoanInteractor.selectBank(claims, Bank.TINKOFF, Bank.ALFA)).isEqualTo(Bank.ALFA)
    }

    @Test
    fun `if tinkoff claim in postpone fallback status and less than 51 hours passed should return TINKOFF`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextDayDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.CLIENT_VERIFICATION))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.TINKOFF)
    }

    @Test
    fun `if tinkoff claim in postpone fallback status and more than 51 hours passed should return fallback`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextWeekDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.CLIENT_VERIFICATION))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.ALFA)
    }

    @Test
    fun `if claim in not forced exp statuses should return experiment bank`() {
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.UPDATE_STATUS_UNKNOWN))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.SRAVNI)
    }

    @Test
    fun `if claim in draft should return experiment bank`() {
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.DRAFT))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.SRAVNI)
    }

    @Test
    fun `if claim in hold should return experiment bank`() {
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.HOLD))
        assertThat(LoanInteractor.selectBank(claims, Bank.SRAVNI, Bank.ALFA)).isEqualTo(Bank.SRAVNI)
    }

    @Test
    fun `if claim in draft and no experiment bank and passed less than 51 hours should return TINKOFF`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextDayDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.DRAFT))
        assertThat(LoanInteractor.selectBank(claims, Bank.TINKOFF, Bank.ALFA)).isEqualTo(Bank.TINKOFF)
    }

    @Test
    fun `if claim in hold and no experiment bank and passed less than 51 hours should return TINKOFF`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextDayDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.HOLD))
        assertThat(LoanInteractor.selectBank(claims, Bank.TINKOFF, Bank.ALFA)).isEqualTo(Bank.TINKOFF)
    }

    @Test
    fun `if claim in draft and no experiment bank and more than 51 hours passed should return fallback bank`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextWeekDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.DRAFT))
        assertThat(LoanInteractor.selectBank(claims, Bank.TINKOFF, Bank.ALFA)).isEqualTo(Bank.ALFA)
    }

    @Test
    fun `if claim in hold and no experiment bank and more than 51 hours passed should return fallback bank`() {
        Clock.impl = object : Clock {
            override fun nowMillis(): Long = nextWeekDate.time
        }
        val claims = listOf(claimTemplate.copy(status = UpdateStatusType.HOLD))
        assertThat(LoanInteractor.selectBank(claims, Bank.TINKOFF, Bank.ALFA)).isEqualTo(Bank.ALFA)
    }

    @After
    fun tearDown() {
        Clock.impl = Clock.System
    }

}
