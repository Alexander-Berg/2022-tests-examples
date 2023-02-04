package ru.auto.ara.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.SplashActivity
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.shark.getCalculatorParamsNotFound
import ru.auto.ara.core.dispatchers.shark.getErrorCreditProduct
import ru.auto.ara.core.dispatchers.shark.getOneCreditProduct
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.loanbroker.checkLoanLK
import ru.auto.ara.core.robot.search_filter.checkPriceFilter
import ru.auto.ara.core.robot.search_filter.checkPriceFilterScreenshot
import ru.auto.ara.core.robot.search_filter.performPriceFilter
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.RootRouting
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

private const val PRICE_FIELD_NAME = "Цена, \u20BD"
private const val PRICE_FROM_PARAM = "price_from"
private const val PRICE_TO_PARAM = "price_to"
private const val SEARCH_TAG_PARAM = "search_tag"
private const val CREDIT_GROUP_PARAM = "credit_group"
private const val ALLOWED_FOR_CREDIT = "allowed_for_credit"
private const val ALLOWED_FOR_CREDIT_FIELD = "Доступны в\u00A0кредит"
private const val OVERSCROLL_FIELD = "Срок размещения"

@RunWith(AndroidJUnit4::class)
class LoanPricePickerTest {
    private val webServerRule = WebServerRule {
        getOneCreditProduct()
        getOfferCount()
    }
    private val activityRule = lazyActivityScenarioRule<SplashActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityRule,
    )

    @Before
    fun setup() {
        activityRule.launchActivity()
        performMain { openFilters() }
    }

    @Test
    fun shouldSeePricePicker() {
        openPriceField()
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("just_price") }
    }

    @Test
    @Ignore("toast assertions don't work when keyboard is shown since API 30 https://github.com/android/android-test/issues/803")
    fun shouldSeeWarningIfTryToEnterPriceOutsideRange() {
        openPriceField()
        performPriceFilter { enterStartPrice(0) }
        checkCommon { isToastWithText("Значение цены должно быть больше 10 000 ₽ и меньше 300 000 000 ₽") }
    }

    @Test
    @Ignore("toast assertions don't work when keyboard is shown since API 30 https://github.com/android/android-test/issues/803")
    fun shouldSeeErrorIfCreditProductNotLoaded() {
        openPriceField {
            getErrorCreditProduct()
            getCalculatorParamsNotFound()
        }
        checkCommon { isToastWithTextNotExist() }
        performPriceFilter { clickCreditSwitch() }
        checkCommon { isToastWithText("Что-то пошло не так, попробуйте позже") }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("just_price") }
    }

    @Test
    fun allowedForCreditCheckboxShouldDeactivatePricePickerCreditCheckbox() {
        performFilter { clickField(PRICE_FIELD_NAME) }
        performPriceFilter { clickCreditSwitch() }
        performFilter { clickAcceptButton() }
        checkFilter { isCheckedWithOverScroll(OVERSCROLL_FIELD, ALLOWED_FOR_CREDIT_FIELD, checked = true) }
        performFilter { clickCheckBoxWithOverScroll(OVERSCROLL_FIELD, ALLOWED_FOR_CREDIT_FIELD) }
        performFilter { clickField(PRICE_FIELD_NAME) }
        checkPriceFilter { checkLoanSwitchIs(enabled = false) }
    }

    @Test
    fun allowedForCreditCheckboxShouldActivatePricePickerCreditCheckbox() {
        performFilter { clickField(PRICE_FIELD_NAME) }
        checkPriceFilter { checkLoanSwitchIs(enabled = false) }
        performFilter {
            clickAcceptButton()
            clickCheckBoxWithOverScroll(OVERSCROLL_FIELD, ALLOWED_FOR_CREDIT_FIELD)
            clickField(PRICE_FIELD_NAME)
        }
        checkPriceFilter { checkLoanSwitchIs(enabled = true) }
        performPriceFilter { clickCreditSwitch() }
        performFilter { clickAcceptButton() }
        checkFilter { isCheckedWithOverScroll(OVERSCROLL_FIELD, ALLOWED_FOR_CREDIT_FIELD, checked = false) }
    }

    @Test
    fun shouldEnterPrice() {
        openPriceField {
            getOneCreditProduct()
            getOfferCount().watch {
                checkRequestBodyParameter(PRICE_FROM_PARAM, "40000")
                checkRequestBodyParameter(PRICE_TO_PARAM, "1000000")
                checkNotRequestBodyParameter(CREDIT_GROUP_PARAM)
                checkNotRequestBodyParameter(SEARCH_TAG_PARAM)
            }
        }
        performPriceFilter {
            enterStartPrice(price = 40_000)
            enterEndPrice(price = 1000_000)
        }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("entered_price") }
        performFilter { clickAcceptButton() }

        checkFilter { isContainer(PRICE_FIELD_NAME, "от 40 000 до 1 000 000") }

        performFilter { clickField(PRICE_FIELD_NAME) }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("entered_price") }
    }

    @Test
    fun shouldEnterOnlyPriceFrom() {
        openPriceField {
            getOneCreditProduct()
            getOfferCount().watch {
                checkRequestBodyParameter(PRICE_FROM_PARAM, "40000")
                checkNotRequestBodyParameter(PRICE_TO_PARAM)
                checkNotRequestBodyParameter(CREDIT_GROUP_PARAM)
                checkNotRequestBodyParameter(SEARCH_TAG_PARAM)
            }
        }
        performPriceFilter {
            moveStartPriceSlider(position = 5)
        }
        performFilter { clickAcceptButton() }

        checkFilter { isContainer(PRICE_FIELD_NAME, "от 40 000 ") }
    }


    @Test
    fun shouldEnterOnlyPriceTo() {
        openPriceField {
            getOneCreditProduct()
            getOfferCount().watch {
                checkNotRequestBodyParameter(PRICE_FROM_PARAM)
                checkRequestBodyParameter(PRICE_TO_PARAM, "1000000")
                checkNotRequestBodyParameter(CREDIT_GROUP_PARAM)
                checkNotRequestBodyParameter(SEARCH_TAG_PARAM)
            }
        }
        webServerRule.routing {
        }
        performPriceFilter {
            enterEndPrice(price = 1000_000)
        }
        performFilter { clickAcceptButton() }
        checkFilter { isContainer(PRICE_FIELD_NAME, "до 1 000 000") }
    }

    @Test
    fun shouldSeeLoanPicker() {
        openPriceField()
        performPriceFilter { clickCreditSwitch() }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("with_credit") }
    }

    @Test
    fun shouldEnterLoanParameters() {
        openPriceField {
            getOneCreditProduct()
            getOfferCount().watch {
                checkRequestBodyParameter(PRICE_FROM_PARAM, "1100000")
                checkRequestBodyParameter(PRICE_TO_PARAM, "1670000")
                checkRequestBodyArrayParameter(SEARCH_TAG_PARAM, setOf(ALLOWED_FOR_CREDIT))
                checkRequestBodyParameter("$CREDIT_GROUP_PARAM.initial_fee", "1000000")
                checkRequestBodyParameter("$CREDIT_GROUP_PARAM.payment_from", "6400")
                checkRequestBodyParameter("$CREDIT_GROUP_PARAM.payment_to", "42750")
                checkRequestBodyParameter("$CREDIT_GROUP_PARAM.loan_term", "17")
            }
        }
        performPriceFilter {
            clickCreditSwitch()
            moveMonthlyPaymentFromSlider(4)
            moveLoanPeriodSlider(8)
            enterDownpayment(1000000)
        }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("entered_loan") }
        performFilter { clickAcceptButton() }

        checkFilter { isContainer(PRICE_FIELD_NAME, "от 1 100 000 до 1 670 000") }

        performFilter { clickField(PRICE_FIELD_NAME) }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("entered_loan") }

        performPriceFilter {
            clickCreditSwitch()
            clickCreditSwitch()
        }
        checkPriceFilterScreenshot { comparePriceFilterScreenshots("after_switched") }
    }

    @Test
    fun shouldOpenLoanLkFromPromo() {
        openPriceField()
        performPriceFilter {
            clickCreditSwitch()
            clickLoanPromo()
        }
        checkLoanLK { isLoanLK() }
    }

    private fun openPriceField(routing: RootRouting.() -> Unit = { getOneCreditProduct() }) {
        webServerRule.routing(routing)
        performFilter { clickField(PRICE_FIELD_NAME) }
    }
}
