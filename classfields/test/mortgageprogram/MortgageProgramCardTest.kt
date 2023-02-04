package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageApplicationFormScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createStandardProgram
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 6/22/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createStandardProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule
    )

    @Before
    fun createImages() {
        createImageOnExternalDir(name = "test_image_0", rColor = 0, gColor = 255, bColor = 0)
    }

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun shouldShowStaticInfo() {
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            listView.isItemsStateMatches(
                key = "MortgageProgramCardTest/shouldShowStaticInfo/topInfo",
                fromItem = bankLogoItem,
                toItem = calculatorTitleItem,
                inclusive = false
            )

            listView.scrollTo(bankDisclaimerItem)
                .isViewStateMatches(
                    "MortgageProgramCardTest/shouldShowStaticInfo/bankDisclaimer"
                )

            listView.scrollTo(mortgageDisclaimerItem)
                .isViewStateMatches(
                    "MortgageProgramCardTest/shouldShowStaticInfo/mortgageDisclaimer"
                )
        }
    }

    @Test
    fun shouldShowExpandableSections() {
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            val prefix = "MortgageProgramCardTest/shouldShowExpandableSections"
            listView.isItemsStateMatches(
                key = "$prefix/expandedRateConditions",
                fromItem = rateConditionsTitleItem,
                toItem = documentsTitleItem,
                inclusive = false
            )
            listView.scrollTo(rateConditionsTitleItem).click()
            listView.isItemsStateMatches(
                key = "$prefix/collapsedRateConditions",
                fromItem = rateConditionsTitleItem,
                toItem = documentsTitleItem,
                inclusive = false
            )

            listView.isItemsStateMatches(
                key = "$prefix/collapsedDocuments",
                fromItem = documentsTitleItem,
                toItem = additionalConditionsTitleItem,
                inclusive = false
            )
            listView.scrollTo(documentsTitleItem).click()
            listView.isItemsStateMatches(
                key = "$prefix/expandedDocuments",
                fromItem = documentsTitleItem,
                toItem = additionalConditionsTitleItem,
                inclusive = false
            )

            listView.isItemsStateMatches(
                key = "$prefix/collapsedAdditionalConditions",
                fromItem = additionalConditionsTitleItem,
                toItem = similarProgramsTitleItem,
                inclusive = false
            )
            listView.scrollTo(additionalConditionsTitleItem).click()
            listView.isItemsStateMatches(
                key = "$prefix/expandedAdditionalConditions",
                fromItem = additionalConditionsTitleItem,
                toItem = similarProgramsTitleItem,
                inclusive = false
            )
        }
    }

    @Test
    fun shouldShowSimilarPrograms() {
        configureWebServer {
            registerSimilarPrograms()
        }
        activityTestRule.launchActivity()
        onScreen<MortgageProgramCardScreen> {
            waitUntil { listView.contains(similarProgramsTitleItem) }
            mortgageProgramSnippet(SIMILAR_PROGRAM_ID)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<MortgageProgramCardScreen> {
            screenTitleItem(SIMILAR_PROGRAM_TITLE).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldOpenFormsInSimilarPrograms() {
        configureWebServer {
            registerSimilarPrograms()
            registerCalculatorConfig()
            registerCalculatorResult()
        }
        activityTestRule.launchActivity()
        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(ID_ALFA_INTEGRATION)
                .waitUntil { listView.contains(this) }
                .formalizeButton
                .click()
        }
        onScreen<WebViewScreen> {
            waitUntil { webView.isCompletelyDisplayed() }
            pressBack()
        }
        onScreen<MortgageProgramCardScreen> {
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
    fun shouldShowErrorForSimilarPrograms() {
        configureWebServer {
            registerSimilarProgramsError()
            registerSimilarPrograms()
        }
        activityTestRule.launchActivity()
        onScreen<MortgageProgramCardScreen> {
            similarProgramsErrorItem.waitUntil { listView.contains(this) }
            listView.contains(similarProgramsTitleItem)
            similarProgramsErrorItem.view.click()
            mortgageProgramSnippet(SIMILAR_PROGRAM_ID).waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<MortgageProgramCardScreen> {
            screenTitleItem(SIMILAR_PROGRAM_TITLE).waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerSimilarPrograms() {
        registerSimilarPrograms(
            response {
                assetBody("mortgage/programSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarProgramsError() {
        registerSimilarPrograms(response { error() })
    }

    private fun DispatcherRegistry.registerSimilarPrograms(response: MockResponse) {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/similar")
            },
            response
        )
    }

    private fun DispatcherRegistry.registerCalculatorConfig() {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/calculator")
            },
            response {
                assetBody("mortgage/calculatorConfig.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorResult() {
        register(
            request {
                path("2.0/mortgage/program/$PROGRAM_ID/calculator")
            },
            response {
                assetBody("mortgage/calculatorResultDefault.json")
            }
        )
    }

    companion object {

        private const val PROGRAM_ID = "1"
        private const val ID_ALFA_INTEGRATION = "10"
        private const val ID_NATIVE_INTEGRATION = "11"
        private const val SIMILAR_PROGRAM_ID = "12"

        private const val SIMILAR_PROGRAM_TITLE = "Приобретение готового жилья"
    }
}
