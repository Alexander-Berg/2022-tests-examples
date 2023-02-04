package ru.auto.ara.screenshotTests.auction.frontlog_tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.auctions.c2bApplicationInfo
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplication
import ru.auto.ara.core.dispatchers.auctions.c2bAuctionApplicationMobileTexts
import ru.auto.ara.core.dispatchers.draft.DraftDispatcherArgs
import ru.auto.ara.core.dispatchers.draft.getDraft
import ru.auto.ara.core.dispatchers.draft.putDraft
import ru.auto.ara.core.dispatchers.frontlog.checkAuctionFrontLog
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.auction.performAuctionForm
import ru.auto.ara.core.robot.wizard.performWizard
import ru.auto.ara.core.rules.SetupTestEnvironmentRule
import ru.auto.ara.core.rules.ShouldShowWizardRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.WizardStepRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.wizard.AuctionProposalStep
import ru.auto.data.util.REGEX_ANY
import ru.auto.data.util.TEST_DRAFT_AND_WIZARD_WITH_MOCKS
import ru.auto.data.util.TEST_WIZARD_AUCTION_SHOW_BEFORE_PRICE
import ru.auto.data.util.TEST_WIZARD_IS_AUCTION_AVAILABLE_FOR_OFFER
import ru.auto.experiments.Experiments
import ru.auto.experiments.c2bAuctionFlow

@RunWith(AndroidJUnit4::class)
class AuctionFrontlogFromWizardTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        c2bApplicationInfo()
        c2bAuctionApplicationMobileTexts()
        c2bAuctionApplication()
    }

    val experiments = experimentsOf {
        Experiments::c2bAuctionFlow then "auction_flow_v1"
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        ShouldShowWizardRule(true),
        SetupTestEnvironmentRule { test ->
            TEST_DRAFT_AND_WIZARD_WITH_MOCKS = test
            TEST_WIZARD_AUCTION_SHOW_BEFORE_PRICE = test
            TEST_WIZARD_IS_AUCTION_AVAILABLE_FOR_OFFER = test
        },
        WizardStepRule(AuctionProposalStep),
        ImmediateImageLoaderRule { url ->
            ImmediateImageLoaderRule.mockPhotosPredicate(url) || url.contains("yastatic.net")
        },
        arguments = TestMainModuleArguments(experiments)
    )

    @Test
    fun shouldLogFromWizard() {
        routeDraft()
        activityRule.launchDeepLinkActivity("autoru://app/cars/used/add")
        webServerRule.routing {
            checkAuctionFrontLog("PAGE_DRAFT_CREATE")
        }
        performWizard {
            openAuctionRequestForm()
        }
        performAuctionForm {
            clickCrateAuctionRequest()
        }
        // This wait for check events within watcher.
        waitSomething(1)
    }

    private fun routeDraft() {
        val args = DraftDispatcherArgs()
        webServerRule.routing {
            getDraft(args)
            putDraft(args)
            getDraft(args.copy(offerId = REGEX_ANY))
            putDraft(args.copy(offerId = REGEX_ANY))
        }
    }
}
