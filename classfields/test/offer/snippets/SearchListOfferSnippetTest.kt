package com.yandex.mobile.realty.test.offer.snippets

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
 * @author solovevai on 08.09.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListOfferSnippetTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldShowFullTrustedOwnerBadgeSellApartment() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("searchListOfferSnippetTest/sellApartmentFullTrustedOwner.json")
                }
            )
        }

        activityTestRule.launchActivity()

        val fileName = "SearchListOfferSnippetTest/sellApartmentFullTrustedOwner"

        onScreen<SearchListScreen> {
            offerSnippet("1")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(fileName)
                }
        }
    }

    @Test
    fun shouldShowFullTrustedOwnerBadgeRentLongApartment() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody(
                        "searchListOfferSnippetTest/rentLongApartmentFullTrustedOwner.json"
                    )
                }
            )
        }

        activityTestRule.launchActivity()

        val fileName = "SearchListOfferSnippetTest/rentLongApartmentFullTrustedOwner"

        onScreen<SearchListScreen> {
            offerSnippet("1")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(fileName)
                }
        }
    }

    @Test
    fun shouldNotShowOwnerBadgeWhenAuthorIsOwner() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody(
                        "searchListOfferSnippetTest/sellApartmentNotFullTrustedOwner.json"
                    )
                }
            )
        }

        activityTestRule.launchActivity()

        val fileName = "SearchListOfferSnippetTest/sellApartmentNotFullTrustedOwner"

        onScreen<SearchListScreen> {
            offerSnippet("1")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(fileName)
                }
        }
    }

    @Test
    fun shouldShowYandexRentBadgeRentLongApartment() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("searchListOfferSnippetTest/rentLongApartmentYandexRent.json")
                }
            )
        }

        activityTestRule.launchActivity()

        val fileName = "SearchListOfferSnippetTest/rentLongApartmentYandexRent"

        onScreen<SearchListScreen> {
            offerSnippet("1")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(fileName)
                }
        }
    }

    @Test
    fun shouldShowVirtualTourBadgeRentLongApartment() {
        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("page", "0")
                },
                response {
                    assetBody("searchListOfferSnippetTest/rentLongApartmentVirtualTour.json")
                }
            )
        }

        activityTestRule.launchActivity()

        val fileName = "SearchListOfferSnippetTest/rentLongApartmentVirtualTour"

        onScreen<SearchListScreen> {
            offerSnippet("1")
                .waitUntil { listView.contains(this) }
                .invoke {
                    isViewStateMatches(fileName)
                }
        }
    }
}
