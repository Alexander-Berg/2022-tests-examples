package ru.auto.ara.test.user_offers

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOfferDispatcher
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PutProductProlongDispatcher
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.network.scala.offer.converter.ServicePriceConverter.PROLONGATION_ADDITIONAL_TIME_SEC
import ru.auto.data.util.TEST_SEVEN_DAYS_TIMER

@RunWith(AndroidJUnit4::class)
class SevenDaysWithExpTest {
    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    private val userOffersDispatcherHolder = DispatcherHolder()
    private val userOfferDispatcherHolder = DispatcherHolder()
    private val putProductProlongRW = RequestWatcher()
    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            userOfferDispatcherHolder,
            userOffersDispatcherHolder,
            PutProductProlongDispatcher(
                category = "cars",
                offerId = OFFER_ID,
                product = "all_sale_activate",
                requestWatcher = putProductProlongRW
            ),
            GetUserOffersDispatcher.emptySecondPage("all")
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupTimeRule(date = "07.02.2020"),
        SetupAuthRule()
    )

    @Before
    fun before() {
        enableSevenDaysTimer(false)
    }

    @Test
    fun shouldSeeProlongationEnabledBlock() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.prolongationEnabledActive()
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
        }.checkResult {
            isProlongationEnabledBlockDisplayed(TIME_LEFT)
        }
    }

    @Test
    fun shouldSeeProlongationDisabledBlock() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.prolongationDisabledActive()
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }

        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
        }.checkResult {
            isProlongationFailedBlockDisplayed(TIME_LEFT)
            isProlongationDisabledBlockDisplayed(TIME_LEFT)
        }
    }

    @Test
    fun shouldSeeSevenDaysInactiveBlock() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.sevenDaysInactive()
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
        }.checkResult {
            isSevenDaysInactiveBlockDisplayed()
        }
    }

    @Test
    fun shouldNotSeeSevenDaysInactiveBlockIfNoDiscount() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.sevenDaysInactiveWithoutDiscount()
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
        }.checkResult {
            isSevenDaysInactiveBlockGone()
        }
    }


    @Test
    fun shouldSeeSevenDaysWillExpireTimerBlock() {
        enableSevenDaysTimer(true)
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.sevenDaysInactive()
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
        }.checkResult {
            isSevenDaysWillExpireTimerBlockDisplayed(timerSec = PROLONGATION_ADDITIONAL_TIME_SEC)
        }
    }

    @Test
    fun shouldOpenWebviewWhenClickQuestionIcon() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.prolongationDisabledActive()
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }

        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
        }
        watchWebView {
            performOffers {
                interactions.onProlongationLeftValueIcon().waitUntilIsCompletelyDisplayed().performClick()
            }
        }.checkResult {
            checkTitleMatches("Помогаем продавать машины быстрее")
            checkUrlMatches("https://auto.ru/dealer/profi/")
        }
    }

    @Test
    fun shouldProlongFromFailedBlock() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.prolongationDisabledActive()

        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }

        performOffers {
            waitForUserOffersCompletelyDisplayed()
            scrollToOfferSnippet(0)
            userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.prolongationEnabledActive()
            userOfferDispatcherHolder.innerDispatcher = GetUserOfferDispatcher(VehicleCategory.CARS, OFFER_ID)
            interactions.onEnablePrologationButton().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isProlongationEnabledBlockDisplayed(TIME_LEFT)
            putProductProlongRW.checkRequestWasCalled()
        }
    }

    private fun enableSevenDaysTimer(enable: Boolean) {
        TEST_SEVEN_DAYS_TIMER = enable
    }

    companion object {
        private const val OFFER_ID = "1095669442-b39897242"
        private const val TIME_LEFT = "6 дней"
    }
}
