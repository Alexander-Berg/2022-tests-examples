package ru.auto.ara.test.other_dealers_offers

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.auto.ara.core.rules.GrantPermissionsRule
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.actions.scroll.scrollToItemWithText
import ru.auto.ara.core.behaviour.checkInitialApp2AppOutgoingCallIsDisplayingCorrectly
import ru.auto.ara.core.behaviour.disableApp2AppInstantCalling
import ru.auto.ara.core.behaviour.enableApp2AppInstantCalling
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.interaction.other_dealers_offers.OtherDealersOffersInteractions
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.other_dealers_offers.checkOtherDealersOffers
import ru.auto.ara.core.robot.other_dealers_offers.performOtherDealersOffers
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.ratecall.performRateCall
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.rules.DecreaseRateCallTimeRule
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.NamedViewInteraction
import ru.auto.ara.core.utils.activityScenarioWithFragmentRule
import ru.auto.ara.core.utils.intendingNotInternal
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup
import ru.auto.experiments.Experiments
import ru.auto.experiments.showOtherDealersOffersCount
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OtherDealersOffersTest {

    private val webServerRule = WebServerRule {
        userSetup()
        getOfferCount()
        postSearchOffers(
            assetPath = "listing_offers/bmw_cars_page_1.json"
        )
        getOffer(
            "1088594960-9a9bdcaf"
        )
        getPhones(PhonesResponse.ONE)
    }

    private val activityRule = activityScenarioWithFragmentRule<SearchFeedActivity, SearchFeedFragment>(
        SearchFeedFragmentBundles.searchFeedBundle(
            category = VehicleCategory.CARS,
            stateGroup = StateGroup.ALL
        )
    )

    private val experiments = experimentsOf {
        Experiments::showOtherDealersOffersCount then 1
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityRule,
        DecreaseRateCallTimeRule(),
        SetupAuthRule(),
        GrantPermissionsRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.SYSTEM_ALERT_WINDOW),
        arguments = TestMainModuleArguments(
            testExperiments = experiments,
        )
    )

    @Before
    fun setUp() {
        experiments.disableApp2AppInstantCalling()
    }

    @Test
    fun shouldOpenOtherDealersOffersAfterCall() {
        withIntents {
            intendingCall()
            triggerOtherDealersOffers()
        }
        performOtherDealersOffers().checkResult {
            isTitleDisplayed()
            areViewsDisplayed(
                interactions.onSubtitle("Новые BMW X1 18d xDrive 2.0d AT (150 л.с.) 4WD xDrive18d Sport Line"),
                interactions.onFirstSnippet(),
                interactions.onOfferSnippet(
                    price = "7 499 500 \u20BD",
                    options = "73 опции",
                    badge = "Лучшая комплектация",
                    dealer = "БорисХоф BMW Юг",
                    dealerAddress = "Москва, 29-й км МКАД"
                ),
                interactions.onOfferSnippet(
                    price = "9 400 000 \u20BD",
                    options = "59 опций",
                    badge = null,
                    dealer = "Автодель BMW",
                    dealerAddress = "Симферополь, проспект Победы 110"
                )
            )
        }
    }

    @Test
    fun shouldNotOpenOtherDealersOffersAfterSecondCall() {
        withIntents {
            intendingCall()
            // open first time and close
            triggerOtherDealersOffers()
            performOtherDealersOffers().checkResult { isTitleDisplayed() }
            performOtherDealersOffers { clickCloseIcon() }

            // open second time and check rate call dialog is displayed
            triggerOtherDealersOffers()
            performRateCall().checkResult { isRateCallDialogWithoutCheckupButtonDisplayed(false) }
        }
    }

    @Test
    fun shouldOpenOtherDealersOffersAfterNonTrackingCall() {
        withIntents {
            intendingCall(waitTimeout = false)
            // try to open dialog first time (this shouldn't be opened)
            triggerOtherDealersOffers()
            performSearchFeed { scrollToTop() }
            performSearchFeed().checkResult { isStateSelectorDisplayed() }

            intendingCall()
            // really open first time and close
            triggerOtherDealersOffers()
            performOtherDealersOffers().checkResult { isTitleDisplayed() }
        }
    }

    @Test
    fun shouldDisableSnippetAfterCall() {
        // open dialog
        withIntents {
            intendingCall()
            triggerOtherDealersOffers()
        }
        // call by first snippet
        withIntents {
            intendingCall()
            performOtherDealersOffers { clickCallButton("Автомобили Баварии BMW Пермь") }
            performCommon().checkResult { isActionDialIntentCalled("+7 985 440-66-27") }
        }
        checkOtherDealersOffers { isSnippetDisabled(interactions.onFirstSnippet()) }
    }

    @Test
    @Ignore("other dealers screen for some reason is not showing after call from offer :(")
    fun shouldOpenOtherDealersOffersAfterSecondCallFromOffer() {
        withIntents {
            intendingCall()
            // open dialog
            triggerOtherDealersOffers()

            // open first snippet
            performOtherDealersOffers { interactions.onFirstSnippet().performClick() }

            // click by call from card
            performOfferCard {
                interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed().performClick()
            }

            performOtherDealersOffers().checkResult { isTitleDisplayed() }
        }
    }

    @Test
    fun shouldCellCallNotApp2AppOnOfferSnippet() {
        webServerRule.routing {
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        withIntents {
            intendingCall()
            triggerOtherDealersOffers()
            checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
        }
    }

    @Test
    fun shouldCallByApp2AppInstantlyOnOfferSnippet() {
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        withIntents {
            intendingCall()
            triggerOtherDealersOffers()
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    private fun triggerOtherDealersOffers() {
        performSearchFeed {
            val offerTitle = "BMW 3 серия Gran Turismo 320i xDrive VI (F3x) Рестайлинг, 2019"
            waitSearchFeed()
            scrollToItemWithText(offerTitle)
            clickOnCallButtonFromSnippetGallery(offerTitle)
        }
    }

    private fun intendingCall(waitTimeout: Boolean = true) {
        intendingNotInternal().respondWithFunction {
            if (waitTimeout) {
                waitSomething(500, TimeUnit.MILLISECONDS) // wait dealers offers
                performCommon { moveTimeForward(5) }
            }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
    }

    private fun OtherDealersOffersInteractions.onFirstSnippet() =
        onOfferSnippet(
            price = "от 2 220 000 \u20BD",
            options = "52 опции",
            badge = "Лучшая цена",
            dealer = "Автомобили Баварии BMW Пермь",
            dealerAddress = "Пермь, Россия, Пермский край, Большое Савино, шоссе Космонавтов, 380"
        )

    private fun areViewsDisplayed(vararg viewInteractions: NamedViewInteraction) {
        viewInteractions.forEachIndexed { index, interaction ->
            performOtherDealersOffers { scrollToItemAt(index) }
                .checkResult { isViewDisplayed(interaction, index) }
        }
    }

}
