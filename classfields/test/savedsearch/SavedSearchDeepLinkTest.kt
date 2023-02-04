package com.yandex.mobile.realty.test.savedsearch

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SplashActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnSavedSearchOffersScreen
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddSavedSearchesEntryStatement
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author rogovalex on 24/06/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SavedSearchDeepLinkTest {

    private val activityTestRule = SplashActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        DatabaseRule(
            createAddSavedSearchesEntryStatement(
                StoredSavedSearch.of(
                    "saved-search-3",
                    "Поиск",
                    Filter.SellApartment(),
                    GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
                ).apply {
                    subscriptionId = "123abc"
                    status = StoredSavedSearch.Status.OK
                }
            )
        ),
        activityTestRule
    )

    @Test
    fun shouldOpenSavedSearchStoredLocally() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                },
                response {
                    assetBody("savedSearchesTest/offerWithSiteSearch.json")
                }
            )
        }

        activityTestRule.launchActivity(
            Intent().apply {
                putExtra("subscriptionId", "123abc")
            }
        )

        performOnSavedSearchOffersScreen {
            waitUntil { containsOfferSnippet("1") }
        }
    }

    @Test
    fun shouldOpenSavedSearchStoredRemotely() {
        configureWebServer {
            register(
                request {
                    method("GET")
                    path("2.0/savedSearch")
                },
                response {
                    assetBody("savedSearchesTest/single1.json")
                }
            )
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                },
                response {
                    assetBody("savedSearchesTest/offerWithSiteSearch.json")
                }
            )
        }

        activityTestRule.launchActivity(
            Intent().apply {
                putExtra("subscriptionId", "173edfcd5e6adb820c3e1f94a879aa2dd9ad1883")
            }
        )

        performOnSavedSearchOffersScreen {
            waitUntil { containsOfferSnippet("1") }
        }
    }
}
