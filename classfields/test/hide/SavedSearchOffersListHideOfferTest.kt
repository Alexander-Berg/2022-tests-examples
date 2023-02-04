package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SavedSearchOfferListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.robot.performOnSavedSearchOffersScreen
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddSavedSearchesEntryStatement
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
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
 * @author andrikeev on 19/01/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedSearchOffersListHideOfferTest {

    private val search = createStoredSavedSearch()

    private val activityTestRule = SavedSearchOfferListActivityTestRule(
        SEARCH_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        DatabaseRule(createAddSavedSearchesEntryStatement(search))
    )

    @Test
    fun shouldHideOfferWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerOffers()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        performOnSavedSearchOffersScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
                performOnOfferMenuDialog {
                    isHideButtonShown()
                    tapOn(lookup.matchesHideButton())
                    performOnConfirmationDialog {
                        isTitleEquals(getResourceString(R.string.hide_offer_confirmation_title))
                        isMessageEquals(getResourceString(R.string.hide_offer_confirmation))
                        confirm()
                    }
                }
            }
            waitUntil { doesNotContainsOffer(OFFER_ID) }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("hideOfferTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerHideOffer() {
        register(
            request {
                path("1.0/user/me/personalization/hideOffers")
                queryParam("offerId", OFFER_ID)
            },
            response {
                setBody("{}")
            }
        )
    }

    companion object {
        private const val SEARCH_ID = "a"
        private const val OFFER_ID = "1"

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
