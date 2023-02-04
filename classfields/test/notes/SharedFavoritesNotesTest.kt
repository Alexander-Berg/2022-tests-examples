package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.NoteScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.ModelType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 02.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SharedFavoritesNotesTest : NotesTest() {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.OFFER,
        objectIds = listOf(OFFER_ID),
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
            registerOffers()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    menuButton.click()
                }
        }
        onScreen<OfferMenuDialogScreen> {
            addNoteButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<NoteScreen> {
            inputView.typeText(TEXT)
            submitButton.click()
        }
        onScreen<SharedFavoritesScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("SharedFavoritesNotesTest/snippetWithNote")
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("notesTest/offerWithSiteSearch.json")
            }
        )
    }
}
