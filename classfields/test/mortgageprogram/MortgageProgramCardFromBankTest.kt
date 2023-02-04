package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createAlfaProgram
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
class MortgageProgramCardFromBankTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createAlfaProgram(CARD_PROGRAM_ID),
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
    fun shouldShowBankProgramsWithoutPaging() {
        configureWebServer {
            registerMortgageProgramBankSearch("bankProgramsOnePage.json")
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(PROGRAM_ID_1)
                .waitUntil { listView.contains(this) }

            checkBankProgramsTitleViewState()
            listView.contains(mortgageProgramSnippet(PROGRAM_ID_3))
            listView.doesNotContain(nextProgramsButton)
        }
    }

    @Test
    fun shouldShowBankProgramsWithPaging() {
        configureWebServer {
            registerMortgageProgramBankSearch("bankProgramsFirstPage.json", page = 0)
            registerMortgageProgramBankSearch("bankProgramsSecondPage.json", page = 1)
        }
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardFromBankTest/shouldShowBankProgramsWithPaging"

        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(PROGRAM_ID_1)
                .waitUntil { listView.contains(this) }
            checkBankProgramsTitleViewState()

            listView.contains(mortgageProgramSnippet(PROGRAM_ID_10))
            listView.doesNotContain(mortgageProgramSnippet(PROGRAM_ID_13))
            listView.scrollTo(nextProgramsButton)
                .isViewStateMatches("$prefix/nextPageButton")
                .click()

            mortgageProgramSnippet(PROGRAM_ID_13)
                .waitUntil { listView.contains(this) }
            listView.doesNotContain(nextProgramsButton)
        }
    }

    @Test
    fun shouldOpenMortgageProgram() {
        configureWebServer {
            registerMortgageProgramBankSearch("bankProgramsOnePage.json")
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            mortgageProgramSnippet(PROGRAM_ID_1)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<MortgageProgramCardScreen> {
            screenTitleItem(CARD_TITLE)
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowBankProgramsErrors() {
        configureWebServer {
            registerMortgageProgramBankSearchError(page = 0)
            registerMortgageProgramBankSearch("bankProgramsFirstPage.json", page = 0)
            registerMortgageProgramBankSearchError(page = 1)
            registerMortgageProgramBankSearch("bankProgramsSecondPage.json", page = 1)
        }
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardFromBankTest/shouldShowBankProgramsErrors"

        onScreen<MortgageProgramCardScreen> {
            bankProgramsErrorItem
                .waitUntil { listView.contains(this) }
                .also { checkBankProgramsTitleViewState() }
                .isViewStateMatches("$prefix/errorState")
                .click()

            mortgageProgramSnippet(PROGRAM_ID_10)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(nextProgramsButton)
                .click()

            bankProgramsErrorItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("$prefix/errorState")
                .click()

            mortgageProgramSnippet(PROGRAM_ID_13)
                .waitUntil { listView.contains(this) }
            listView.doesNotContain(bankProgramsErrorItem)
        }
    }

    private fun MortgageProgramCardScreen.checkBankProgramsTitleViewState() {
        listView.scrollTo(bankProgramsTitleItem)
            .isViewStateMatches("MortgageProgramCardFromBankTest/bankPrograms")
    }

    private fun DispatcherRegistry.registerMortgageProgramBankSearch(
        responseFileName: String,
        page: Int = 0
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("rgid", RGID)
                queryParam("page", page.toString())
                queryParam("pageSize", "10")
                queryParam("excludeMortgageProgramId", CARD_PROGRAM_ID)
                queryParam("bankId", BANK_ID)
            },
            response {
                assetBody("MortgageProgramCardFromBankTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramBankSearchError(page: Int = 0) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("rgid", RGID)
                queryParam("page", page.toString())
                queryParam("pageSize", "10")
                queryParam("excludeMortgageProgramId", CARD_PROGRAM_ID)
                queryParam("bankId", BANK_ID)
            },
            error()
        )
    }

    companion object {

        private const val RGID = "587795"
        private const val BANK_ID = "10"
        private const val PROGRAM_ID_1 = "1"
        private const val PROGRAM_ID_3 = "3"
        private const val PROGRAM_ID_10 = "10"
        private const val PROGRAM_ID_13 = "13"
        private const val CARD_PROGRAM_ID = "1"
        private const val CARD_TITLE = "Ипотека на готовое жильё"
    }
}
