package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SiteResellerOffersTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.robot.performOnSiteResellerOffersScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteResellerOffersNotesTest : NotesTest() {

    private val activityTestRule = SiteResellerOffersTestRule(
        siteId = "1",
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldAddNote() {
        configureWebServer {
            registerOffersWithSiteSearch()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnSiteResellerOffersScreen {
            waitUntil { containsOffer(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
            }
        }
        performOnOfferMenuDialog {
            isAddNoteButtonShown()
            tapOn(lookup.matchesAddNoteButton())
        }
        performOnNoteScreen {
            typeText(lookup.matchesInputView(), TEXT)
            tapOn(lookup.matchesSubmitButton())
        }
        performOnSiteResellerOffersScreen {
            waitUntil { containsOffer(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                isNoteShown(TEXT)
            }
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

    companion object {

        private const val OFFER_ID = "1"
    }
}
