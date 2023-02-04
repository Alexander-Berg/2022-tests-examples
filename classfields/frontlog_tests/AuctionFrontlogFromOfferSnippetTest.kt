package ru.auto.ara.screenshotTests.auction.frontlog_tests

import org.junit.Rule
import org.junit.Test
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.auctions.c2bApplicationInfo
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplication
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplicationMobileTexts
import ru.auto.ara.core.dispatchers.frontlog.checkAuctionFrontLog
import ru.auto.ara.core.dispatchers.frontlog.postFrontLog
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.getMultipleActiveUserOffers
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.auction.performAuctionBuyout
import ru.auto.ara.core.robot.auction.performAuctionForm
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import ru.auto.experiments.Experiments
import ru.auto.experiments.auctionDisplaying
import ru.auto.experiments.c2bAuctionFlow

class AuctionFrontlogFromOfferSnippetTest {

    val activityRule = activityScenarioRule<MainActivity>()

    private val experiments = experimentsOf {
        Experiments::auctionDisplaying then "show_before_price"
        Experiments::c2bAuctionFlow then "auction_flow_v1"
    }

    private val webServerRule = WebServerRule {
        userSetup()
        getMultipleActiveUserOffers(
            mapper = {
                copy(
                    offers = offers?.map({ it.addC2Auction() })
                )
            }
        )
        c2bApplicationInfo()
        c2bAuctionApplicationMobileTexts()
        c2bAuctionApplication()
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
    fun shouldLogFromOfferSnippet() {
        webServerRule.routing {
            checkAuctionFrontLog("PAGE_LK")
        }
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            scrollToOfferSnippet(0)
            clickCreateAuctionRequest(0)
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
                checkRequestsCount(3)
            }
        }
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForOfferSnippets(2)
            scrollToBottom()
            waitSomething(2)
            scrollToTop()
        }
    }
}
