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
import ru.auto.ara.core.dispatchers.user_offers.getActiveUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getUserOffer
import ru.auto.ara.core.dispatchers.user_offers.postUserOffersEdit
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.auction.performAuctionBuyout
import ru.auto.ara.core.robot.auction.performAuctionForm
import ru.auto.ara.core.robot.full_draft.performFullDraft
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTestEnvironmentRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import ru.auto.data.util.REGEX_ANY
import ru.auto.data.util.TEST_DRAFT_AND_WIZARD_WITH_MOCKS
import ru.auto.data.util.TEST_WIZARD_AUCTION_SHOW_BEFORE_PRICE
import ru.auto.data.util.TEST_WIZARD_IS_AUCTION_AVAILABLE_FOR_OFFER
import ru.auto.experiments.Experiments
import ru.auto.experiments.auctionDisplaying
import ru.auto.experiments.c2bAuctionFlow

@RunWith(AndroidJUnit4::class)
class AuctionFrontlogFromOfferEditTest {

    private val activityRule = lazyActivityScenarioRule<MainActivity>()

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
    fun shouldLogFromOfferEdit() {
        webServerRule.routing {
            checkAuctionFrontLog("PAGE_OFFER_EDIT")
        }
        openOfferEdit()
        performFullDraft {
            onSaveButtonClick()
        }
        performAuctionBuyout {
            clickLeaveRequestButton()
        }
        performAuctionForm {
            clickCrateAuctionRequest()
        }
        // This wait for check events within watcher.
        waitSomething(1)
    }

    private fun openOfferEdit() {
        webServerRule.routing {
            getActiveUserOffers()
            getUserOffer(offerId = OFFER_ID)
            postUserOffersEdit(
                offerId = OFFER_ID,
                draftId = DRAFT_ID_AFTER_EDIT
            )
            getDraft(offerArgs)
            putDraft(offerArgs)
            putDraft(offerArgs.copy(offerId = REGEX_ANY))
            delegateDispatchers(PostDraftPublishDispatcher(status = "ACTIVE"))
        }

        activityRule.launchActivity()
        performMain { openLowTab(R.string.offers) }
        performOffers {
            clickOnEditButtonOnOfferSnippet(0)
        }
    }

    companion object {
        private const val DRAFT_OFFER_ID = "1098043730-c9e04520"
        private const val DRAFT_ID_AFTER_EDIT = "8655763168354548560-d55703ee"
        private const val OFFER_ID = "1092688300-a5a5cc01"
        private val offerArgs = DraftDispatcherArgs(
            category = "cars",
            offerId = DRAFT_ID_AFTER_EDIT,
            withVip = true,
            withTurbo = true,
            activateArgs = null
        )
    }
}
