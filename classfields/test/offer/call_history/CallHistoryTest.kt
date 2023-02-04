package ru.auto.ara.test.offer.call_history

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.behaviour.checkInitialApp2AppOutgoingCallIsDisplayingCorrectly
import ru.auto.ara.core.behaviour.disableApp2AppInstantCalling
import ru.auto.ara.core.behaviour.enableApp2AppInstantCalling
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.app2app.getApp2AppCallInfo
import ru.auto.ara.core.dispatchers.call_history.GetCallHistoryDispatcher
import ru.auto.ara.core.dispatchers.call_history.getApp2AppCallHistory
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOfferDispatcher
import ru.auto.ara.core.dispatchers.user_offers.GetUserOfferStatsDispatcher
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.call_history.checkCallHistory
import ru.auto.ara.core.robot.call_history.performCallHistory
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeZoneRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.OfferCardBundles
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.OfferDetailsActivity
import ru.auto.ara.ui.fragment.offer.OfferDetailsFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.util.getRuLocale
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class CallHistoryTest {

    private val userOfferStatsWatcher = RequestWatcher()

    private val userOfferDispatcherHolder = DispatcherHolder()
    private val userOfferStatsDispatcherHolder = DispatcherHolder()

    private val callHistoryRequestWatcher: RequestWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            userOfferDispatcherHolder,
            userOfferStatsDispatcherHolder,
            GetCallHistoryDispatcher(
                category = VehicleCategory.CARS,
                offerId = OFFER_ID,
                page = 1
            ),
            GetCallHistoryDispatcher(
                category = VehicleCategory.CARS,
                offerId = OFFER_ID,
                page = 2,
                watcher = callHistoryRequestWatcher
            )
        )
    }

    private val activityRule = lazyActivityScenarioRule<OfferDetailsActivity>()

    private val experiments = experimentsOf()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule(),
        SetupTimeZoneRule(),
        GrantPermissionsRule.grant(RECORD_AUDIO, SYSTEM_ALERT_WINDOW),
        arguments = TestMainModuleArguments(
            experiments
        )
    )

    @After
    fun doAfter() {
        experiments.disableApp2AppInstantCalling()
    }

    @Test
    fun shouldOpenCallHistoryScreen() {
        openCard()
        performOfferCard {
            scrollToCounters()
        }.checkResult {
            isCallsDisplayed(
                weeklyCount = WEEKLY_CALLS_COUNT,
                totalCount = TOTAL_CALLS_COUNT
            )

            val simpleDate = SimpleDateFormat("yyy-MM-dd", getRuLocale())
            val to = Calendar.getInstance().time
            val from = Calendar.getInstance().apply { add(Calendar.DATE, -6) }.time
            userOfferStatsWatcher.checkQueryParameter("from", simpleDate.format(from))
            userOfferStatsWatcher.checkQueryParameter("to", simpleDate.format(to))
        }

        performOfferCard {
            interactions.onTotalCalls().performClick()
        }

        checkCallHistory {
            isTitleDisplayed()
            areViewsMatchesOrdered(
                interactions.onDateDivider(date = "27 ноября 2018 г."),
                interactions.onMissedCall(
                    phone = PHONE,
                    time = "11:47"
                ),
                interactions.onDateDivider(date = "26 ноября 2018 г."),
                interactions.onAcceptedCall(
                    phone = PHONE,
                    time = "12:00",
                    duration = "6 мин. 7 сек."
                )
            )
        }
    }

    @Test
    fun shouldLoadNextPageOfCallHistory() {
        openCard()
        performOfferCard {
            scrollToCounters()
            interactions.onTotalCalls().performClick()
        }

        performCallHistory {
            scrollToBottom()
        }.checkResult {
            interactions.onDateDivider("21 ноября 2018 г.").waitUntilIsCompletelyDisplayed()
            callHistoryRequestWatcher.checkQueryParameter("page", "2")
        }
    }

    @Test
    fun shouldCallByClickByPhone() {
        openCard()
        performOfferCard {
            scrollToCounters()
            interactions.onTotalCalls().performClick()
        }

        performCallHistory {
            Intents.init()
            interactions.onMissedCall(
                phone = PHONE,
                time = "11:47"
            ).performClick()
        }

        checkCommon { isActionDialIntentCalled(RAW_PHONE) }
    }

    @Test
    fun shouldApp2AppCallByClickByPhone() {
        webServerRule.routing {
            getApp2AppCallHistory()
            getApp2AppCallInfo()
        }
        openCard()
        performOfferCard {
            scrollToCounters()
            interactions.onTotalCalls().performClick()
        }

        performCallHistory {
            interactions.onMissedCall(
                phone = APP2APP_ALIAS,
                time = "11:47"
            ).performClick()
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldCallByApp2AppInstantlyByClickByPhone() {
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            getApp2AppCallHistory()
            getApp2AppCallInfo()
        }
        openCard()
        performOfferCard {
            scrollToCounters()
            interactions.onTotalCalls().performClick()
        }

        performCallHistory {
            interactions.onMissedCall(
                phone = APP2APP_ALIAS,
                time = "11:47"
            ).performClick()
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }

    @Test
    fun shouldNotOpenCallHistoryScreen() {
        openCard(offerId = EMPTY_HISTORY_OFFER_ID)
        performOfferCard {
            scrollToCounters()
            interactions.onTotalCalls().performClick()
        }.checkResult {
            interactions.onTotalCalls().checkIsCompletelyDisplayed()
        }
    }

    private fun openCard(offerId: String = OFFER_ID) {
        userOfferDispatcherHolder.innerDispatcher = GetUserOfferDispatcher(VehicleCategory.CARS, offerId)
        userOfferStatsDispatcherHolder.innerDispatcher = GetUserOfferStatsDispatcher(
            category = "cars",
            name = STATS_MOCK,
            watcher = userOfferStatsWatcher
        )
        activityRule.launchFragment<OfferDetailsFragment>(
            OfferCardBundles.userOfferBundle(
                category = VehicleCategory.CARS,
                offerId = offerId
            )
        )
    }

    companion object {
        private const val OFFER_ID = "1056093692-2f0b33d2"
        private const val EMPTY_HISTORY_OFFER_ID = "1056093692-2f0b33d3"

        private const val WEEKLY_CALLS_COUNT = 22
        private const val TOTAL_CALLS_COUNT = 35

        private const val PHONE = "+7 (985) 440-66-37"
        private const val APP2APP_ALIAS = "id25367896"
        private const val RAW_PHONE = "+79854406637"

        private const val STATS_MOCK = "call_history_test"
    }
}
