package ru.auto.ara.test.other.notifications

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.device_subscriptions.DeleteSubscriptionDispatcher
import ru.auto.ara.core.dispatchers.device_subscriptions.PostSubscriptionDispatcher
import ru.auto.ara.core.robot.othertab.checkNotifications
import ru.auto.ara.core.robot.othertab.performNotifications
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.NOTIFICATIONS_ITEMS
import ru.auto.ara.core.utils.activityScenarioWithFragmentRule
import ru.auto.ara.ui.activity.SurfaceActivity
import ru.auto.ara.ui.fragment.notifications.NotificationsFragment

@RunWith(Parameterized::class)
class NotificationsSwitchersTest(private val testParam: TestParameter) {
    private val postWatcher = RequestWatcher()
    private val deleteWatcher = RequestWatcher()
    private val webServerRule = WebServerRule()

    private val activityTestRule = activityScenarioWithFragmentRule<SurfaceActivity, NotificationsFragment>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
    )

    @Test
    fun shouldSwitchNotifications() {
        webServerRule.routing {
            delegateDispatchers(
                PostSubscriptionDispatcher(testParam.apiParam, postWatcher),
                DeleteSubscriptionDispatcher(testParam.apiParam, deleteWatcher)
            )
        }

        checkNotifications().isSwitched(testParam.displayName)

        performNotifications().switch(testParam.displayName).checkResult {
            isNotSwitched(testParam.displayName)
            deleteWatcher.checkRequestWasCalled()
            postWatcher.checkRequestWasNotCalled()
            deleteWatcher.clearRequestWatcher()
        }

        performNotifications().switch(testParam.displayName).checkResult {
            isSwitched(testParam.displayName)
            postWatcher.checkRequestWasCalled()
            deleteWatcher.checkRequestWasNotCalled()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = NOTIFICATIONS.map { arrayOf(it) }

        private val NOTIFICATIONS = NOTIFICATIONS_ITEMS.map { (displayName, apiParam) ->
            TestParameter(
                displayName = displayName,
                apiParam = apiParam
            )
        }
    }

    data class TestParameter(
        val displayName: String,
        val apiParam: String
    ) {
        override fun toString(): String = apiParam
    }
}
