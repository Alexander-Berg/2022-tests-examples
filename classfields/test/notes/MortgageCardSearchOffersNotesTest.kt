package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.NoteScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createSecondaryFlatProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 03.08.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageCardSearchOffersNotesTest : NotesTest() {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createSecondaryFlatProgram(),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldAddNote() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
            registerOffersWithSiteSearch()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("NotesTest/snippetWithoutNote")
                .menuButton
                .click()
        }
        onScreen<OfferMenuDialogScreen> {
            addNoteButton.waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<NoteScreen> {
            inputView.typeText(TEXT)
            submitButton.click()
        }
        onScreen<MortgageProgramListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("NotesTest/snippetWithNote")
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("notesTest/offerWithSiteSearchDefaultSorting.json")
            }
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
        private const val OFFER_ID = "1"
    }
}
