package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnSaveSearchScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 2019-09-27
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListTransitionsTest {

    private var activityTestRule = MainActivityTestRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldOpenSaveSearch() {
        onScreen<SearchListScreen> {
            subscribeButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnSaveSearchScreen {
            isSaveSearchShown()
        }
    }

    @Test
    fun testSearchListToMap() {
        onScreen<SearchListScreen> {
            switchViewModeButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<SearchMapScreen> {
            mapView.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenFilters() {
        onScreen<SearchListScreen> {
            filterButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<FiltersScreen> {
            submitButton.isCompletelyDisplayed()
        }
    }
}
