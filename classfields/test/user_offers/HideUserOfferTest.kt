package ru.auto.ara.test.user_offers

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PostHideUserOffersDispatcher
import ru.auto.ara.core.robot.performImageTextActionDialog
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.OffersRobot
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.HIDE_REASON_OFFER_DATA

@RunWith(Parameterized::class)
class HideUserOfferTest(private val testParams: TestParameter) {

    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    private val userOffersDispatcherHolder = DispatcherHolder()
    private val watcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            userOffersDispatcherHolder,
            PostHideUserOffersDispatcher(requestWatcher = watcher)
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule()
    )

    @Before
    fun setUp() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.active()
        activityTestRule.launchActivity()
        performMain {
            waitLowTabsShown()
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForOfferSnippet()
        }
    }

    @Test
    fun shouldHideUserOffer() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.inactive(mockFilePrefix = "bmw_")
        performOffers {
            clickHideOffer(FIRST_OFFER_INDEX)
        }.checkResult {
            isBottomSheetListHasExpectedChildsCount(ITEMS_COUNT)
        }

        performOffers {
            clickSelectOptionWithScroll(testParams.reasonName)
        }.checkResult {
            watcher.checkRequestBodyParameter(
                "reason", testParams.reasonRequest
            )
        }

        performImageTextActionDialog {
            clickOnClose()
        }

        performOffers {
            waitForOfferSnippet()
        }.checkResult {
            isInactiveSnippetDisplayed(
                index = FIRST_OFFER_INDEX,
                title = TITLE,
                price = PRICE,
                status = INACTIVE_STATUS,
                paidReason = R.string.vas_desc_activate_payment_group
            )
        }
    }

    private fun OffersRobot.waitForOfferSnippet() {
        waitForOfferSnippets(1)
    }

    companion object {
        private const val FIRST_OFFER_INDEX = 0
        private const val TITLE = "BMW 3 серия 330e VI (F3x) Рестайлинг, 2019"
        private const val PRICE = "1,887,000 \u20BD"
        private const val INACTIVE_STATUS = "Неактивно"
        private const val ITEMS_COUNT = 7

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data() = HIDE_REASON_OFFER_DATA.map { (name, reason) ->
            TestParameter(
                reasonRequest = reason,
                reasonName = name
            )
        }

    }

    data class TestParameter(
        val reasonRequest: String,
        val reasonName: String
    ){
        override fun toString(): String = reasonRequest
    }
}
