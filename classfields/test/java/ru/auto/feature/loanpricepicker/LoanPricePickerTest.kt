package ru.auto.feature.loanpricepicker

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ru.auto.data.model.data.offer.CreditGroup
import ru.auto.data.util.LoadableData
import ru.auto.data.util.Try
import ru.auto.data.util.maybeValue
import ru.auto.feature.loanpricepicker.LoanPricePicker.Thumb
import ru.auto.feature.loanpricepicker.model.LoanPricePickerModel
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testdata.CALCULATOR_PARAMS_GENERIC
import ru.auto.feature.loanpricepicker.LoanPricePicker.Context as LoanPricePickerContext

class LoanPricePickerTest : FreeSpec({
    val calculatorParams = CALCULATOR_PARAMS_GENERIC
    val defaultContext = LoanPricePickerContext(
        10_000L, 300_000_000L,
        LoanPricePickerModel(null, null, null),
        expandLoanEvenIfEmpty = false
    )
    val openEffs = setOf(LoanPricePicker.Eff.LogLoanCalculatorToggle(becomeEnabled = true), LoanPricePicker.Eff.LogShowLoanAd)
    "describe init calculator when credit calculatorParams is ready" - {
        val defaultInitialState =
            LoanPricePicker.initialState(defaultContext).copy(calculatorParams = LoadableData.Success(calculatorParams))
        "context user did not input any price" - {
            val initialState = defaultInitialState
            val (state, eff) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnToggleLoanCalculator, initialState)
            "it triggers only logging effs" {
                eff shouldContainExactlyInAnyOrder openEffs
            }
            "it inits price from as minimal amount" {
                state.priceFrom shouldBe calculatorParams.amountRange.from
            }
            "it inits price to as maximum amount" {
                state.priceTo shouldBe calculatorParams.amountRange.to
            }
            "it inits downpayment as 0" {
                state.loanState.maybeValue?.initialFee shouldBe 0
            }
            "it inits maximum monthly payment by maximum amount allowed" {
                state.loanState.maybeValue?.paymentTo shouldBe 72_000L
            }
            "it inits minimum monthly payment by minimum price" {
                state.loanState.maybeValue?.paymentFrom shouldBe 3600L
            }
            "it inits term months as maximum term allowed by calculatorParams" {
                state.loanState.maybeValue?.loanTerm shouldBe calculatorParams.periodRange.to
            }
        }
        "context user did input some price in loan amount range" - {
            val initialState = defaultInitialState.copy(priceFrom = 150_000L, priceTo = 1_000_000L)
            val (state, eff) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnToggleLoanCalculator, initialState)
            "it triggers only logging effs" {
                eff shouldContainExactlyInAnyOrder openEffs
            }
            "it inits downpayment as 0" {
                state.loanState.maybeValue?.initialFee shouldBe 0
            }
            "it inits maximum monthly payment by max price entered" {
                state.loanState.maybeValue?.paymentTo shouldBe 36_000L
            }
            "it inits minimum monthly payment by min price entered" {
                state.loanState.maybeValue?.paymentFrom shouldBe 5_400L
            }
        }
        "context user input some price outside amount range and difference between price from and to is more than max amount" - {
            val extraMoney = 1_000_000L
            val initialState =
                defaultInitialState.copy(priceFrom = 150_000L, priceTo = calculatorParams.amountRange.to + extraMoney)
            val (state, eff) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnToggleLoanCalculator, initialState)
            "it triggers only logging effs" {
                eff shouldContainExactlyInAnyOrder openEffs
            }
            "it inits downpayment as diff between price to and max loan amount" {
                state.loanState.maybeValue?.initialFee shouldBe extraMoney
            }
            "it inits monthly payment by max price minus downpayment" {
                state.loanState.maybeValue?.paymentTo shouldBe 72_000L
            }
            "it inits minimum monthly payment to zero because it less than downpayment" {
                state.loanState.maybeValue?.paymentFrom shouldBe 0L
            }
            "it does not change price" {
                state.priceFrom shouldBe initialState.priceFrom
            }
        }
        "context user did input price outside amount range and diff between price from and to is less than max amount" - {
            val extraMoney = 1_000_000L
            val initialState =
                defaultInitialState.copy(priceFrom = 150_000L + extraMoney,
                    priceTo = calculatorParams.amountRange.to + extraMoney)
            val (state, _) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnToggleLoanCalculator, initialState)
            "it inits minimum monthly payment by min price entered" {
                state.loanState.maybeValue?.paymentFrom shouldBe 5_400L
            }
        }
    }
    "describe price picker when credit button is turned off" - {
        "context initial price range" - {
            "it should init as min and max if no previous value" {
                val initialState = LoanPricePicker.initialState(defaultContext)
                initialState.priceFrom shouldBe defaultContext.minPrice
                initialState.priceTo shouldBe defaultContext.maxPrice
            }
            "it should init as previous value if exists" {
                val priceFrom = 150_000L
                val priceTo = 250_000L
                val initialState = LoanPricePicker.initialState(
                    defaultContext.copy(
                        previousPricePickerModel = LoanPricePickerModel(
                            priceFrom,
                            priceTo,
                            null
                        )
                    )
                )
                initialState.priceFrom shouldBe priceFrom
                initialState.priceTo shouldBe priceTo
            }
        }
        "context recalculating price within range" - {
            val priceFrom = 150_000L
            val priceTo = 250_000L
            val initialState = LoanPricePicker.initialState(
                defaultContext.copy(
                    previousPricePickerModel = LoanPricePickerModel(
                        priceFrom,
                        priceTo,
                        null
                    )
                )
            )
            "it should recalculate priceFrom within range if it is lower than min price" {
                val (state, eff) = LoanPricePicker.reduce(
                    LoanPricePicker.Msg.OnInputPrice(5000L, thumb = Thumb.LEFT),
                    initialState
                )
                state.priceFrom shouldBe defaultContext.minPrice
                state.priceTo shouldBe initialState.priceTo
            }
            "it should recalculate priceTo within range if it is higher than max price" {
                val (state, eff) = LoanPricePicker.reduce(
                    LoanPricePicker.Msg.OnInputPrice(350_000_000L, thumb = Thumb.RIGHT),
                    initialState
                )
                state.priceTo shouldBe defaultContext.maxPrice
                state.priceFrom shouldBe initialState.priceFrom
            }
            "it should make priceTo the same as priceFrom if new priceFrom is higher" {
                val newValue = 1_000_000L
                val (state, eff) = LoanPricePicker.reduce(
                    LoanPricePicker.Msg.OnInputPrice(newValue, thumb = Thumb.LEFT),
                    initialState
                )
                state.priceTo shouldBe newValue
                state.priceFrom shouldBe newValue
            }
            "it should make priceFrom the same as priceTo if new priceTo is lower" {
                val newValue = 100_000L
                val (state, eff) = LoanPricePicker.reduce(
                    LoanPricePicker.Msg.OnInputPrice(newValue, thumb = Thumb.RIGHT),
                    initialState
                )
                state.priceTo shouldBe newValue
                state.priceFrom shouldBe newValue
            }
        }
    }
    "describe recalculating loan parameters from price input" - {
        val (defaultInitialState, _) = LoanPricePicker.reduce(
            LoanPricePicker.Msg.OnToggleLoanCalculator,
            LoanPricePicker.initialState(defaultContext).copy(calculatorParams = LoadableData.Success(calculatorParams))
        )
        "context priceTo is less than loan amount" - {
            val initialState = defaultInitialState
            "it should recalculate only max monthly payment from priceTo input" - {
                val newPrice = 1_000_000L
                val (state, eff) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnInputPrice(newPrice, Thumb.RIGHT), initialState)
                "should be zero effects" {
                    eff shouldHaveSize 0
                }
                "should recalculate paymentTo from priceTo" {
                    state.priceTo shouldBe newPrice
                    state.loanState.maybeValue?.paymentTo shouldBe 36_000
                }
                "other parameters should remain unchanged" {
                    state.priceFrom shouldBe initialState.priceFrom
                    state.loanState.maybeValue?.initialFee shouldBe initialState.loanState.maybeValue?.initialFee
                    state.loanState.maybeValue?.paymentFrom shouldBe initialState.loanState.maybeValue?.paymentFrom
                    state.loanState.maybeValue?.loanTerm shouldBe initialState.loanState.maybeValue?.loanTerm
                }
            }
            "it should recalculate only min mothly payment from priceFrom input" - {
                val newPrice = 200_000L
                val (state, eff) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnInputPrice(newPrice, Thumb.LEFT), initialState)
                "should be zero effects" {
                    eff shouldHaveSize 0
                }
                "should recalculate paymentFrom from priceFrom" {
                    state.priceFrom shouldBe newPrice
                    state.loanState.maybeValue?.paymentFrom shouldBe 7_200
                }
                "other parameters should remain unchanged" {
                    state.priceTo shouldBe initialState.priceTo
                    state.loanState.maybeValue?.initialFee shouldBe initialState.loanState.maybeValue?.initialFee
                    state.loanState.maybeValue?.paymentTo shouldBe initialState.loanState.maybeValue?.paymentTo
                    state.loanState.maybeValue?.loanTerm shouldBe initialState.loanState.maybeValue?.loanTerm
                }
            }
            "it should add to downpayment what it cannot handle with amount" - {
                val newPriceFrom = 1100_000L
                val newPrice = 3_000_000L
                val feature = createFeature(initialState)
                feature.accept(LoanPricePicker.Msg.OnInputPrice(newPriceFrom, Thumb.LEFT))
                feature.accept(LoanPricePicker.Msg.OnInputPrice(newPrice, Thumb.RIGHT))
                val state = feature.currentState
                "should be zero effects" {
                    feature.latestEffects shouldHaveSize 0
                }
                "should recalculate paymentTo from priceTo" {
                    state.priceTo shouldBe newPrice
                    state.loanState.maybeValue?.paymentTo shouldBe 72_000
                }
                "should add extra to downpayment" {
                    state.loanState.maybeValue?.initialFee shouldBe 1000_000L
                }
                "should recalculate paymentFrom" {
                    state.loanState.maybeValue?.paymentFrom shouldBe 3_600
                }
                "other parameters should remain unchanged" {
                    state.priceFrom shouldBe newPriceFrom
                    state.loanState.maybeValue?.loanTerm shouldBe initialState.loanState.maybeValue?.loanTerm
                }
                "context decrease priceTo in loan amount range" - {
                    feature.accept(LoanPricePicker.Msg.OnInputPrice(1_900_000, Thumb.RIGHT))
                    "it should drop downpayment to zero" {
                        feature.currentState.loanState.maybeValue?.initialFee shouldBe 0
                    }
                }
            }
            "it should set paymentFrom to 0 if priceFrom is less than new downpayment" {
                val newPrice = 3_000_000L
                val (state, _) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnInputPrice(newPrice, Thumb.RIGHT), initialState)
                state.priceFrom shouldBeLessThanOrEqual checkNotNull(state.loanState.maybeValue?.initialFee)
                state.loanState.maybeValue?.paymentFrom shouldBe 0
            }
        }
    }
    "describe changing price from loan block" - {
        val (defaultInitialState, _) = LoanPricePicker.reduce(
            LoanPricePicker.Msg.OnToggleLoanCalculator,
            LoanPricePicker.initialState(defaultContext).copy(calculatorParams = LoadableData.Success(calculatorParams))
        )
        "context user changes paymentFrom" - {
            val newPaymentFrom = 10_000L
            val (state, eff) = LoanPricePicker.reduce(
                LoanPricePicker.Msg.OnInputMonthlyPayment(newPaymentFrom, Thumb.LEFT),
                defaultInitialState
            )
            "it should produce no effects" {
                eff shouldHaveSize 0
            }
            "it should change paymentFrom to desired value" {
                state.loanState.maybeValue?.paymentFrom shouldBe newPaymentFrom
            }
            "it should recalculate priceFrom" {
                state.priceFrom shouldBe 278_000
            }
            "other parameters should remain unchanged" {
                state.priceTo shouldBe defaultInitialState.priceTo
                state.loanState.maybeValue?.paymentTo shouldBe defaultInitialState.loanState.maybeValue?.paymentTo
                state.loanState.maybeValue?.initialFee shouldBe defaultInitialState.loanState.maybeValue?.initialFee
                state.loanState.maybeValue?.loanTerm shouldBe defaultInitialState.loanState.maybeValue?.loanTerm
            }
        }
        "context user changes paymentTo" - {
            val newPaymentTo = 70_000L
            val (state, eff) = LoanPricePicker.reduce(
                LoanPricePicker.Msg.OnInputMonthlyPayment(newPaymentTo, Thumb.RIGHT),
                defaultInitialState
            )
            "it should produce no effects" {
                eff shouldHaveSize 0
            }
            "it should change paymentTo to desired value" {
                state.loanState.maybeValue?.paymentTo shouldBe newPaymentTo
            }
            "it should recalculate priceTo" {
                state.priceTo shouldBe 1_946_000
            }
            "other parameters should remain unchanged" {
                state.priceFrom shouldBe defaultInitialState.priceFrom
                state.loanState.maybeValue?.paymentFrom shouldBe defaultInitialState.loanState.maybeValue?.paymentFrom
                state.loanState.maybeValue?.initialFee shouldBe defaultInitialState.loanState.maybeValue?.initialFee
                state.loanState.maybeValue?.loanTerm shouldBe defaultInitialState.loanState.maybeValue?.loanTerm
            }
        }
        "context user input downpayment" - {
            val newPayment = 1_000_000L
            val (state, eff) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnInputDownpayment(newPayment), defaultInitialState)
            "it should not have any effects" {
                eff shouldHaveSize 0
            }
            "it should set desired downpayment" {
                state.loanState.maybeValue?.initialFee shouldBe newPayment
            }
            "it should increase price by downpayment" {
                state.priceFrom shouldBe (defaultInitialState.priceFrom + newPayment)
                state.priceTo shouldBe (defaultInitialState.priceTo + newPayment)
            }
            "other parameters should remain unchanged" {
                state.loanState.maybeValue?.paymentTo shouldBe defaultInitialState.loanState.maybeValue?.paymentTo
                state.loanState.maybeValue?.paymentFrom shouldBe defaultInitialState.loanState.maybeValue?.paymentFrom
                state.loanState.maybeValue?.loanTerm shouldBe defaultInitialState.loanState.maybeValue?.loanTerm
            }
            "context user clears downpayment" - {
                val (newState, _) = LoanPricePicker.reduce(LoanPricePicker.Msg.OnInputDownpayment(0), state)
                "it should reverse to previous state" {
                    newState shouldBe defaultInitialState
                }
            }
        }
    }
    "describe changing loan period" - {
        val feature = createFeature(LoanPricePicker.initialState(defaultContext))
        feature.accept(LoanPricePicker.Msg.OnCalculatorParamsResult(Try.Success(calculatorParams)))
        feature.accept(LoanPricePicker.Msg.OnToggleLoanCalculator)
        "context decrease loan period when payments are in range" - {
            val months = 12
            feature.accept(LoanPricePicker.Msg.OnInputMonthlyPayment(36_000, Thumb.LEFT))
            val startingState = feature.currentState
            feature.accept(LoanPricePicker.Msg.OnInputLoanPeriod(months))
            "it should set loan period" {
                feature.currentState.loanState.maybeValue?.loanTerm shouldBe months
            }
            "priceFrom recalculated with the same monthly payment but different loan" {
                feature.currentState.priceFrom shouldBe 406_000
            }
            "priceTo recalculated with the same monthly payment but different loan" {
                feature.currentState.priceTo shouldBe 813_000
            }
            "other parameters should remain unchanged" {
                feature.currentState.loanState.maybeValue?.paymentFrom shouldBe startingState.loanState.maybeValue?.paymentFrom
                feature.currentState.loanState.maybeValue?.paymentTo shouldBe startingState.loanState.maybeValue?.paymentTo
                feature.currentState.loanState.maybeValue?.initialFee shouldBe startingState.loanState.maybeValue?.initialFee
            }
        }
        "context increase loan period when monthlyPaymentTo is out of range" - {
            val months = 32
            feature.accept(LoanPricePicker.Msg.OnInputMonthlyPayment(120_000, Thumb.RIGHT))
            val startingState = feature.currentState
            feature.accept(LoanPricePicker.Msg.OnInputLoanPeriod(months))
            "it should cap monthlyPaymentTo by the max possible payment" {
                feature.currentState.loanState.maybeValue?.paymentTo shouldBe 72_000
            }
            "priceTo recalculated with the new monthly payment and different period" {
                feature.currentState.priceTo shouldBe 2_000_000
            }
            "other parameters should remain unchanged" {
                feature.currentState.priceFrom shouldBe startingState.priceFrom
                feature.currentState.loanState.maybeValue?.paymentFrom shouldBe startingState.loanState.maybeValue?.paymentFrom
                feature.currentState.loanState.maybeValue?.initialFee shouldBe startingState.loanState.maybeValue?.initialFee
            }
        }
        "context decrease loan period when monthlyPaymentFrom is out of range" - {
            val months = 12
            feature.accept(LoanPricePicker.Msg.OnInputMonthlyPayment(4_000, Thumb.LEFT))
            val startingState = feature.currentState
            feature.accept(LoanPricePicker.Msg.OnInputLoanPeriod(months))
            "it should cap monthlyPaymentFrom by the min possible payment" {
                feature.currentState.loanState.maybeValue?.paymentFrom shouldBe 8_900
            }
            "priceFrom recalculated with the same monthly payment but different period" {
                feature.currentState.priceFrom shouldBe 100_000
            }
            "priceTo recalculated with the new monthly payment and different period" {
                feature.currentState.priceTo shouldBe 813_000
            }
            "other parameters should remain unchanged" {
                feature.currentState.loanState.maybeValue?.paymentTo shouldBe startingState.loanState.maybeValue?.paymentTo
                feature.currentState.loanState.maybeValue?.initialFee shouldBe startingState.loanState.maybeValue?.initialFee
            }
        }
    }
    "describe user already input some loan parameters and open picker again" - {
        val creditGroup = CreditGroup(
            paymentFrom = 6400,
            paymentTo = 42_750,
            initialFee = 1_000_000,
            loanTerm = 17
        )
        val priceFrom: Long = 1_100_000
        val priceTo: Long = 1_670_000
        val contextWithPrice = defaultContext.copy(
            previousPricePickerModel = LoanPricePickerModel(
                priceFrom,
                priceTo,
                creditGroup
            )
        )
        val feature = createFeature(LoanPricePicker.initialState(contextWithPrice))
        feature.accept(LoanPricePicker.Msg.OnCalculatorParamsResult(Try.Success(calculatorParams)))
        "it should init unchanged" {
            feature.currentState.asClue {
                it.loanState.maybeValue shouldBe creditGroup
                it.priceFrom shouldBe priceFrom
                it.priceTo shouldBe priceTo
            }
        }
    }
    "describe open price picker even if empty" - {
        val context = defaultContext.copy(expandLoanEvenIfEmpty = true)
        val feature = createFeature(LoanPricePicker.initialState(context))
        "it should expand loan without toggle message" {
            feature.accept(LoanPricePicker.Msg.OnCalculatorParamsResult(Try.Success(calculatorParams)))
            feature.currentState.loanState.maybeValue.asClue {
                assertSoftly {
                    it.shouldNotBeNull()
                    it.paymentTo shouldBe 72000
                    it.paymentFrom shouldBe 3600
                    it.loanTerm shouldBe 32
                    it.initialFee shouldBe 0
                }
            }
        }
    }
})

private fun createFeature(state: LoanPricePicker.State) = TeaTestFeature(state, LoanPricePicker::reduce)
