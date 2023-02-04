package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BankSuggestScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 09.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageBankSuggestTest {

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
        activityTestRule
    )

    @Test
    fun shouldShowBankSuggest() {

        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch("mortgageProgramSearch5.json")
            registerBankSearch("bankSearch.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearch1.json",
                bankIds = listOf(BANK_ID_1, BANK_ID_2, BANK_ID_3)
            )
            registerMortgageProgramSearch("mortgageProgramSearch2.json")
            registerBankSearch("bankSearch.json")
            registerMortgageProgramSearch(
                responseFileName = "mortgageProgramSearch3.json",
                bankIds = listOf(BANK_ID_1)
            )
            registerMortgageProgramSearch("mortgageProgramSearch4.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()

            banksItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            onScreen<BankSuggestScreen> {
                bankSuggest(BANK_NAME_1).waitUntil { listView.contains(this) }

                isViewStateMatches(
                    "/MortgageBankSuggestTest/shouldShowBankSuggest/initialState"
                )

                searchView.typeText("Test bank")

                listView.scrollTo(bankSuggest(BANK_NAME_1)).click()
                listView.scrollTo(bankSuggest(BANK_NAME_2)).click()
                listView.scrollTo(bankSuggest(BANK_NAME_3)).click()

                isViewStateMatches(
                    "/MortgageBankSuggestTest/shouldShowBankSuggest/selectedState"
                )
                submitButton.click()
            }
            mortgageProgramSnippet(PROGRAM_ID_1).waitUntil { listView.contains(this) }
            listView.scrollTo(banksItem)
                .isViewStateMatches(
                    "/MortgageBankSuggestTest/shouldShowBankSuggest/bankField1"
                )
            banksResetButton.click()
            mortgageProgramSnippet(PROGRAM_ID_2).waitUntil { listView.contains(this) }
            listView.scrollTo(banksItem)
                .isViewStateMatches(
                    "/MortgageBankSuggestTest/shouldShowBankSuggest/bankField2"
                )
                .click()
            onScreen<BankSuggestScreen> {
                bankSuggest(BANK_NAME_1).waitUntil { listView.contains(this) }
                    .click()
                submitButton.click()
            }
            mortgageProgramSnippet(PROGRAM_ID_3).waitUntil { listView.contains(this) }
            resetButton.click()
            mortgageProgramSnippet(PROGRAM_ID_4).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowEmptyState() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearch1.json")
            registerBankSearch(responseFileName = "bankSearchEmpty.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()

            banksItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            onScreen<BankSuggestScreen> {
                emptyItem.waitUntil { listView.contains(this) }
                isViewStateMatches(
                    "/MortgageBankSuggestTest/shouldShowEmptyState/emptyState"
                )
            }
        }
    }

    @Test
    fun shouldShowErrorState() {
        configureWebServer {
            registerInitialParamsSearch("mortgageProgramInitialParams.json")
            registerMortgageProgramSearch(responseFileName = "mortgageProgramSearch1.json")
            registerBankSearch(response = error())
            registerBankSearch(responseFileName = "bankSearchEmpty.json")
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            filterExpanderItem.waitUntil { listView.contains(this) }
                .click()

            banksItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            onScreen<BankSuggestScreen> {
                errorItem.waitUntil { listView.contains(this) }
                isViewStateMatches(
                    "/MortgageBankSuggestTest/shouldShowErrorState/errorState"
                )

                errorItem.view.click()
                emptyItem.waitUntil { listView.contains(this) }
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
                assetBody("MortgageBankSuggestTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        bankIds: List<String>? = null,
        page: Int = 0
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("rgid", RGID)
                queryParam("page", page.toString())
                bankIds?.forEach { queryParam("bankId", it) }
            },
            response {
                assetBody("MortgageBankSuggestTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerBankSearch(
        responseFileName: String
    ) {
        registerBankSearch(
            response {
                assetBody("MortgageBankSuggestTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerBankSearch(
        response: MockResponse
    ) {
        register(
            request {
                path("2.0/bank/search")
                queryParam("rgid", RGID)
                queryParam("hasMortgagePrograms", "YES")
            },
            response
        )
    }

    companion object {

        private const val RGID = "587795"

        private const val PROGRAM_ID_1 = "1"
        private const val PROGRAM_ID_2 = "2"
        private const val PROGRAM_ID_3 = "3"
        private const val PROGRAM_ID_4 = "4"

        private const val BANK_NAME_1 = "Test bank 1"
        private const val BANK_NAME_2 = "Test bank 2"
        private const val BANK_NAME_3 = "Test bank 3"

        private const val BANK_ID_1 = "13"
        private const val BANK_ID_2 = "14"
        private const val BANK_ID_3 = "15"
    }
}
