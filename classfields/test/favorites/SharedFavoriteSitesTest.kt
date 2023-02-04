package com.yandex.mobile.realty.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.ModelType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SharedFavoriteSitesTest {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.SITE,
        objectIds = listOf(SITE_ID),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowSharedSites() {
        configureWebServer {
            registerSite()
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            toolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(R.string.shared_favorites_sites)
            siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "1"
    }
}
