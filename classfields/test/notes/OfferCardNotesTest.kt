package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.NoteScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
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
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OfferCardNotesTest : NotesTest() {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldEditNoteInOffer() {
        configureWebServer {
            registerOfferWithNote()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }
            appBar.collapse()

            noteItem
                .waitUntil { listView.contains(this) }
                .apply {
                    isViewStateMatches("OfferCardNotesTest/shouldEditNoteInOffer/oldNote")
                }
                .click()
        }
        onScreen<NoteScreen> {
            inputView.clearText()
            inputView.typeText(TEXT)
            submitButton.click()
        }
        onScreen<OfferCardScreen> {
            waitUntil { noteText.isTextEquals(TEXT) }
        }
    }

    @Test
    fun shouldRemoveNoteInOffer() {
        configureWebServer {
            registerOfferWithNote()
            registerNoteRemoving(OFFER_ID)
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }
            appBar.collapse()

            noteItem
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<NoteScreen> {
            inputView.clearText()
            submitButton.click()
        }
        onScreen<OfferCardScreen> {
            waitUntil { listView.doesNotContain(noteItem) }
        }
    }

    @Test
    fun shouldAddNoteToSimilarOffer() {
        configureWebServer {
            registerOffer()
            registerSimilarOffers()
            registerNoteSaving(SIMILAR_OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsSimilarOfferSnippet(SIMILAR_OFFER_ID) }
            performOnSimilarOfferSnippet(SIMILAR_OFFER_ID) {
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
        performOnOfferCardScreen {
            waitUntil { containsSimilarOfferSnippet(SIMILAR_OFFER_ID) }
            performOnSimilarOfferSnippet(SIMILAR_OFFER_ID) {
                isNoteShown(TEXT)
            }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("callButtonTest/cardWithViewsLarge.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithNote() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("notesTest/cardWithViewsLarge.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("notesTest/similar.json")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SIMILAR_OFFER_ID = "1"
    }
}
