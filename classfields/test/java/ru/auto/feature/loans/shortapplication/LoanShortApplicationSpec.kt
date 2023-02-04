package ru.auto.feature.loans.shortapplication

import io.kotest.assertions.asClue
import io.kotest.matchers.be
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.qameta.allure.kotlin.Allure.step
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.User
import ru.auto.data.model.dadata.Suggest
import ru.auto.data.util.Try
import ru.auto.data.util.firstInstance
import ru.auto.data.util.firstInstanceOrNull
import ru.auto.feature.loans.calculator.LoanCalculator
import ru.auto.feature.loans.common.model.LoanProfileParameters
import ru.auto.test.runner.AllureRobolectricRunner
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testdata.CALCULATOR_PARAMS_GENERIC
import ru.auto.testdata.CREDIT_PRODUCT_GENERIC
import ru.auto.testdata.USER_GENERIC
import ru.auto.testextension.shouldContainMatching

@RunWith(AllureRobolectricRunner::class)
class LoanShortApplicationSpec {

    val boilerplateState = LoanShortApplication.initialState(
        calculatorState = LoanCalculator.initialStateFromInitiator(LoanCalculator.Initiator(CALCULATOR_PARAMS_GENERIC)),
        creditProducts = listOf(CREDIT_PRODUCT_GENERIC)
    )

    @Test
    fun `should send application with suggested fio if user selected some fio suggest`() {
        val feature = prepareFeature()
        step("Select suggest") {
            feature.accept(LoanShortApplication.Msg.OnSuggestSelected(parameter("suggest", FIO_SUGGEST)))
        }
        step("it should send application with suggested fio") {
            feature.accept(LoanShortApplication.Msg.OnSendShortApplicationClick)
            feature.latestEffects shouldContainMatching beInstanceOf<LoanShortApplication.Eff.ProceedWithApplication>()
            feature.latestEffects.firstInstance<LoanShortApplication.Eff.ProceedWithApplication>().asClue { eff ->
                eff.application.personProfile?.name.shouldNotBeNull().asClue { nameEntity ->
                    nameEntity.name shouldBe FIO_SUGGEST.data.name
                    nameEntity.surname shouldBe FIO_SUGGEST.data.surname
                    nameEntity.patronymic shouldBe FIO_SUGGEST.data.patronymic
                }
            }
        }
    }

    @Test
    fun `should send application with verified fio`() {
        val feature = prepareFeature()
        step("user not selected any suggests") {
            feature.accept(LoanShortApplication.Msg.OnSendShortApplicationClick)
            step("it should send verification request") {
                feature.latestEffects shouldContainMatching be(LoanShortApplication.Eff.VerifyFio(FIO_INPUT))
            }
            step("when got successful verification response") {
                feature.accept(LoanShortApplication.Msg.OnVerifyResponse(Try.Success(FIO_SUGGEST)))
            }
            step("it should send application with verified fio") {
                feature.latestEffects.firstInstanceOrNull<LoanShortApplication.Eff.ProceedWithApplication>()
                    ?.application
                    ?.personProfile
                    ?.name
                    .shouldNotBeNull().asClue {
                        it.name shouldBe FIO_SUGGEST.data.name
                        it.surname shouldBe FIO_SUGGEST.data.surname
                        it.patronymic shouldBe FIO_SUGGEST.data.patronymic
                    }
            }
        }
    }

    @Test
    fun `should send application with unverified fio`() {
        val feature = prepareFeature()
        step("user not selected any suggests") {
            feature.accept(LoanShortApplication.Msg.OnSendShortApplicationClick)
            step("it should send verification request") {
                feature.latestEffects shouldContainMatching be(LoanShortApplication.Eff.VerifyFio(FIO_INPUT))
            }
            step("when got unsuccessful verification response") {
                feature.accept(LoanShortApplication.Msg.OnVerifyResponse(Try.Error(Throwable())))
            }
            step("it should send application with user input if verification fails") {
                feature.latestEffects.firstInstanceOrNull<LoanShortApplication.Eff.ProceedWithApplication>()
                    ?.application
                    ?.personProfile
                    ?.name
                    .shouldNotBeNull().asClue {
                        it.name shouldBe "Тестов"
                        it.surname shouldBe "Тестик"
                        it.patronymic shouldBe "Тестович"
                    }
            }
        }
    }


    private fun prepareFeature(): TeaTestFeature<LoanShortApplication.Msg, LoanShortApplication.State, LoanShortApplication.Eff> {
        val initialState = boilerplateState.copy(loanProfileParameters = LoanProfileParameters.fromUser(USER_GENERIC))
        val feature = createFeature(initialState, USER_GENERIC)
        feature.accept(LoanShortApplication.Msg.OnFioInput(FIO_INPUT))
        return feature
    }

    companion object {
        private const val FIO_INPUT = "Тестик Тестов Тестович"
        private const val FIO_VALUE = "Тестов Тест Тестович"
        private val FIO_SUGGEST = Suggest(
            value = FIO_VALUE,
            unrestrictedValue = FIO_VALUE,
            data = Suggest.Data(
                "Тестов",
                "Тест",
                "Тестович",
                Suggest.Data.Gender.UNKNOWN,
                Suggest.Data.QCDetect.KNOWN
            )
        )

        private fun createFeature(state: LoanShortApplication.State, user: User?) =
            TeaTestFeature(initialState = state) { msg: LoanShortApplication.Msg, state: LoanShortApplication.State ->
                LoanShortApplication.reducer(msg = msg, state = state, user = user)
            }
    }
}
