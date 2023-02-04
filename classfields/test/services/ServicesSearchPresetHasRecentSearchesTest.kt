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
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoObject
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.geo.GeoType
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.RecentSearch
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
class ServicesSearchPresetHasRecentSearchesTest : BaseTest() {

    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        DatabaseRule(
            DatabaseRule.createAddRecentSearchesEntryStatement(createRecentSearch()),
            DatabaseRule.createAddRecentSearchesEntryStatement(createRecentSearch())
        ),
        activityTestRule
    )

    @Test
    fun testPresetsConfiguration() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            searchCard
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("presetsHasRecentSearch"))
        }
    }

    companion object {

        private fun createRecentSearch(): RecentSearch {
            val geoObjects = List(8) { createGeoObject() }
            return RecentSearch(
                filter = Filter.SellApartment(),
                geoIntent = GeoIntent.Objects(geoObjects),
                region = GeoRegion.DEFAULT
            )
        }

        private fun createGeoObject(): GeoObject {
            return GeoObject(
                type = GeoType.METRO_STATION,
                fullName = "metro",
                shortName = "metro",
                scope = null,
                point = GeoPoint(10.0, 11.0),
                leftTop = GeoPoint(9.0, 10.0),
                rightBottom = GeoPoint(11.0, 12.0),
                colors = emptyList(),
                params = mapOf("param_key" to listOf("param_value"))
            )
        }
    }
}
