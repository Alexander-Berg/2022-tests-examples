package ru.auto.ara.screenshotTests.auction.frontlog_tests

import org.junit.Rule
import org.junit.Test
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.auctions.c2bApplicationInfo
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplication
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplicationMobileTexts
import ru.auto.ara.core.dispatchers.frontlog.checkAuctionFrontLog
import ru.auto.ara.core.dispatchers.frontlog.postFrontLog
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.auction.performAuctionBuyout
import ru.auto.ara.core.robot.auction.performAuctionForm
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.network.scala.auction.NWAuctionInfo
import ru.auto.data.model.network.scala.offer.NWOffer
import ru.auto.data.model.network.scala.stats.NWPriceRange
import ru.auto.experiments.Experiments
import ru.auto.experiments.auctionDisplaying
import ru.auto.experiments.c2bAuctionFlow

class AuctionFrontlogFromOfferDetailsTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val experiments = experimentsOf {
        Experiments::auctionDisplaying then "show_before_price"
        Experiments::c2bAuctionFlow then "auction_flow_v1"
    }

    private val webServerRule = WebServerRule {
        userSetup()
        c2bApplicationInfo()
        c2bAuctionApplicationMobileTexts()
        c2bAuctionApplication()
        getOffer(
            offerId = OFFER_ID,
            category = "cars",
            mapper = {
                copy(offer = this.offer?.addC2Auction())
            }
        )
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        SetupAuthRule(),
        arguments = TestMainModuleArguments(
            testExperiments = experiments,
        )
    )

    @Test
    fun shouldLogFromOfferDetails() {
        webServerRule.routing {
            checkAuctionFrontLog("PAGE_OFFER_OWNER")
        }
        openOfferCard()
        performOfferCard {
            scrollToAuctionBanner()
            clickOnAuctionLeaveRequestButton()
        }
        performAuctionBuyout {
            clickLeaveRequestButton()
        }
        performAuctionForm {
            clickCrateAuctionRequest()
        }
    }

    @Test
    fun shouldLogBuyoutShownOnEachItemScroll() {
        webServerRule.routing {
            postFrontLog("buyout_show").watch {
                checkRequestsCount(2)
            }
        }
        openOfferCard()
        performOfferCard {
            scrollToBottom()
            waitSomething(1)
            smoothScrollToTop()
            waitSomething(1)
            scrollToBottom()
        }
    }

    private fun openOfferCard() {
        activityRule.launchDeepLinkActivity(String.format("https://auto.ru/%s/used/sale/%s", "cars", OFFER_ID))
        checkOfferCard { isOfferCard() }
    }

    companion object {
        private const val OFFER_ID = "1113808894-b0e4846d"
    }
}

fun NWOffer.addC2Auction(priceFrom: Long = 1000000, priceTo: Long = 2000000) = this.copy(
    state = this.state?.copy(
        c2b_auction_info = NWAuctionInfo(
            can_apply = true,
            price_range = NWPriceRange(
                from = priceFrom,
                to = priceTo,
                currency = "RUR"
            )
        )
    )
)

fun NWOffer.removeC2Auction() = this.copy(
    state = this.state?.copy(
        c2b_auction_info = null
    )
)
