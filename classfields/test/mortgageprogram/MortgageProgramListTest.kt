package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageApplicationFormScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramSortingDialogScreen
import com.yandex.mobile.realty.core.screen.MortgageRegionSuggestScreen
import com.yandex.mobile.realty.core.screen.RangeDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * @author shpigun on 15.02.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramListTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = MortgageProgramListActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            regionParams = RegionParams(
                RGID.toInt(),
                0,
                "в Москве и МО",
                emptyMap(),
                emptyMap(),
                RegionParamsConfigImpl.DEFAULT.schoolInfo,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                null
            )
        ),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldShowPrograms() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearchAlfaIntegration.json")

            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                price = TWO_MILLIONS,
                initialPayment = ONE_MILLION,
                period = SIXTEEN
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAgeAndExp.json",
                price = MIN_PRICE,
                initialPayment = MIN_INITIAL_PAYMENT,
                period = MIN_PERIOD
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoExp.json",
                price = MAX_PRICE,
                initialPayment = MAX_INITIAL_PAYMENT,
                period = MAX_PERIOD
            )

            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoLogo.json",
                price = PRICE_SLIDER_VALUE.roundToInt().toString(),
                initialPayment = INITIAL_PAYMENT_SLIDER_VALUE.roundToInt().toString(),
                period = PERIOD_SLIDER_VALUE.roundToInt().toString()
            )
        }

        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            waitUntil {
                listView.contains(flatTypeItem)
            }

            listView.scrollToTop()
            listView.isMortgageProgramListContentMatches(
                key = "/MortgageProgramListTest/shouldShowPrograms/contentState1"
            )

            listView.scrollToTop()
            flatTypeNew.click()
            priceInputView.retypeText(TWO_MILLIONS)
            initialPaymentInputView.retypeText(ONE_MILLION)
            periodInputView.retypeText(SIXTEEN)

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }

            listView.isMortgageProgramListContentMatches(
                key = "/MortgageProgramListTest/shouldShowPrograms/contentState2"
            )

            listView.scrollToTop()

            flatTypeSecondary.click()
            priceInputView.retypeText(ZERO)
            waitUntil {
                initialPaymentInputView.textMatches(UPPER_BOUND_FORMATTED)
            }
            initialPaymentField.isViewStateMatches(
                "/MortgageProgramListTest/shouldShowPrograms/initialPaymentFilterUpperBound"
            )
            initialPaymentInputView.retypeText(ZERO)
            periodInputView.retypeText(ZERO)

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE_AND_EXP))
            }

            listView.isMortgageProgramListContentMatches(
                key = "/MortgageProgramListTest/shouldShowPrograms/contentState3"
            )

            listView.scrollToTop()

            flatTypeAny.click()
            priceInputView.retypeText(SIXTY_MILLIONS)
            waitUntil {
                initialPaymentInputView.textMatches(LOWER_BOUND_FORMATTED)
            }

            initialPaymentField.isViewStateMatches(
                "/MortgageProgramListTest/shouldShowPrograms/initialPaymentFilterLowerBound"
            )
            initialPaymentInputView.retypeText(SIXTY_MILLIONS)
            periodInputView.retypeText(SIXTY_MILLIONS)
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_EXP))
            }

            listView.isMortgageProgramListContentMatches(
                key = "/MortgageProgramListTest/shouldShowPrograms/contentState4"
            )

            listView.scrollToTop()
            priceSlider.setValue(PRICE_SLIDER_VALUE)
            waitUntil {
                initialPaymentInputView.textMatches(SLIDER_PRICE_FORMATTED)
            }
            initialPaymentSlider.setValue(INITIAL_PAYMENT_SLIDER_VALUE)
            periodSlider.setValue(PERIOD_SLIDER_VALUE)

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_LOGO))
            }

            listView.isMortgageProgramListContentMatches(
                key = "/MortgageProgramListTest/shouldShowPrograms/contentState5"
            )
        }
    }

    @Test
    fun shouldShowRegionSuggest() {

        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
            registerRegionList(
                suggestText = SAINT_PETERSBURG,
                responseFileName = "regionListSaintPetersburg.json"
            )
            registerRegionInfo(rgid = RGID_SPB, responseFileName = "regionInfoSaintPetersburg.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchAlfaIntegration.json",
                rgid = RGID_SPB
            )
            registerRegionList(
                suggestText = MOSCOW_MO,
                responseFileName = "regionListMoscowMO.json"
            )
            registerRegionInfo(
                rgid = RGID_MOSCOW,
                responseFileName = "regionInfoMoscowMO.json"
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                rgid = RGID_MOSCOW
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            waitUntil {
                listView.contains(filterExpanderItem)
            }
            filterExpanderItem.view.click()

            waitUntil {
                listView.contains(regionItem)
            }
            regionItem.view.click()
        }

        onScreen<MortgageRegionSuggestScreen> {
            root.isMortgageRegionSuggestStateMatches(
                "/MortgageProgramListTest/shouldShowRegionSuggest/initialState"
            )
            searchView.typeText(SAINT_PETERSBURG)

            regionSuggest(SAINT_PETERSBURG)
                .waitUntil { listView.contains(this) }
                .also { view ->
                    view.isViewStateMatches(
                        "/MortgageProgramListTest/" +
                            "shouldShowRegionSuggest/suggestSaintPetersburg"
                    )
                }
                .click()
        }

        onScreen<MortgageProgramListScreen> {
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_ALFA_INTEGRATION))
            }
            listView.scrollTo(regionItem)
            regionItem.view.isViewStateMatches(
                "/MortgageProgramListTest/shouldShowRegionSuggest/regionSaintPetersburg"
            )

            regionItem.view.click()
        }

        onScreen<MortgageRegionSuggestScreen> {
            searchView.typeText(MOSCOW_MO)

            regionSuggest(MOSCOW_MO)
                .waitUntil { listView.contains(this) }
                .also { view ->
                    view.isViewStateMatches(
                        "/MortgageProgramListTest/" +
                            "shouldShowRegionSuggest/suggestMoscowMO"
                    )
                }
                .click()
        }

        onScreen<MortgageProgramListScreen> {
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }
            listView.scrollTo(regionItem)
            regionItem.view.isViewStateMatches(
                "/MortgageProgramListTest/shouldShowRegionSuggest/regionMoscowMO"
            )
        }
    }

    @Test
    fun shouldOpenAlfaForm() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearchAlfaIntegration.json")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            mortgageProgramSnippet(ID_ALFA_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(MORTGAGE_PROPOSITION_URL) }
        }
    }

    @Test
    fun shouldOpenNativeForm() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearchNativeIntegration.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            mortgageProgramSnippet(ID_NATIVE_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .click()
        }
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { listView.contains(lastNameInputItem) }
        }
    }

    @Test
    fun shouldShowNextPage() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchAlfaIntegration.json",
                page = 1
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            waitUntil {
                listView.contains(nextPageButtonItem)
            }
            nextPageButtonItem.view.isViewStateMatches(
                "/MortgageProgramListTest/shouldShowNextPage/nextPageButton"
            )
            nextPageButtonItem.view.click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_ALFA_INTEGRATION))
            }
            listView.doesNotContain(nextPageButtonItem)
        }
    }

    @Test
    fun shouldApplyMortgageTypeFilter() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                mortgageType = listOf("EASTERN", "MILITARY")
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAgeAndExp.json",
                mortgageType = listOf("EASTERN", "MILITARY", "STANDARD", "STATE_SUPPORT")
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoExp.json",
                mortgageType = listOf(
                    "EASTERN",
                    "MILITARY",
                    "STANDARD",
                    "STATE_SUPPORT",
                    "TARGET",
                    "YOUNG_FAMILY"
                )
            )
            registerMortgageProgramSearch("mortgageProgramSearchAlfaIntegration.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()
            mortgageTypeItem.waitUntil { listView.contains(this) }
                .childWithText(R.string.mortgage_filter_mortgage_type_eastern)
                .click()
            mortgageTypeItem.view
                .childWithText(R.string.mortgage_filter_mortgage_type_military)
                .click()

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }
            listView.scrollTo(mortgageTypeItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyMortgageTypeFilter/twoSelected"
            )

            listView.scrollToTop()
            listView.scrollTo(mortgageTypeItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_mortgage_type_standart)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            mortgageTypeItem.view
                .childWithText(R.string.mortgage_filter_mortgage_type_state_support)
                .click()

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE_AND_EXP))
            }
            listView.scrollTo(mortgageTypeItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyMortgageTypeFilter/fourSelected"
            )

            listView.scrollToTop()
            listView.scrollTo(mortgageTypeItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_mortgage_type_target)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            mortgageTypeItem.view
                .childWithText(R.string.mortgage_filter_mortgage_type_young_family)
                .click()

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_EXP))
            }
            listView.scrollTo(mortgageTypeItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyMortgageTypeFilter/allSelected"
            )

            resetButton.click()

            listView.scrollToTop()
            listView.scrollTo(mortgageTypeItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyMortgageTypeFilter/noSelected"
            )
        }
    }

    @Test
    fun shouldApplyRateFilter() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                rate = Pair("2", "5")
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAgeAndExp.json",
                rate = Pair("2", null)
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoExp.json",
                rate = Pair(null, "5")
            )
            registerMortgageProgramSearch("mortgageProgramSearchAlfaIntegration.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()
            rateItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            onScreen<RangeDialogScreen> {
                valueFromView.typeText("2")
                valueToView.typeText("5")
                okButton.click()
            }
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }
            listView.scrollTo(rateItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyRateFilter/closed"
            )

            resetButton.click()

            listView.scrollToTop()
            listView.scrollTo(rateItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .waitUntil { isCompletelyDisplayed() }
                .click()
            onScreen<RangeDialogScreen> {
                valueFromView.typeText("2")
                okButton.click()
            }
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE_AND_EXP))
            }
            listView.scrollTo(rateItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyRateFilter/lowerBound"
            )

            resetButton.click()
            listView.scrollToTop()
            listView.scrollTo(rateItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .waitUntil { isCompletelyDisplayed() }
                .click()
            onScreen<RangeDialogScreen> {
                valueToView.typeText("5")
                okButton.click()
            }
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_EXP))
            }
            listView.scrollTo(rateItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyRateFilter/upperBound"
            )
            resetButton.click()

            listView.scrollTo(rateItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyRateFilter/empty"
            )
        }
    }

    @Test
    fun shouldApplyBorrowerCategoryFilter() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                borrowerCategory = listOf("INDIVIDUAL_ENTREPRENEUR")
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAgeAndExp.json",
                borrowerCategory = listOf("EMPLOYEE")
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoExp.json",
                borrowerCategory = listOf("BUSINESS_OWNER")
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()
            borrowerCategoryItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(
                    R.string.mortgage_filter_borrower_category_individual_entrepreneur
                )
                .waitUntil { isCompletelyDisplayed() }
                .click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }
            listView.scrollTo(borrowerCategoryItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyBorrowerCategoryFilter/oneSelected"
            )

            listView.scrollToTop()
            listView.scrollTo(borrowerCategoryItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_borrower_category_employee)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE_AND_EXP))
            }
            listView.scrollTo(borrowerCategoryItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyBorrowerCategoryFilter/twoSelected"
            )

            listView.scrollToTop()
            listView.scrollTo(borrowerCategoryItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_borrower_category_business_owner)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_EXP))
            }
            listView.scrollTo(borrowerCategoryItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyBorrowerCategoryFilter/threeSelected"
            )
            resetButton.click()
            listView.scrollTo(borrowerCategoryItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyBorrowerCategoryFilter/noSelected"
            )
        }
    }

    @Test
    fun shouldApplyIncomeConfirmationFilter() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                incomeConfirmation = "WITHOUT_PROOF"
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAgeAndExp.json",
                incomeConfirmation = "PFR"
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoExp.json",
                incomeConfirmation = "BANK_REFERENCE"
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchAlfaIntegration.json",
                incomeConfirmation = "REFERENCE_2NDFL"
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()

            listView.scrollTo(incomeConfirmationItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_income_confirmation_without_proof)
                .waitUntil { isCompletelyDisplayed() }
                .click()

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }

            listView.scrollTo(incomeConfirmationItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyIncomeConfirmationFilter/withoutProof"
            )

            resetButton.click()
            listView.scrollToTop()

            listView.scrollTo(incomeConfirmationItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_income_confirmation_pfr)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE_AND_EXP))
            }
            listView.scrollTo(incomeConfirmationItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyIncomeConfirmationFilter/pfr"
            )
            listView.scrollToTop()

            listView.scrollTo(incomeConfirmationItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_income_confirmation_bank_reference)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_EXP))
            }
            listView.scrollTo(incomeConfirmationItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyIncomeConfirmationFilter/bankReference"
            )
            listView.scrollToTop()
            listView.scrollTo(incomeConfirmationItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_income_confirmation_2ndfl)
                .waitUntil { isCompletelyDisplayed() }
                .click()
            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_ALFA_INTEGRATION))
            }
            listView.scrollTo(incomeConfirmationItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyIncomeConfirmationFilter/2Ndfl"
            )
            resetButton.click()
            listView.scrollTo(incomeConfirmationItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyIncomeConfirmationFilter/noSelected"
            )
        }
    }

    @Test
    fun shouldApplyMaternityFundsFilter() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                maternityFunds = true
            )
            registerMortgageProgramSearch("mortgageProgramSearchNoAgeAndExp.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()

            listView.scrollTo(maternityFundsItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_maternity_funds)
                .waitUntil { isCompletelyDisplayed() }
                .click()

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE))
            }

            listView.scrollTo(maternityFundsItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyMaternityFundsFilter/selected"
            )

            resetButton.click()

            waitUntil {
                listView.contains(mortgageProgramSnippet(ID_NO_AGE_AND_EXP))
            }

            listView.scrollTo(maternityFundsItem).isViewStateMatches(
                "/MortgageProgramListTest/shouldApplyMaternityFundsFilter/unselected"
            )
        }
    }

    @Test
    fun shouldShowFilterExpander() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()
            filterExpanderItem.waitUntil { filterExpanderTextView.isTextEquals("Скрыть параметры") }
                .isViewStateMatches(
                    "/MortgageProgramListTest/showFilterExpander/expandedExpander"
                )

            listView.scrollToTop()
            listView.scrollTo(mortgageTypeItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_mortgage_type_target)
                .waitUntil { isCompletelyDisplayed() }
                .click()

            listView.scrollTo(rateItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()

            onScreen<RangeDialogScreen> {
                valueFromView.typeText("2")
                okButton.click()
            }

            listView.scrollTo(borrowerCategoryItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_borrower_category_business_owner)
                .click()

            listView.scrollTo(incomeConfirmationItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_income_confirmation_2ndfl)
                .click()

            listView.scrollTo(maternityFundsItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_maternity_funds)
                .click()

            listView.scrollTo(filterExpanderItem)
                .click()
            filterExpanderItem.waitUntil {
                filterExpanderTextView.isTextEquals("Скрыто 5 параметров")
            }
                .isViewStateMatches(
                    "/MortgageProgramListTest/showFilterExpander/collapsedExpander"
                )
        }
    }

    @Test
    fun shouldShowParamsToolbarButton() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearchNoLogo.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchAlfaIntegration.json",
                mortgageType = listOf("TARGET")
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()
            showProgramsButtonItem.waitUntil { listView.contains(this) }
            listView.scrollToTop()
            root.isViewStateMatches(
                "/MortgageProgramListTest/showParamsToolbarButton/expanded"
            )
            listView.scrollTo(showProgramsButtonItem).click()
            root.isViewStateMatches(
                "/MortgageProgramListTest/showParamsToolbarButton/showProgramsWithoutFilters"
            )
            paramsButton.click()
            waitUntil {
                paramsButton.isHidden()
            }
            listView.scrollTo(mortgageTypeItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .childWithText(R.string.mortgage_filter_mortgage_type_target)
                .click()
            floatingShowProgramsButton.click()
            root.isViewStateMatches(
                "/MortgageProgramListTest/showParamsToolbarButton/showProgramsWithFilters"
            )
        }
    }

    @Test
    fun shouldChangeProgramsOrderWhenSortClicked() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchAlfaIntegration.json",
                sorting = "RELEVANCE"
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoAge.json",
                sorting = "MORTGAGE_MIN_RATE"
            )
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearchNoExp.json",
                sorting = "RELEVANCE"
            )
        }
        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            mortgageProgramSnippet(ID_ALFA_INTEGRATION).waitUntil { listView.contains(this) }
            listView.scrollTo(sortingButton)
                .click()
            onScreen<MortgageProgramSortingDialogScreen> {
                mortgageRateView.waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            mortgageProgramSnippet(ID_NO_AGE).waitUntil { listView.contains(this) }
            listView.scrollTo(sortingButton)
                .click()
            onScreen<MortgageProgramSortingDialogScreen> {
                relevanceView.waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            mortgageProgramSnippet(ID_NO_EXP).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldOpenProgramCard() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearchAllIntegrationTypes.json")
        }
        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            mortgageProgramSnippet(ID_ALFA_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .click()
            onScreen<MortgageProgramCardScreen> {
                screenTitleItem(CARD_TITLE_ALFA).waitUntil { listView.contains(this) }
                pressBack()
            }
            listView.scrollTo(mortgageProgramSnippet(ID_NO_INTEGRATION))
                .detailsButton
                .click()
            onScreen<MortgageProgramCardScreen> {
                screenTitleItem(CARD_TITLE_NO_INTEGRATION).waitUntil { listView.contains(this) }
            }
        }
    }

    private fun DispatcherRegistry.registerInitialParamsSearch(responseFileName: String) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("page", "0")
            },
            response {
                assetBody("MortgageProgramListTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        rgid: String = RGID,
        flatType: String? = null,
        price: String? = null,
        initialPayment: String? = null,
        period: String? = null,
        mortgageType: List<String>? = null,
        rate: Pair<String?, String?>? = null,
        borrowerCategory: List<String>? = null,
        incomeConfirmation: String? = null,
        maternityFunds: Boolean = false,
        sorting: String? = null,
        page: Int = 0
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("rgid", rgid.toString())
                queryParam("page", page.toString())
                queryParam("pageSize", "10")
                flatType?.let { queryParam("flatType", it) }
                price?.let { queryParam("propertyCost", it) }
                initialPayment?.let { queryParam("downPaymentSum", it) }
                period?.let { queryParam("periodYears", it) }
                mortgageType?.forEach { queryParam("mortgageType", it) }
                rate?.let { (lower, upper) ->
                    lower?.let { queryParam("minRate", it) }
                    upper?.let { queryParam("maxRate", it) }
                }
                borrowerCategory?.forEach { queryParam("borrowerCategory", it) }
                incomeConfirmation?.let { queryParam("incomeConfirmationType", it) }
                if (maternityFunds) {
                    queryParam("maternityCapital", "YES")
                }
                sorting?.let { queryParam("sort", it) }
            },
            response {
                assetBody("MortgageProgramListTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerRegionList(
        suggestText: String,
        responseFileName: String
    ) {
        register(
            request {
                path("1.0/regionList.json")
                queryParam("text", suggestText)
            },
            response {
                assetBody("MortgageProgramListTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerRegionInfo(
        rgid: String,
        responseFileName: String
    ) {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", rgid)
            },
            response {
                assetBody("MortgageProgramListTest/$responseFileName")
            }
        )
    }

    companion object {

        private const val RGID = "587795"
        private const val RGID_MOSCOW = "741964"
        private const val RGID_SPB = "417899"

        private const val SIXTY_MILLIONS = "60000000"
        private const val TWO_MILLIONS = "2000000"
        private const val ONE_MILLION = "1000000"
        private const val SIXTEEN = "16"
        private const val ZERO = "0"

        private const val UPPER_BOUND_FORMATTED = "333 334"
        private const val LOWER_BOUND_FORMATTED = "5 000 000"
        private const val SLIDER_PRICE_FORMATTED = "1 500 000"

        private const val MIN_PRICE = "333334"
        private const val MIN_INITIAL_PAYMENT = "33333"
        private const val MIN_PERIOD = "10"

        private const val MAX_PRICE = "50000000"
        private const val MAX_INITIAL_PAYMENT = "50000000"
        private const val MAX_PERIOD = "30"

        private const val PRICE_SLIDER_VALUE = 1_500_000f
        private const val INITIAL_PAYMENT_SLIDER_VALUE = 1_200_000f
        private const val PERIOD_SLIDER_VALUE = 13f

        private const val MORTGAGE_PROPOSITION_URL = "https://alfabank.ru/get-money/mortgage/" +
            "iform/?maternalCapital=false&payrollClient=false&partnerProperty=true" +
            "&utm_source=yandex_realty&utm_medium=month&filterCity=msc&" +
            "type=construction&creditTerm=15&estateCost=3000000&initialFee=900000&" +
            "stateSubsidy=false&utm_campaign=marketing_feb-dec21_mortgage_omd&" +
            "utm_content=mortgage-section_iframe_application-form__always-on&" +
            "utm_term=yarealty-uid_1&" +
            "platformId=yandex-realty_month_marketing_feb-dec21_mortgage_omd_mortgage-" +
            "section_iframe_application-form__always-on_yarealty-" +
            "uid_1&only-content=true"

        private const val ID_ALFA_INTEGRATION = "1"
        private const val ID_NATIVE_INTEGRATION = "7"
        private const val ID_NO_INTEGRATION = "10"
        private const val ID_NO_AGE = "2"
        private const val ID_NO_AGE_AND_EXP = "3"
        private const val ID_NO_EXP = "4"
        private const val ID_NO_LOGO = "5"

        private const val SAINT_PETERSBURG = "city_Saint-Petersburg"
        private const val MOSCOW_MO = "subject_MoscowMO"

        private const val CARD_TITLE_ALFA = "Ипотека на строящееся жилье"
        private const val CARD_TITLE_NO_INTEGRATION = "Ипотека Мегаполис"
    }
}
