package com.yandex.mobile.realty.test.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
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
 * @author scrooge on 14.03.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapTest {

    private var activityTestRule = MainActivityTestRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowMyLocationButton() {
        performOnSearchMapScreen {
            waitUntil { isMyLocationShown() }
        }
    }

    @Test
    fun shouldOpenSearchList() {
        onScreen<SearchMapScreen> {
            switchViewModeButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<SearchListScreen> {
            listView.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenFilters() {
        onScreen<SearchMapScreen> {
            filterButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<FiltersScreen> {
            submitButton.isCompletelyDisplayed()
        }
    }
}
