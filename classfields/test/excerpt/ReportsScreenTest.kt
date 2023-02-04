package com.yandex.mobile.realty.test.excerpt

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ReportsActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnAuthWebView
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnReportsScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 08.12.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ReportsScreenTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ReportsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun shouldOpenSearchWithExcerptReports() {
        configureWebServer {
            registerEmptyReports()
            registerOffers()
        }

        activityTestRule.launchActivity()

        performOnReportsScreen {
            waitUntil { isFullscreenEmptyViewShown() }

            performOnFullscreenEmpty {
                isViewStateMatches(
                    "/ReportsScreenTest/shouldOpenSearchWithExcerptReports/emptyScreen"
                )

                tapOn(lookup.matchesActionButton())
            }
        }

        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldLoadFirstPage() {
        configureWebServer {
            registerFirstReportsPage()
            registerSecondReportsPage()
        }

        activityTestRule.launchActivity()

        performOnReportsScreen {
            waitUntil { containsReportSnippet(FIRST_REPORT_ID) }
        }
    }

    @Test
    fun shouldLoadSecondPage() {
        configureWebServer {
            registerFirstReportsPage()
            registerSecondReportsPage()
        }

        activityTestRule.launchActivity()

        performOnReportsScreen {
            waitUntil { containsReportSnippet(FIRST_REPORT_ID) }

            containsReportSnippet("4")

            waitUntil { containsReportSnippet("5") }
        }
    }

    @Test
    fun shouldOpenReport() {
        configureWebServer {
            registerFirstReportsPage()
        }

        activityTestRule.launchActivity()

        performOnReportsScreen {
            waitUntil { containsReportSnippet(FIRST_REPORT_ID) }

            performOnReportSnippet(FIRST_REPORT_ID) {
                tapOn(lookup.matchesShowReportButton())
            }
        }

        performOnAuthWebView {
            waitUntil {
                isPageUrlEquals(
                    "https://m.realty.yandex.ru/egrn-report/" +
                        "$FIRST_REPORT_ID/?only-content=true"
                )
            }
        }
    }

    @Test
    fun shouldOpenOffer() {
        configureWebServer {
            registerFirstReportsPage()
            registerOffer()
        }

        activityTestRule.launchActivity()

        performOnReportsScreen {
            waitUntil { containsReportSnippet(FIRST_REPORT_ID) }

            performOnReportSnippet(FIRST_REPORT_ID) {
                tapOn(lookup.matchesShowOfferButton())
            }
        }

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("1 531 231 \u20BD") }
        }
    }

    private fun DispatcherRegistry.registerEmptyReports() {
        register(
            request {
                path("2.0/paid-report/user/me")
            },
            response {
                setBody(
                    """{
                                "response": {
                                    "paidReports": []
                                } 
                            }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerFirstReportsPage() {
        register(
            request {
                path("2.0/paid-report/user/me")
                queryParam("pageNum", "0")
            },
            response {
                assetBody("reportScreenTest/reportFirstPage.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSecondReportsPage() {
        register(
            request {
                path("2.0/paid-report/user/me")
                queryParam("pageNum", "1")
            },
            response {
                assetBody("reportScreenTest/reportsSecond.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
                queryParam("id", "3209306734197045248")
            },
            response {
                assetBody("cardWithViews.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("withExcerptsOnly", "YES")
            },
            response {
                assetBody("reportScreenTest/offerWithSiteSearch.json")
            }
        )
    }

    companion object {

        private const val FIRST_REPORT_ID = "1"
    }
}
