package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SavedSearchOfferListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.robot.performOnSavedSearchOffersScreen
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddSavedSearchesEntryStatement
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.model.StoredSavedSearch
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.search.Filter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedSearchOfferListNotesTest : NotesTest() {

    private val search = createStoredSavedSearch()

    private val activityTestRule = SavedSearchOfferListActivityTestRule(
        SEARCH_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        DatabaseRule(createAddSavedSearchesEntryStatement(search)),
        activityTestRule
    )

    @Test
    fun shouldAddNote() {
        configureWebServer {
            registerOffers()
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnSavedSearchOffersScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
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
        performOnSavedSearchOffersScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                isNoteShown(TEXT)
            }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("notesTest/offerWithSiteSearch.json")
            }
        )
    }

    companion object {
        private const val SEARCH_ID = "a"

        fun createStoredSavedSearch(): StoredSavedSearch {
            return StoredSavedSearch.of(
                SEARCH_ID,
                "test",
                Filter.SellApartment(),
                GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
            )
        }
    }
}
