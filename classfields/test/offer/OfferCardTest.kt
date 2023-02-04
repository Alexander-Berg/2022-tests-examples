package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.DateRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrikeev on 30/07/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OfferCardTest {

    private var activityTestRule = OfferCardActivityTestRule(offerId = OFFER_ID, launchActivity = false)
    private val dateRule = DateRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        dateRule,
        MetricaEventsRule()
    )

    @Before
    fun setTestDate() {
        dateRule.setDate(2020, 12, 14)
    }

    @Test
    fun shouldMatchSellApartmentSecondaryScreenshots() {
        configureWebServer {
            registerSellSecondaryApartment()
            registerManualPosts()
        }
        activityTestRule.launchActivity()
        val cardShownMetricaEvent = event("Посмотреть карточку объявления") {
            "id" to OFFER_ID
            "Категория объявления" to jsonArrayOf("Sell", "SecondaryFlat_Sell")
        }

        performOnOfferCardScreen {
            waitUntil { cardShownMetricaEvent.isOccurred() }
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsExcerptBriefReportBlock() }
            waitUntil { containsArchiveError() }
            waitUntil { containsSimilarOffersError() }
            waitUntil { containsManualError() }
            scrollToTop()
            expandAppBar()

            isInitialStateMatches("/offerCard/sell/apartment/initialState")
            collapseAppBar()
            isContentMatches("/offerCard/sell/apartment/content")
        }
    }

    @Test
    fun shouldMatchSellNewApartmentScreenshots() {
        configureWebServer {
            registerSellNewApartment()
            registerManualPosts()
        }
        activityTestRule.launchActivity()
        val cardShownMetricaEvent = event("Посмотреть карточку объявления") {
            "id" to OFFER_ID
            "Категория объявления" to jsonArrayOf("Sell", "NewFlatSale_Primary", "NewFlatSale")
        }

        performOnOfferCardScreen {
            waitUntil { cardShownMetricaEvent.isOccurred() }
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()
            waitUntil { containsArchiveError() }
            waitUntil { containsSimilarOffersError() }
            waitUntil { containsManualError() }
            scrollToTop()
            expandAppBar()

            isInitialStateMatches("/offerCard/sell/newapartment/initialState")
            collapseAppBar()
            isContentMatches("/offerCard/sell/newapartment/content")
        }
    }

    private fun DispatcherRegistry.registerSellSecondaryApartment() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("offerCardTest/sellSecondaryApartmentOfferDetails.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSellNewApartment() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("offerCardTest/sellNewApartmentOfferDetails.json")
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

    companion object {

        private const val OFFER_ID = "0"
    }
}
