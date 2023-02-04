package ru.auto.feature.loans.calculator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import ru.auto.testdata.CALCULATOR_PARAMS_GENERIC


class LoanCalculatorTest : FreeSpec({
    val calculatorParams = CALCULATOR_PARAMS_GENERIC
    val minAmount = calculatorParams.amountRange.from
    val maxAmount = calculatorParams.amountRange.to
    val interestRange = calculatorParams.interestRange
    "describe Credit param exp active" - {
        "describe inited without offer" - {
            val state = LoanCalculator.initialStateFromInitiator(LoanCalculator.Initiator(calculatorParams))

            "context OnLoanAmountInput" - {
                "should change loan amount literally when it is in amountRange" {
                    val loanAmount = 625_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, state)
                    newState.loanParameters.loanAmount shouldBe loanAmount
                }
                "should change loan amount to min amount when it is below amountRange" {
                    val loanAmount = 25_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, state)
                    newState.loanParameters.loanAmount shouldBe minAmount
                }
                "should change loan amount to max amount when it is above amountRange" {
                    val loanAmount = 2_500_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, state)
                    newState.loanParameters.loanAmount shouldBe maxAmount
                }
                "should change downPayment amount if it is below downPaymentRate" {
                    val loanAmount = 2_000_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, state)
                    newState.loanParameters.downPayment shouldBe 200_000L
                }
            }
        }

        "describe inited with offer price" - {
            val state = LoanCalculator.initialStateFromInitiator(
                LoanCalculator.Initiator(calculatorParams = calculatorParams, offerPrice = 1_000_000L)
            )


            "context OnLoanAmountInput" - {

                "should change loan amount and downpayment" {
                    val loanAmount = 700_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, state)
                    newState.loanParameters.loanAmount shouldBe loanAmount
                    newState.loanParameters.downPayment shouldBe 300_000
                }
                "should change loan amount to offer price minus downPaymentRate" {
                    val loanAmount = 1000_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, state)
                    newState.loanParameters.loanAmount shouldBe 909_100L
                    newState.loanParameters.downPayment shouldBe 90_950
                }
                "should change loan amount to max loan amount" {
                    val specificState = state.copy(desiredTotalAmount = 3000_000)
                    val loanAmount = 2500_000L
                    val msg = LoanCalculator.Msg.OnLoanAmountInput(loanAmount, loanAmount.toString())
                    val (newState, _) = LoanCalculator.reducer(msg, specificState)
                    newState.loanParameters.loanAmount shouldBe maxAmount
                    newState.loanParameters.downPayment shouldBe 1000_000
                }
            }
        }
    }
})
