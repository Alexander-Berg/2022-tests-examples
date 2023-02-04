package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.view.TRecyclerItem
import com.yandex.mobile.realty.core.view.TView
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createStandardProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 6/22/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardCalculatorTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createStandardProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule
    )

    @Test
    fun shouldShowCalculator() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult("resultDefault.json")
            registerCalculatorResult(
                "resultChanged.json",
                propertyCost = PRICE_CHANGED,
                rate = RATE_CHANGED,
                downPaymentSum = INITIAL_PAYMENT_CHANGED,
                periodYears = PERIOD_CHANGED
            )
            registerCalculatorResult(
                "resultChangedOutOfRange.json",
                propertyCost = PRICE_OUT_OF_RANGE,
                rate = RATE_ODD,
                downPaymentSum = INITIAL_PAYMENT_OUT_OF_RANGE,
                periodYears = PERIOD_OUT_OF_RANGE
            )
        }
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardCalculatorTest/shouldShowCalculator"

        onScreen<MortgageProgramCardScreen> {
            isCalculatorResultViewStateMatches(
                "MortgageProgramCardCalculatorTest/successDefault",
                MONTHLY_PAYMENT_DEFAULT
            )

            priceInputSlider.setValue(PRICE_CHANGED.toFloat())
            rateInputSlider.setValue(RATE_CHANGED.toFloat())
            initialPaymentInputSlider.setValue(INITIAL_PAYMENT_CHANGED.toFloat())
            periodInputSlider.setValue(PERIOD_CHANGED.toFloat())
            isCalculatorResultViewStateMatches(
                "$prefix/successChanged",
                MONTHLY_PAYMENT_CHANGED
            )

            priceInputView.replaceText(PRICE_OUT_OF_RANGE_INPUT.toString())
            rateInputView.replaceText(RATE_ODD_INPUT)
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_OUT_OF_RANGE_INPUT.toString())
            periodInputView.replaceText(PERIOD_OUT_OF_RANGE_INPUT.toString())
            isCalculatorResultViewStateMatches(
                "$prefix/successChangedOutOfRange",
                MONTHLY_PAYMENT_OUT_OF_RANGE
            )
        }
    }

    @Test
    fun shouldShowErrors() {
        configureWebServer {
            registerCalculatorConfigError()
            registerCalculatorConfig()
            registerCalculatorError()
            registerCalculatorResult("resultDefault.json")
            registerCalculatorError(
                responseFileName = "errorInsufficient.json",
                propertyCost = PRICE_CREDIT_INSUFFICIENT,
                downPaymentSum = INITIAL_PAYMENT_CREDIT_INSUFFICIENT
            )
            repeat(2) {
                registerCalculatorResult(
                    "resultChanged.json",
                    propertyCost = PRICE_CHANGED,
                    downPaymentSum = INITIAL_PAYMENT_CHANGED,
                    periodYears = PERIOD_CHANGED
                )
            }
            registerCalculatorError(
                responseFileName = "errorExceeded.json",
                propertyCost = PRICE_CREDIT_EXCEEDED,
                downPaymentSum = INITIAL_PAYMENT_CREDIT_EXCEEDED
            )
        }
        activityTestRule.launchActivity()
        val prefix = "MortgageProgramCardCalculatorTest/shouldShowErrors"

        onScreen<MortgageProgramCardScreen> {
            isCalculatorErrorViewStateMatches("$prefix/errorConfig", calculatorBlockErrorItem)

            calculatorBlockErrorItem.view.click()
            isCalculatorErrorViewStateMatches("$prefix/errorDefault", calculatorGenericErrorItem)

            calculatorGenericErrorItem.view.click()
            isCalculatorResultViewStateMatches(
                "MortgageProgramCardCalculatorTest/successDefault",
                MONTHLY_PAYMENT_DEFAULT
            )

            priceInputView.replaceText(PRICE_CREDIT_INSUFFICIENT.toString())
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CREDIT_INSUFFICIENT.toString())
            isCalculatorErrorViewStateMatches(
                "$prefix/errorCreditInsufficient",
                calculatorParamsErrorItem
            )

            priceInputView.replaceText(PRICE_CHANGED.toString())
            periodInputView.replaceText(PERIOD_CHANGED.toString())
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CHANGED.toString())
            isCalculatorResultViewStateMatches(
                "$prefix/recoveredCreditInsufficient",
                MONTHLY_PAYMENT_CHANGED
            )

            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CREDIT_EXCEEDED.toString())
            periodInputView.replaceText(PERIOD_DEFAULT.toString())
            priceInputView.replaceText(PRICE_CREDIT_EXCEEDED.toString())
            isCalculatorErrorViewStateMatches(
                "$prefix/errorCreditExceeded",
                calculatorParamsErrorItem
            )

            priceInputView.replaceText(PRICE_CHANGED.toString())
            periodInputView.replaceText(PERIOD_CHANGED.toString())
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CHANGED.toString())
            isCalculatorResultViewStateMatches(
                "$prefix/recoveredCreditExceeded",
                MONTHLY_PAYMENT_CHANGED
            )
        }
    }

    @Test
    fun shouldOpenProgramCardsWithDefaultCalculatorParams() {
        configureWebServer {
            registerCalculatorConfig(programId = CARD_PROGRAM_ID)
            registerCalculatorResult("resultDefault.json", programId = CARD_PROGRAM_ID)
            registerCalculatorConfig(programId = SIMILAR_PROGRAM_ID)
            registerCalculatorResult("resultDefault.json", programId = SIMILAR_PROGRAM_ID)
            registerSimilarPrograms()
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(SIMILAR_PROGRAM_ID)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<MortgageProgramCardScreen> {
            isCalculatorResultViewStateMatches(
                "MortgageProgramCardCalculatorTest/successDefault",
                MONTHLY_PAYMENT_DEFAULT
            )
        }
    }

    @Test
    fun shouldOpenProgramCardsWithChangedCalculatorParams() {
        configureWebServer {
            registerCalculatorConfig(programId = CARD_PROGRAM_ID)
            registerCalculatorConfig(programId = SIMILAR_PROGRAM_ID)
            registerCalculatorResult(
                "resultChanged.json",
                programId = CARD_PROGRAM_ID,
                propertyCost = PRICE_CHANGED,
                downPaymentSum = INITIAL_PAYMENT_CHANGED,
                rate = RATE_CHANGED,
                periodYears = PERIOD_CHANGED,
            )
            registerCalculatorResult(
                "resultChanged.json",
                programId = SIMILAR_PROGRAM_ID,
                propertyCost = PRICE_CHANGED,
                downPaymentSum = INITIAL_PAYMENT_CHANGED,
                periodYears = PERIOD_CHANGED
            )
            registerSimilarPrograms()
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            waitUntil { listView.contains(calculatorGenericErrorItem) }
            listView.scrollTo(rateConditionsTitleItem)
            rateInputView.replaceText(RATE_CHANGED) // not used in params
            priceInputView.replaceText(PRICE_CHANGED.toString())
            initialPaymentInputView.replaceText(INITIAL_PAYMENT_CHANGED.toString())
            periodInputView.replaceText(PERIOD_CHANGED.toString())
            calculatorResultItem(MONTHLY_PAYMENT_CHANGED)
                .waitUntil { listView.contains(this) }
            mortgageProgramSnippet(SIMILAR_PROGRAM_ID)
                .waitUntil { listView.contains(this) }
                .click()
        }
        val prefix = "MortgageProgramCardCalculatorTest/" +
            "shouldOpenProgramCardsWithChangedCalculatorParams"
        onScreen<MortgageProgramCardScreen> {
            isCalculatorResultViewStateMatches(
                "$prefix/successChangedWithDefaultRate",
                MONTHLY_PAYMENT_CHANGED
            )
        }
    }

    private fun MortgageProgramCardScreen.isCalculatorResultViewStateMatches(
        key: String,
        monthlyPayment: String
    ) {
        calculatorResultItem(monthlyPayment)
            .waitUntil { listView.contains(this) }
        listView.isItemsStateMatches(
            key,
            calculatorTitleItem,
            rateConditionsTitleItem,
            inclusive = false
        )
    }

    private fun MortgageProgramCardScreen.isCalculatorErrorViewStateMatches(
        key: String,
        errorItem: TRecyclerItem<TView>
    ) {
        waitUntil { listView.contains(errorItem) }
        listView.isItemsStateMatches(
            key,
            calculatorTitleItem,
            rateConditionsTitleItem,
            inclusive = false
        )
    }

    private fun DispatcherRegistry.registerCalculatorConfig(programId: String = CARD_PROGRAM_ID) {
        register(
            request {
                path("2.0/mortgage/program/$programId/calculator")
            },
            response {
                assetBody("MortgageProgramCardCalculatorTest/config.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorConfigError() {
        register(
            request {
                path("2.0/mortgage/program/$CARD_PROGRAM_ID/calculator")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerCalculatorResult(
        responseFileName: String,
        programId: String = CARD_PROGRAM_ID,
        downPaymentSum: Long = 900_000,
        rate: String = "7.3",
        periodYears: Int = PERIOD_DEFAULT,
        propertyCost: Long = 3_000_000
    ) {
        register(
            request {
                path("2.0/mortgage/program/$programId/calculator")
                queryParam("downPaymentSum", downPaymentSum.toString())
                queryParam("rate", rate)
                queryParam("periodYears", periodYears.toString())
                queryParam("propertyCost", propertyCost.toString())
            },
            response {
                assetBody("MortgageProgramCardCalculatorTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorError(
        responseFileName: String? = null,
        downPaymentSum: Long = 900_000,
        rate: String = "7.3",
        periodYears: Int = 15,
        propertyCost: Long = 3_000_000
    ) {
        register(
            request {
                path("2.0/mortgage/program/$CARD_PROGRAM_ID/calculator")
                queryParam("downPaymentSum", downPaymentSum.toString())
                queryParam("rate", rate)
                queryParam("periodYears", periodYears.toString())
                queryParam("propertyCost", propertyCost.toString())
            },
            response {
                if (responseFileName != null) {
                    assetBody("MortgageProgramCardCalculatorTest/$responseFileName")
                    setResponseCode(400)
                } else {
                    setResponseCode(500)
                }
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarPrograms() {
        register(
            request {
                path("2.0/mortgage/program/$CARD_PROGRAM_ID/similar")
            },
            response {
                assetBody("mortgage/programSearch.json")
            }
        )
    }

    companion object {

        private const val CARD_PROGRAM_ID = "1"
        private const val SIMILAR_PROGRAM_ID = "12"

        private const val PERIOD_DEFAULT = 15
        private const val MONTHLY_PAYMENT_DEFAULT = "19 936 ₽"

        private const val PRICE_OUT_OF_RANGE_INPUT = 50_222_111L
        private const val PRICE_OUT_OF_RANGE = 50_000_000L
        private const val INITIAL_PAYMENT_OUT_OF_RANGE_INPUT = 1_544_333L
        private const val INITIAL_PAYMENT_OUT_OF_RANGE = 7_500_000L
        private const val RATE_ODD_INPUT = "11.23333"
        private const val RATE_ODD = "11.2"
        private const val PERIOD_OUT_OF_RANGE_INPUT = 1
        private const val PERIOD_OUT_OF_RANGE = 3
        private const val MONTHLY_PAYMENT_OUT_OF_RANGE = "51 218 ₽"

        private const val PRICE_CHANGED = 5_000_000L
        private const val INITIAL_PAYMENT_CHANGED = 1_200_000L
        private const val RATE_CHANGED = "8.8"
        private const val PERIOD_CHANGED = 16
        private const val MONTHLY_PAYMENT_CHANGED = "49 001 ₽"

        private const val PRICE_CREDIT_INSUFFICIENT = 1_000_000L
        private const val INITIAL_PAYMENT_CREDIT_INSUFFICIENT = 900_000L
        private const val PRICE_CREDIT_EXCEEDED = 30_000_000L
        private const val INITIAL_PAYMENT_CREDIT_EXCEEDED = 5_000_000L
    }
}
