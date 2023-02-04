package ru.auto.ara.screenshotTests.auction.frontlog_tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.auctions.c2bApplicationInfo
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplication
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplicationMobileTexts
import ru.auto.ara.core.dispatchers.draft.DraftDispatcherArgs
import ru.auto.ara.core.dispatchers.draft.PostDraftPublishDispatcher
import ru.auto.ara.core.dispatchers.draft.getDraft
import ru.auto.ara.core.dispatchers.draft.putDraft
import ru.auto.ara.core.dispatchers.frontlog.checkAuctionFrontLog
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersParametrizedDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PostActivateUserOffersDispatcher
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.auction.performAuctionBuyout
import ru.auto.ara.core.robot.auction.performAuctionForm
import ru.auto.ara.core.robot.full_draft.performFullDraftVas
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.Routing
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTestEnvironmentRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import ru.auto.data.util.TEST_DRAFT_AND_WIZARD_WITH_MOCKS
import ru.auto.data.util.TEST_WIZARD_AUCTION_SHOW_BEFORE_PRICE
import ru.auto.data.util.TEST_WIZARD_IS_AUCTION_AVAILABLE_FOR_OFFER
import ru.auto.experiments.Experiments
import ru.auto.experiments.auctionDisplaying
import ru.auto.experiments.c2bAuctionFlow

@RunWith(AndroidJUnit4::class)
class AuctionFrontlogFromDraftTest {

    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    private var draftDispatcherArgs = DraftDispatcherArgs(draftResponseOfferId = DRAFT_OFFER_ID)

    private val webServerRule = WebServerRule {
        userSetup()
        c2bApplicationInfo()
        c2bAuctionApplicationMobileTexts()
        c2bAuctionApplication()
        delegateDispatchers(
            PostDraftPublishDispatcher(
                status = "ACTIVE",
                isActivationFree = true,
            ),
            GetUserOffersParametrizedDispatcher(
                watcher = null,
                category = "(cars|all)",
                withEmptyResponse = false
            ),
            PostActivateUserOffersDispatcher(
                offerId = DRAFT_OFFER_ID,
            )
        )
    }

    private val experiments = experimentsOf {
        Experiments::auctionDisplaying then "show_before_price"
        Experiments::c2bAuctionFlow then "auction_flow_v1"
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule(),
        SetupTestEnvironmentRule { test ->
            TEST_DRAFT_AND_WIZARD_WITH_MOCKS = test
            TEST_WIZARD_AUCTION_SHOW_BEFORE_PRICE = test
            TEST_WIZARD_IS_AUCTION_AVAILABLE_FOR_OFFER = test
        },
        ImmediateImageLoaderRule { url ->
            ImmediateImageLoaderRule.mockPhotosPredicate(url) || url.contains("yastatic.net")
        },
        arguments = TestMainModuleArguments(experiments)
    )

    @Test
    fun shouldLogFromDraft() {
        webServerRule.routing {
            checkAuctionFrontLog("PAGE_DRAFT_EDIT")
        }
        openDraft()
        openBuyout()
        performAuctionBuyout {
            clickLeaveRequestButton()
        }
        performAuctionForm {
            clickCrateAuctionRequest()
        }
        // This wait for check events within watcher.
        waitSomething(1)
    }

    private fun openBuyout() {
        performFullDraftVas {
            scrollToNoPublishButton()
            clickNoPublishButton()
        }
    }

    private fun openDraft() {
        webServerRule.routing {
            getAndPutDraft(draftDispatcherArgs.copy(offerId = DRAFT_OFFER_ID))
            getAndPutDraft(draftDispatcherArgs.copy(offerId = null))
        }
        activityRule.launchActivity()
        performMain { openLowTab(R.string.offers) }
        performOffers { openDraftOrWizardScreen(isAddOfferButtonFree = false) }
    }

    private fun Routing.getAndPutDraft(args: DraftDispatcherArgs) {
        draftDispatcherArgs = args
        getDraft(args)
        putDraft(args)
    }

    companion object {
        private const val DRAFT_OFFER_ID = "1098043730-c9e04520"
    }
}
