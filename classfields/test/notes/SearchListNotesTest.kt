package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.NoteScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 1/25/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListNotesTest : NotesTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldAddNote() {
        val dispatcher = DispatcherRegistry()
        dispatcher.registerOffers()
        dispatcher.registerNoteSaving(OFFER_ID, TEXT)
        val favoriteAddingRequest = dispatcher.registerFavoriteAdding()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches("/SearchListNotesTest/shouldAddNote/snippetWithoutNote")
                    menuButton.click()
                }
        }
        onScreen<OfferMenuDialogScreen> {
            addNoteButton.isCompletelyDisplayed()
            addNoteButton.click()
        }
        onScreen<NoteScreen> {
            closeKeyboard()
            isViewStateMatches("/SearchListNotesTest/shouldAddNote/emptyNote")
            inputView.typeText(TEXT)
            isViewStateMatches("/SearchListNotesTest/shouldAddNote/typedText")
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches("/SearchListNotesTest/shouldAddNote/snippetWithNote")
                }
            waitUntil { favoriteAddingRequest.isOccured() }
        }
    }

    @Test
    fun shouldEditNote() {
        configureWebServer {
            registerOfferWithNote(TEXT)
            registerNoteSaving(OFFER_ID, "$TEXT $PRESET")
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches("/SearchListNotesTest/shouldEditNote/snippetWithOldNote")
                    noteView.click()
                }
        }
        onScreen<NoteScreen> {
            closeKeyboard()
            isViewStateMatches("/SearchListNotesTest/shouldEditNote/oldText")
            presetView(PRESET)
                .apply { presetsListView.scrollTo(this) }
                .view
                .click()
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches("/SearchListNotesTest/shouldEditNote/snippetWithNewNote")
                }
        }
    }

    @Test
    fun shouldRemoveNote() {
        configureWebServer {
            registerOfferWithNote(TEXT)
            registerNoteRemoving(OFFER_ID)
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches("/SearchListNotesTest/shouldRemoveNote/snippetWithNote")
                    menuButton.click()
                }
        }
        onScreen<OfferMenuDialogScreen> {
            editNoteButton.isCompletelyDisplayed()
            editNoteButton.click()
        }
        onScreen<NoteScreen> {
            closeKeyboard()
            inputView.clearText()
            isViewStateMatches("/SearchListNotesTest/shouldRemoveNote/removedText")
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(
                        "/SearchListNotesTest/shouldRemoveNote/snippetWithoutNote"
                    )
                }
        }
    }

    @Test
    fun shouldShowSavingError() {
        configureWebServer {
            registerOffers()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { menuButton.click() }
        }
        onScreen<OfferMenuDialogScreen> {
            addNoteButton.click()
        }
        onScreen<NoteScreen> {
            inputView.typeText(TEXT)
            submitButton.click()

            toastView("Не удалось добавить заметку. Попробуйте позже.").isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldShowRemovingError() {
        configureWebServer {
            registerOfferWithNote(TEXT)
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { noteView.click() }
        }
        onScreen<NoteScreen> {
            inputView.clearText()
            submitButton.click()

            toastView("Не удалось удалить заметку. Попробуйте позже.").isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldReturnWhenBackPressed() {
        configureWebServer {
            registerOfferWithNote(TEXT)
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { noteView.click() }
        }
        onScreen<NoteScreen> {
            backButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(
                        "/SearchListNotesTest/shouldReturnWhenBackPressed/snippetWithNote"
                    )
                }
        }
    }

    @Test
    fun shouldHideAddNoteButtonWhenOfferInactive() {
        configureWebServer {
            registerInactiveOffer()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { menuButton.click() }
        }
        onScreen<OfferMenuDialogScreen> {
            addNoteButton.doesNotExist()
        }
    }

    @Test
    fun shouldHideNoteWhenOfferInactive() {
        configureWebServer {
            registerInactiveOfferWithNote()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { noteView.isHidden() }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("notesTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithNote(note: String) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                setBody(
                    """
                        {
                            "response": {
                                "offers": {
                                    "items": [
                                        {
                                            "offerId": "$OFFER_ID",
                                            "offerType": "SELL",
                                            "offerCategory": "APARTMENT",
                                            "active": true,
                                            "userNote": "$note",
                                            "price": {
                                                "trend": "UNCHANGED",
                                                "price": {
                                                    "value": 14990000,
                                                    "currency": "RUB",
                                                    "priceType": "PER_OFFER",
                                                    "pricingPeriod": "WHOLE_LIFE"
                                                }
                                            }
                                        }
                                    ]
                                },
                                "pager": {
                                    "totalItems": 1
                                },
                                "searchQuery": {
                                    "logQueryId" : "b7128fa3fad87a0c",
                                    "url" : "offerSearchV2.json"
                                },
                                "timeStamp": "2020-02-03T09:07:02.980Z"
                            }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerInactiveOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                setBody(
                    """
                        {
                            "response": {
                                "offers": {
                                    "items": [
                                        {
                                            "offerId": "$OFFER_ID",
                                            "offerType": "SELL",
                                            "offerCategory": "APARTMENT",
                                            "active": false,
                                            "price": {
                                                "trend": "UNCHANGED",
                                                "price": {
                                                    "value": 14990000,
                                                    "currency": "RUB",
                                                    "priceType": "PER_OFFER",
                                                    "pricingPeriod": "WHOLE_LIFE"
                                                }
                                            }
                                        }
                                    ]
                                },
                                "pager": {
                                    "totalItems": 1
                                },
                                "searchQuery": {
                                    "logQueryId" : "b7128fa3fad87a0c",
                                    "url" : "offerSearchV2.json"
                                },
                                "timeStamp": "2020-02-03T09:07:02.980Z"
                            }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerInactiveOfferWithNote() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                setBody(
                    """
                        {
                            "response": {
                                "offers": {
                                    "items": [
                                        {
                                            "offerId": "$OFFER_ID",
                                            "offerType": "SELL",
                                            "offerCategory": "APARTMENT",
                                            "active": false,
                                            "userNote": "$TEXT",
                                            "price": {
                                                "trend": "UNCHANGED",
                                                "price": {
                                                    "value": 14990000,
                                                    "currency": "RUB",
                                                    "priceType": "PER_OFFER",
                                                    "pricingPeriod": "WHOLE_LIFE"
                                                }
                                            }
                                        }
                                    ]
                                },
                                "pager": {
                                    "totalItems": 1
                                },
                                "searchQuery": {
                                    "logQueryId" : "b7128fa3fad87a0c",
                                    "url" : "offerSearchV2.json"
                                },
                                "timeStamp": "2020-02-03T09:07:02.980Z"
                            }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFavoriteAdding(): ExpectedRequest {
        return register(
            request {
                path("1.0/favorites.json")
                body("""{"add":["$OFFER_ID"]}""")
            },
            response {
                setBody(
                    """{
                              "response" : {
                                "actual" : [ "$OFFER_ID" ],
                                "outdated" : [ ],
                                "relevant" : [ "$OFFER_ID" ]
                              }
                            }"""
                )
            }
        )
    }
}
