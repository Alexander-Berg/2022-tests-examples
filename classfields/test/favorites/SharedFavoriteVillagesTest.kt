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
class SharedFavoriteVillagesTest {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.VILLAGE,
        objectIds = listOf(VILLAGE_ID),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowSharedVillages() {
        configureWebServer {
            registerVillage()
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            toolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(R.string.shared_favorites_villages)
            villageSnippet(VILLAGE_ID).waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerVillage() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("villageId", VILLAGE_ID)
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {

        private const val VILLAGE_ID = "2"
    }
}
