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
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.PostHideUserOffersDispatcher
import ru.auto.ara.core.robot.bottomsheet.checkBottomSheetScreenshot
import ru.auto.ara.core.robot.bottomsheet.performBottomSheet
import ru.auto.ara.core.robot.calendar.checkCalendar
import ru.auto.ara.core.robot.calendar.performCalendar
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.OffersRobot
import ru.auto.ara.core.robot.useroffers.checkOffers
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString

@RunWith(AndroidJUnit4::class)
class HideOfferWithReactivationTest {
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
        SetupAuthRule(),
        SetupTimeRule()
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


    private fun OffersRobot.waitForOfferSnippet() {
        waitForOfferSnippets(count = 1)
    }


    @Test
    fun shouldHideOffer() {
        performOffers {
            clickHideOffer(index = 0)
        }.checkResult {
            isBottomSheetListHasExpectedChildsCount(ITEMS_COUNT)
        }

        performOffers {
            clickSelectOptionWithScroll(getResourceString(R.string.hide_offer_reactivate_later))
        }

        checkBottomSheetScreenshot {
            isDialogScreenshotTheSame("/user_offers/reactivate_later/bs_initial.png")
        }

        performOffers {
            clickReactivateLaterDate()
        }

        checkCalendar {
            isCalendarSame("/user_offers/reactivate_later/calendar_initial.png")
        }

        performCalendar {
            clickDay(contentDescription = "Fri, Jan 30, 1970")
        }.checkResult {
            isCalendarSame("/user_offers/reactivate_later/calendar_30.png")
        }

        performCalendar {
            clickConfirm()
        }

        checkBottomSheetScreenshot {
            isDialogScreenshotTheSame("/user_offers/reactivate_later/bs_30.png")
        }

        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.reactivateLater()

        performOffers {
            clickReactivatePublishButton()
        }

        checkBottomSheetScreenshot {
            isDialogScreenshotTheSame("/user_offers/reactivate_later/success.png")
        }

        performBottomSheet {
            close()
        }

        checkOffers {
            checkSnippetIsSame(
                index = 0,
                fileName = "/user_offers/reactivate_later/temp_inactive_snippet.png"
            )
        }

        performOffers {
            clickOfferSnippet(index = 0)
        }

        checkOfferCard {
            isReactivateLaterExist()
        }
    }

    companion object {
        private const val ITEMS_COUNT = 7
    }
}
