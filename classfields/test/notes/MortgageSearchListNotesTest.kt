package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.NoteScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 3/25/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageSearchListNotesTest : NotesTest() {

    private val activityTestRule = MortgageProgramListActivityTestRule(
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
            registerOffersWithSiteSearch()
            registerNoteSaving(OFFER_ID, TEXT)
            registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
            registerMortgageProgramSearch(
                "mortgageProgramSearchDefault.json",
                rgid = RGID
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
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
                queryParam("page", "0")
            },
            response {
                assetBody("notesTest/offerWithSiteSearchDefaultSorting.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        page: Int = 0,
        rgid: String?,
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                rgid?.let { queryParam("rgid", it) }
                queryParam("page", page.toString())
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {

        private const val RGID = "587795"
        private const val OFFER_ID = "1"
    }
}
