package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.JsonObject
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.robot.performOnSiteCardScreen
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 2020-03-11
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteCardTest : BaseTest() {

    private var activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Before
    fun createImages() {
        repeat(3) { index ->
            val name = "test_image_$index"
            createImageOnExternalDir(name, rColor = 0, gColor = 255, bColor = 0)
        }
    }

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun shouldShowSiteSummary() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatSummary.json")
        }

        activityTestRule.launchActivity()

        performOnSiteCardScreen {
            waitUntil { isSummaryEquals("Стройка заморожена, комфорт") }
        }
    }

    @Test
    fun shouldMatchSiteFreeCard() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfoFree.json")
            registerSitesFromDeveloper()
            registerSimilarSites()
            registerOfferStat()
            registerOfferStat()
            registerManualPosts()
            registerMortgageCalculatorParams()
            registerReviews()
        }

        activityTestRule.launchActivity()
        val cardShownMetricaEvent = event("Посмотреть карточку новостройки") {
            "id" to "site_$SITE_ID"
        }

        onScreen<SiteCardScreen> {
            waitUntil { cardShownMetricaEvent.isOccurred() }
            waitUntilCardLoaded()
            review(REVIEW_ID).waitUntil { listView.contains(this) }
            listView.scrollToTop()
            appBar.expand()

            isViewStateMatches(getTestRelatedFilePath("initialState"))
            appBar.collapse()
            isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldMatchSitePaidCard() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfo.json")
            registerSitesFromDeveloper()
            registerSimilarSites()
            registerOfferStat()
            registerOfferStat()
            registerManualPosts()
            registerMortgageCalculatorParams()
            registerReviews()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            waitUntilCardLoaded()
            review(REVIEW_ID).waitUntil { listView.contains(this) }
            listView.scrollToTop()
            appBar.expand()
            isViewStateMatches(getTestRelatedFilePath("initialState"))
            appBar.collapse()
            isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldMatchSiteExtendedCard() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfoExtended.json")
            registerSitesFromDeveloper()
            registerSimilarSites()
            registerOfferStat()
            registerOfferStat()
            registerManualPosts()
            registerMortgageCalculatorParams()
            registerReviews()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            waitUntilCardLoaded()
            decorationImagesItem.waitUntil { listView.contains(this) }
            review(REVIEW_ID).waitUntil { listView.contains(this) }
            listView.scrollToTop()
            appBar.expand()
            isViewStateMatches(getTestRelatedFilePath("initialState"))
            appBar.collapse()
            isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldMatchSiteLimitedCard() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllInfoLimited.json")
            registerSitesFromDeveloper()
            registerSimilarSites()
            registerOfferStat()
            registerOfferStat()
            registerManualPosts()
            registerMortgageCalculatorParams()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            waitUntilCardLoaded()
            listView.scrollToTop()
            appBar.expand()
            isViewStateMatches(getTestRelatedFilePath("initialState"))
            appBar.collapse()
            isContentStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldMatchShortcuts() {
        configureWebServer {
            registerSiteWithOfferStat("siteWithOfferStatAllShortcuts.json")
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            floatingCommButtons.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            shortcutsListView.waitUntil {
                shortcutsListView.contains(shortcutTour3d)
                shortcutsListView.contains(shortcutReview)
            }
            shortcutsListView.isContentStateMatches(getTestRelatedFilePath("allShortcuts"))
        }
    }

    private fun SiteCardScreen.waitUntilCardLoaded() {
        floatingCommButtons.waitUntil { isCompletelyDisplayed() }
        appBar.collapse()

        roomsFilterItem.waitUntil { listView.contains(this) }
        mortgageCalculationResultItem.waitUntil { listView.contains(this) }
        siteSnippet(SIMILAR_OFFER_ID).waitUntil { listView.contains(this) }
        siteSnippet(SITE_DEVELOPER_ID).waitUntil { listView.contains(this) }
        waitUntil {
            listView.contains(manualArticlesBlockItem)
            manualArticlesList.isCompletelyDisplayed()
        }
    }

    private fun DispatcherRegistry.registerSiteWithOfferStat(responseFileName: String) {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteCardTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerManualPosts() {
        register(
            request {
                method("GET")
                path("1.0/blog/posts")
            },
            response {
                assetBody("manualPosts.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("priceType", "PER_OFFER")
            },
            response {
                assetBody("siteCardTest/offerStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSitesFromDeveloper() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("developerId", DEVELOPER_ID)
                queryParam("excludeSiteId", SITE_ID)
                queryParam("pageSize", "5")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarSites() {
        register(
            request {
                method("GET")
                path("1.0/newbuilding/siteLikeSearch")
                queryParam("siteId", SITE_ID)
                queryParam("excludeSiteId", SITE_ID)
                queryParam("pageSize", "4")
            },
            response {
                assetBody("siteLikeSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageCalculatorParams() {
        register(
            request {
                method("GET")
                path("2.0/mortgage/calculator/regionalParameters")
            },
            response {
                assetBody("mortgageParams.json")
            }
        )
    }

    private fun DispatcherRegistry.registerReviews() {
        register(
            request {
                method("POST")
                path("2.0/graphql")
                jsonPartialBody {
                    "operationName" to "GetReviews"
                    "variables" to JsonObject().apply { addProperty("id", SITE_PERMALINK) }
                }
            },
            response {
                assetBody("siteCardTest/siteReviews.json")
            }
        )
    }

    companion object {
        private const val SITE_ID = "0"
        private const val SIMILAR_OFFER_ID = "2"
        private const val SITE_DEVELOPER_ID = "1"
        private const val DEVELOPER_ID = "52569"
        private const val REVIEW_ID = "B16Urs9-EwxdAtunIhYduI7Eqx6h1Pkb"
        private const val SITE_PERMALINK = "182242396448"
    }
}
