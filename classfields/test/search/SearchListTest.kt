package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 2019-09-13
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListTest {

    private var activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun testSearchListEmptyViewShown() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                    excludeQueryParamKey("excludeRightLongitude")
                },
                response {
                    setBody(
                        "{\"response\": {" +
                            "   \"pager\": {" +
                            "       \"totalItems\": 0" +
                            "   }, " +
                            "   \"searchQuery\" : { " +
                            "       \"logQueryId\" : \"b7128fa3fad87a0c\"," +
                            "       \"url\": \"offerSearchV2.json\"" +
                            "    }, " +
                            "   \"timeStamp\": \"2020-02-03T09:07:02.980Z\"" +
                            "}}"
                    )
                }
            )
            register(
                request {
                    path("2.0/offers/number")
                    queryParamKey("excludeRightLongitude")
                },
                response {
                    setBody("{\"response\": {\"number\": 0}}")
                }
            )
            register(
                request {
                    path("1.0/dynamicBoundingBox")
                },
                response {
                    assetBody("dynamicBoundingBox.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            fullscreenEmptyView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun testSearchListFirstPageShown() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("offerWithSiteSearchPage0.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.contains(offerSnippet("1"))
            listView.contains(offerSnippet("2"))
            listView.contains(offerSnippet("3"))
            listView.contains(offerSnippet("4"))
        }
    }

    @Test
    fun testSearchListSecondPageShown() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("offerWithSiteSearchPage0.json")
                }
            )
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "1")
                },
                response {
                    assetBody("offerWithSiteSearchPage1.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.scrollTo(offerSnippet("4"))

            waitUntil { listView.contains(offerSnippet("5")) }
        }
    }

    @Test
    fun testSearchListFullscreenExpandSearchShown() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                    excludeQueryParamKey("excludeRightLongitude")
                },
                response {
                    setBody(
                        "{\"response\": {" +
                            "   \"pager\": {" +
                            "       \"totalItems\": 0" +
                            "   }, " +
                            "   \"searchQuery\" : { " +
                            "       \"logQueryId\" : \"b7128fa3fad87a0c\"," +
                            "       \"url\": \"offerSearchV2.json\"" +
                            "    }, " +
                            "   \"timeStamp\": \"2020-02-03T09:07:02.980Z\"" +
                            "}}"
                    )
                }
            )
            register(
                request {
                    path("2.0/offers/number")
                    queryParamKey("excludeRightLongitude")
                },
                response {
                    setBody("{\"response\": {\"number\": 1}}")
                }
            )
            register(
                request {
                    path("1.0/dynamicBoundingBox")
                },
                response {
                    assetBody("dynamicBoundingBox.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            fullscreenExpandSearchView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(
                    "SearchListTest/testSearchListFullscreenExpandSearchShown/expandSearch"
                )
        }
    }

    @Test
    fun testSearchListFirstPageAndExpandSearchShown() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                    excludeQueryParamKey("excludeRightLongitude")
                },
                response {
                    assetBody("offerWithSiteSearchPage0.json")
                }
            )
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "1")
                    excludeQueryParamKey("excludeRightLongitude")
                },
                response {
                    setBody(
                        "{\"response\": {" +
                            "   \"pager\": {" +
                            "       \"totalItems\": 0" +
                            "   }, " +
                            "   \"searchQuery\" : { " +
                            "       \"logQueryId\" : \"b7128fa3fad87a0c\"," +
                            "       \"url\": \"offerSearchV2.json\"" +
                            "    }, " +
                            "   \"timeStamp\": \"2020-02-03T09:07:02.980Z\"" +
                            "}}"
                    )
                }
            )
            register(
                request {
                    path("2.0/offers/number")
                    queryParamKey("excludeRightLongitude")
                },
                response {
                    setBody("{\"response\": {\"number\": 1}}")
                }
            )
            register(
                request {
                    path("1.0/dynamicBoundingBox")
                },
                response {
                    assetBody("dynamicBoundingBox.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.scrollTo(offerSnippet("4"))

            expandSearchButton
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(
                    "SearchListTest/testSearchListFirstPageAndExpandSearchShown" +
                        "/expandSearch"
                )
        }
    }

    @Test
    fun testSearchListFirstPageFailed() {
        activityTestRule.launchActivity()
        onScreen<SearchListScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches("SearchListTest/testSearchListFirstPageFailed/error")
        }
    }

    @Test
    fun testSearchListSecondPageFailed() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("offerWithSiteSearchPage0.json")
                }
            )
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.scrollTo(offerSnippet("4"))

            errorView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("SearchListTest/testSearchListSecondPageFailed/error")
        }
    }
}
