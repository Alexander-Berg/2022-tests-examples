package com.yandex.mobile.realty.test.filters

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.doesNotExist
import com.yandex.mobile.realty.core.robot.performOnPriceDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.SearchParamsSuggestionsScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.RequestMatcher
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author wgjuh on 28.07.2021
 */
@LargeTest
class SearchParamsSuggestionTest : BaseTest() {

    private var activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        activityTestRule,
        SetupDefaultAppStateRule()
    )

    @Test
    fun shouldShowSnackbarAndBottomSheet() {
        val priceMin = 3
        val priceMax = 4
        val screenshotPathSnackbar = getTestRelatedFilePath("snackbar")
        val screenshotPathBottomSheet = getTestRelatedFilePath("bottomSheet")
        setupTest {
            requestCountOnly(1, null)
            requestCountOnly(1, 2) {
                queryParam("priceMin", "3")
                queryParam("priceMax", "4")
            }
            requestSearchSuggestions("searchParamsSuggestionsTwoSuggestions.json")
        }

        onScreen<FiltersScreen> {
            priceValue.click()
            performOnPriceDialog {
                waitUntilKeyboardAppear()
                typeText(lookup.matchesValueFrom(), priceMin.toString())
                typeText(lookup.matchesValueTo(), priceMax.toString())
                tapOn(lookup.matchesPositiveButton())
            }
            anySnackBarView()
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(screenshotPathSnackbar)
                .invoke { action.click() }
        }

        onScreen<SearchParamsSuggestionsScreen> {
            priceItem.waitUntil { listView.contains(this) }
            root.isViewStateMatches(screenshotPathBottomSheet)
        }
    }

    @Test
    fun shouldChangeSnackbar() {
        val priceMin = 3
        val priceMax = 4
        val screenshotPath = getTestRelatedFilePath("snackbar")
        setupTest {
            requestCountOnly(1, 3)
            requestCountOnly(1, 10) {
                queryParam("priceMin", "3")
                queryParam("priceMax", "4")
            }
        }

        onScreen<FiltersScreen> {
            anySnackBarView().waitUntil { isCompletelyDisplayed() }
            priceValue.click()
            performOnPriceDialog {
                waitUntilKeyboardAppear()
                typeText(lookup.matchesValueFrom(), priceMin.toString())
                typeText(lookup.matchesValueTo(), priceMax.toString())
                tapOn(lookup.matchesPositiveButton())
            }
            anySnackBarView()
                .waitUntil { doesNotExist() }
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(screenshotPath)
        }
    }

    @Test
    fun shouldNotShowSnackbarIfSwiped() {
        val priceMin = 3
        val priceMax = 4
        setupTest {
            requestCountOnly(1, 3)
            requestCountOnly(1, 10) {
                queryParam("priceMin", "3")
                queryParam("priceMax", "4")
            }
        }

        onScreen<FiltersScreen> {

            anySnackBarView()
                .waitUntil { isCompletelyDisplayed() }
                .apply { swipeRight() }
                .waitUntil { doesNotExist() }

            priceValue.click()
            performOnPriceDialog {
                waitUntilKeyboardAppear()
                typeText(lookup.matchesValueFrom(), priceMin.toString())
                typeText(lookup.matchesValueTo(), priceMax.toString())
                tapOn(lookup.matchesPositiveButton())
            }

            anySnackBarView().delayed(1000) {
                doesNotExist()
            }
        }
    }

    @Test
    fun shouldShowSuggestionsError() {
        val screenshotPath = getTestRelatedFilePath("errorItemView")

        setupTest {
            requestCountOnly(1, 3)
            requestSearchSuggestionsError()
            requestSearchSuggestions("searchParamsSuggestionsTwoSuggestions.json")
        }

        onScreen<FiltersScreen> {
            anySnackBarView()
                .waitUntil { isCompletelyDisplayed() }
                .invoke { action.click() }
        }
        onScreen<SearchParamsSuggestionsScreen> {
            errorItem.waitUntil { listView.contains(this) }
                .isViewStateMatches(screenshotPath)
                .click()

            listView.waitUntil {
                contains(priceItem)
                contains(viewportItem)
            }
        }
    }

    @Test
    fun shouldShowNoSuggestions() {
        val screenshotPath = getTestRelatedFilePath("errorItemView")

        setupTest {
            requestCountOnly(1, 3)
            requestSearchSuggestions("searchParamsSuggestionsNoSuggestions.json")
            requestCountOnly(1, 3)
        }

        onScreen<FiltersScreen> {
            anySnackBarView()
                .waitUntil { isCompletelyDisplayed() }
                .invoke { action.click() }
        }

        onScreen<SearchParamsSuggestionsScreen> {
            errorItem.waitUntil { listView.contains(this) }
                .isViewStateMatches(screenshotPath)
                .click()
            rootView.waitUntil { doesNotExist() }
        }
    }

    @Test
    fun shouldShowNotRecognizedSuggestions() {
        val screenshotPath = getTestRelatedFilePath("errorItemView")

        setupTest {
            requestCountOnly(1, 3)
            requestSearchSuggestions("searchParamsSuggestionsNotRecognizedSuggestions.json")
            requestCountOnly(1, 3)
        }

        onScreen<FiltersScreen> {
            anySnackBarView()
                .waitUntil { isCompletelyDisplayed() }
                .invoke { action.click() }
        }
        onScreen<SearchParamsSuggestionsScreen> {
            errorItem.waitUntil { listView.contains(this) }
                .isViewStateMatches(screenshotPath)
            closeButton.click()
            rootView.waitUntil { doesNotExist() }
        }
    }

    @Test
    fun shouldUpdateExtendedCounter() {
        val screenshotPath = getTestRelatedFilePath("bottomSheet")

        setupTest {
            requestCountOnly(0, 3)
            requestSearchSuggestions("searchParamsSuggestionsTwoSuggestions.json")
            requestCountOnly(0, null)
            requestCountOnly(10, null) {
                queryParam("priceMin", "4500000")
                queryParam("priceMax", "5500000")
            }
            requestCountOnly(12, null) {
                queryParam("priceMin", "4500000")
                queryParam("priceMax", "5500000")
                queryParam("bottomLatitude", "59.856087")
                queryParam("topLatitude", "60.134254")
                queryParam("rightLongitude", "31.020739")
                queryParam("leftLongitude", "30.04442")
            }
            requestCountOnly(2, null) {
                queryParam("bottomLatitude", "59.856087")
                queryParam("topLatitude", "60.134254")
                queryParam("rightLongitude", "31.020739")
                queryParam("leftLongitude", "30.04442")
            }
            requestCountOnly(2, null) {
                queryParam("bottomLatitude", "59.651351")
                queryParam("topLatitude", "60.336023")
                queryParam("rightLongitude", "31.020739")
                queryParam("leftLongitude", "30.044426")
            }
            registerMapSearchWithOneOffer()
        }

        onScreen<FiltersScreen> {
            anySnackBarView()
                .waitUntil { isCompletelyDisplayed() }
                .invoke { action.click() }
        }

        onScreen<SearchParamsSuggestionsScreen> {
            listView.waitUntil {
                contains(viewportItem)
                contains(priceItem)
            }
            priceItem.view.click()
            submitButton.waitUntil { containsText(" 10 ") }
            viewportItem.view.click()
            submitButton.waitUntil { containsText(" 12 ") }
            priceItem.view.click()
            submitButton.waitUntil { containsText(" 2 ") }
            root.isViewStateMatches(screenshotPath)
            submitButton.click()
        }

        onScreen<SearchMapScreen> {
            mapView.waitUntil { isCompletelyDisplayed() }
            waitUntil { offersCountEquals(2, 2) }
        }
    }

    private fun setupTest(dispatcherReg: DispatcherRegistry.() -> Unit) {
        configureWebServer(dispatcherReg)
        activityTestRule.launchActivity()
    }

    private fun DispatcherRegistry.requestCountOnly(
        expectedTotalCount: Int,
        extendedNumber: Int?,
        requestParams: (RequestMatcher.Builder.() -> Unit)? = null
    ) {
        register(
            request {
                path("2.0/offers/number")
                requestParams?.let(this::apply)
            },
            response {
                setBody(
                    "{\"response\":{\"number\":$expectedTotalCount, " +
                        "\"extendedNumber\":$extendedNumber}}"
                )
            }
        )
    }

    private fun DispatcherRegistry.requestSearchSuggestions(responseFileName: String) {
        register(
            request {
                path("2.0/offers/number/extend-filters")
            },
            response {
                assetBody("searchParamsSuggestionTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.requestSearchSuggestionsError() {
        register(
            request {
                path("2.0/offers/number/extend-filters")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithOneOffer() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("searchParamsSuggestionTest/pointStatisticSearch.json")
            }
        )
    }
}
