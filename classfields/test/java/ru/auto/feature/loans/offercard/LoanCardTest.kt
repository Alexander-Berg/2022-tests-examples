package ru.auto.feature.loans.offercard

import android.graphics.Color
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.AutoruUserProfile
import ru.auto.data.model.User
import ru.auto.data.model.UserProfile
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.dadata.Suggest
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.PriceInfo
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.data.offer.SharkInfo
import ru.auto.data.util.LoadableData
import ru.auto.data.util.Try
import ru.auto.feature.loans.common.model.AmountRange
import ru.auto.feature.loans.common.model.Bank
import ru.auto.feature.loans.common.model.CreditProduct
import ru.auto.feature.loans.common.model.CreditProductType
import ru.auto.feature.loans.common.model.InterestRange
import ru.auto.feature.loans.common.model.LoanCalculatorParams
import ru.auto.feature.loans.common.model.PeriodRange
import ru.auto.feature.loans.common.presentation.AutoruPayload
import ru.auto.feature.loans.common.presentation.CreditApplication
import ru.auto.feature.loans.common.presentation.CreditApplicationState
import ru.auto.feature.loans.common.presentation.EmailEntity
import ru.auto.feature.loans.common.presentation.NameEntity
import ru.auto.feature.loans.common.presentation.PersonProfile
import ru.auto.feature.loans.common.presentation.PhoneEntity
import ru.auto.feature.loans.common.presentation.Requirements
import ru.auto.feature.loans.shortapplication.LoanShortApplication
import ru.auto.test.runner.AllureRobolectricRunner
import ru.auto.test.tea.TeaTestFeature

@RunWith(AllureRobolectricRunner::class)
class LoanCardTest {

    val initialState = LoanCard.initialState()

    @Test
    fun `should show loan short application when no user`() {
        val feature = createFeature(initialState)
        feature.initWithOfferAndUserAndProduct()

        feature.currentState.loanShortApplicationState shouldNotBe null
    }

    @Test
    fun `should open fullform when found application in draft after login`() {
        val feature = createFeature(initialState)
        feature.initWithOfferAndUserAndProduct()
        feature.typeInPersonalDataAndSendApplication()
        feature.accept(LoanCard.Msg.OnUserLoaded(someUser))
        feature.accept(LoanCard.Msg.OnLoanApplicationResult(Try.Success(draftApplication)))

        feature.latestEffects shouldContain LoanCard.Eff.EditPersonProfile(draftApplication)
    }

    @Test
    fun `should show active application when found after login`() {
        val feature = createFeature(initialState)
        feature.initWithOfferAndUserAndProduct()
        feature.typeInPersonalDataAndSendApplication()
        feature.accept(LoanCard.Msg.OnUserLoaded(someUser))
        feature.accept(LoanCard.Msg.OnLoanApplicationResult(Try.Success(activeApplication)))

        feature.currentState.loanApplication shouldBe LoadableData.Success(activeApplication)
    }

    @Test
    fun `should show wizard when no loan application`() {
        val feature = createFeature(initialState)
        feature.initWithOfferAndUserAndProduct()
        feature.typeInPersonalDataAndSendApplication()
        feature.accept(LoanCard.Msg.OnUserLoaded(someUser))
        feature.accept(LoanCard.Msg.OnLoanApplicationResult(Try.Success(null)))

        feature.latestEffects shouldContain LoanCard.Eff.LoanShortApplicationEff(
            LoanShortApplication.Eff.ProceedWithApplication(
                CreditApplication(
                    requirements = requirements,
                    personProfile = PersonProfile(
                        name = NameEntity.fromFioString(FIO),
                        emails = listOf(EmailEntity(email = EMAIL)),
                        phones = listOf(PhoneEntity(phone = NORMALIZED_PHONE))
                    ),
                    state = CreditApplicationState.DRAFT,
                    payload = AutoruPayload(offer)
                ),
                isExclusive = false
            )
        )
    }

    @Test
    fun `should not try to load credit application if user is dealer`() {
        val feature = createFeature(initialState)
        feature.initWithOfferAndUserAndProduct(user = dealerUser)

        feature.latestEffects shouldNotContain LoanCard.Eff.LoadApplicationState
    }

