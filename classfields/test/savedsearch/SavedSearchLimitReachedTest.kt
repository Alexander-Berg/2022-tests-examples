package com.yandex.mobile.realty.test.savedsearch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnSaveSearchResultScreen
import com.yandex.mobile.realty.core.robot.performOnSaveSearchScreen
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddSavedSearchesEntryStatement
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.data.model.StoredSavedSearch
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.search.Filter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 26/06/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedSearchLimitReachedTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        DatabaseRule(
            createAddSavedSearchesEntryStatement(
                StoredSavedSearch.of(
                    "saved-search-3",
                    "Поиск3",
                    Filter.SellApartment(),
                    GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
                ).apply {
                    subscriptionId = "123abc"
                    status = StoredSavedSearch.Status.OK
                }
            ),
            createAddSavedSearchesEntryStatement(
                StoredSavedSearch.of(
                    "saved-search-4",
                    "Поиск4",
                    Filter.SellApartment(),
                    GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
                ).apply {
                    subscriptionId = "456edf"
                    status = StoredSavedSearch.Status.OK
                }
            )
        )
    )

    @Test
    fun shouldSaveSearchOnSearchList() {

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            subscribeButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnSaveSearchScreen {
            isNameEquals("Поиск по области карты")
            isGeoRegionButtonShown()
            isGeoDescriptionHidden()

            tapOn(lookup.matchesSaveSearchButton())
        }

        performOnSaveSearchResultScreen {
            waitUntil { isLimitReachedViewShown() }

            tapOn(lookup.matchesDoneButton())
        }

        onScreen<SearchListScreen> {
            listView.isCompletelyDisplayed()
        }
    }
}
