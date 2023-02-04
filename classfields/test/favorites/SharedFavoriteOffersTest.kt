package com.yandex.mobile.realty.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
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
class SharedFavoriteOffersTest {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.OFFER,
        objectIds = firstPageIds + secondPageIds,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldAddAllToFavorite() {
        val dispatcher = DispatcherRegistry()
        dispatcher.registerFirstPage()
        val syncRequest = dispatcher.registerFavoriteSync()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            infoView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("SharedFavoriteOffersTest/sharedOffersInfo")

            addAllButton.click()

            waitUntil { syncRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowSharedOffersFirstAndSecondPage() {
        configureWebServer {
            registerFirstPage()
            registerSecondPage()
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            offerSnippet(firstPageIds.first()).waitUntil { listView.contains(this) }

            listView.scrollTo(offerSnippet(firstPageIds.last()))

            offerSnippet(secondPageIds.first()).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowSharedOffersFirstPageError() {
        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            fullscreenErrorView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches("SharedFavoriteOffersTest/error")
        }
    }

    @Test
    fun shouldShowSharedOffersSecondPageError() {
        configureWebServer {
            registerFirstPage()
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            offerSnippet(firstPageIds.first()).waitUntil { listView.contains(this) }

            listView.scrollTo(offerSnippet(firstPageIds.last()))

            errorView.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowEmptyScreen() {
        configureWebServer {
            registerEmptyOffers()
            registerEmptyOffers()
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            fullscreenEmptyView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches("SharedFavoriteOffersTest/empty")
        }
    }

    private fun DispatcherRegistry.registerFavoriteSync(): ExpectedRequest {
        val idsList = (firstPageIds + secondPageIds).joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]",
            transform = { "\"$it\"" }
        )
        return register(
            request {
                path("1.0/favorites.json")
                body("""{ "add": $idsList }""")
            },
            response {
                setBody(
                    """
                        {
                            "response": {
                                "actual" : $idsList,
                                "outdated" : [ ],
                                "relevant" : $idsList
                            }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFirstPage() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                firstPageIds.forEach { id ->
                    queryParam("offerId", id)
                }
            },
            response {
                assetBody("SharedFavoriteOffersTest/offerWithSiteSearchPage0.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSecondPage() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                secondPageIds.forEach { id ->
                    queryParam("offerId", id)
                }
            },
            response {
                assetBody("SharedFavoriteOffersTest/offerWithSiteSearchPage1.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                setBody(
                    """{
                            "response": {
                                "pager": {
                                    "totalItems": 0
                                },
                                "searchQuery": {
                                    "logQueryId" : "b7128fa3fad87a0c",
                                    "url" : "offerSearchV2.json"
                                },
                                "timeStamp": "2020-02-03T09:07:02.980Z"
                            }
                        }"""
                )
            }
        )
    }

    companion object {

        private val firstPageIds = (0..9).map { it.toString() }
        private val secondPageIds = listOf("10")
    }
}