    @Test
    fun `should not edit person profile when found draft after login`() {
        val feature = createFeature(initialState)
        feature.initWithOfferAndUserAndProduct(user = someUser)
        feature.accept(LoanCard.Msg.OnLoanApplicationResult(Try.Success(draftApplication)))

        feature.latestEffects shouldNotContain LoanCard.Eff.EditPersonProfile(draftApplication)
    }


    companion object {
        private const val FIO = "поломецкая марья васильевна"
        private val FIO_SUGGEST = Suggest(
            value = FIO,
            unrestrictedValue = FIO,
            data = Suggest.Data(
                "поломецкая",
                "марья",
                "васильевна",
                Suggest.Data.Gender.UNKNOWN,
                Suggest.Data.QCDetect.KNOWN
            )
        )
        private const val EMAIL = "test@test.com"
        private const val PHONE = "+7(000)0000000"
        private const val NORMALIZED_PHONE = "70000000000"
        private val someUser = User.Authorized(
            id = "",
            userProfile = UserProfile(autoruUserProfile = null)
        )
        private val dealerUser = User.Authorized(
            id = "",
            userProfile = UserProfile(autoruUserProfile = AutoruUserProfile(clientId = ""))
        )
        private val draftApplication = CreditApplication(
            id = "application",
            state = CreditApplicationState.DRAFT
        )
        private val activeApplication = CreditApplication(
            id = "application",
            state = CreditApplicationState.ACTIVE
        )
        private val offer = Offer(
            category = VehicleCategory.CARS,
            id = "",
            sellerType = SellerType.PRIVATE,
            priceInfo = PriceInfo(1000_000),
            sharkInfo = SharkInfo(
                suitableCreditProductIds = listOf("productId"),
                precondition = SharkInfo.Precondition(0, 0.1, 12, 12000)
            )
        )
        private val creditProduct = CreditProduct(
            id = "productId",
            bank = Bank(
                id = "tinkoff",
                bankName = "",
                bankLogoUrl = "",
                bankDarkLogoUrl = "",
                bankLogoSmall = "",
                bankDarkLogoSmall = "",
                bankLogoRound = "",
                bankColor = Color.WHITE
            ),
            amountRange = AmountRange(0L, 1000L),
            interestRange = InterestRange(from = 0.0, to = 0.0),
            downPaymentRate = 0.0,
            productType = CreditProductType.CONSUMER,
            periodRange = PeriodRange(12, 36),
            isActive = true
        )
        private val calculatorParams = LoanCalculatorParams(
            amountRange = AmountRange(0L, 1000000L),
            interestRange = InterestRange(from = 0.0, to = 0.0),
            downPaymentRate = 0.0,
            periodRange = PeriodRange(12, 36),
        )
        private val requirements = Requirements(
            amount = 1000_000,
            termMonths = 36,
            initialFee = 0,
            geoBaseId = null
        )

        private fun createFeature(state: LoanCard.State) = TeaTestFeature(state, LoanCard::reducer)

        private fun LoanCardFeature.initWithOfferAndUserAndProduct(user: User = User.Unauthorized) {
            accept(LoanCard.Msg.OnOfferLoaded(offer))
            accept(LoanCard.Msg.OnUserLoaded(user = user))
            accept(LoanCard.Msg.OnCreditProductsAndCalculatorParamsLoaded(listOf(creditProduct), calculatorParams))
        }

        private fun LoanCardFeature.typeInPersonalDataAndSendApplication() {
            listOf(
                LoanShortApplication.Msg.OnFioInput(FIO),
                LoanShortApplication.Msg.OnSuggestSelected(FIO_SUGGEST),
                LoanShortApplication.Msg.OnEmailInput(EMAIL),
                LoanShortApplication.Msg.OnPhoneInput(PHONE),
                LoanShortApplication.Msg.OnSendShortApplicationClick,
            ).map(LoanCard.Msg::OnLoanShortApplicationMsg)
                .forEach(this::accept)
        }
    }
}
