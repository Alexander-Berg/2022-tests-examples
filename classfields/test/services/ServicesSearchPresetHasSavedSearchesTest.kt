package com.yandex.mobile.realty.test.services

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.data.model.StoredSavedSearch
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author pvl-zolotov on 22.04.2022
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ServicesSearchPresetHasSavedSearchesTest : BaseTest() {

    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        DatabaseRule(
            DatabaseRule.createAddSavedSearchesEntryStatement(createStoredSavedSearch())
        ),
        activityTestRule
    )

    @Test
    fun testPresetsConfiguration() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            searchCard
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("presetsHasSavedSearch"))
        }
    }

    companion object {

        private const val SEARCH_ID = "abc"

        fun createStoredSavedSearch(): StoredSavedSearch {
            return StoredSavedSearch.of(
                SEARCH_ID,
                "test",
                Filter.SellHouse(),
                GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
            )
        }
    }
}
