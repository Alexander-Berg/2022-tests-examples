package ru.auto.ara.test.favorites

import android.Manifest
import ru.auto.ara.core.rules.GrantPermissionsRule
import org.junit.Rule
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.device.postHello
import ru.auto.ara.core.dispatchers.offer_card.getCallsStats
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.Route
import ru.auto.ara.core.routing.Routing
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.NotificationsEnabledRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

abstract class FavoritesListSetup {
    protected val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()
    protected val webServerRule = WebServerRule {
        stub { getCallsStats() }
        delegateDispatcher(ParseDeeplinkDispatcher.carsAll())
        getFavoritesRoute()
        userSetup()
        postHello()
        getChatRoom("from_customer_to_seller")
        getRoomMessagesFirstPage(RoomMessages.EMPTY)
        getRoomSpamMessages(RoomSpamMessage.EMPTY)
    }
    protected val prefsRule = SetPreferencesRule()
    protected val timeRule = SetupTimeRule(date = "22.09.2020", timeZoneId = "Europe/Moscow")
    protected val experiments = experimentsOf()

    protected open val setupAuthRule: SetupAuthRule? = SetupAuthRule()

    open val areNotificationsEnabledByUser = true
    open val areOverlaysEnabledByUser = true

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        prefsRule,
        setupAuthRule,
        ImmediateImageLoaderRule { url -> url != "//images.mds-proxy.test.avto.ru/" },
        NotificationsEnabledRule({ areNotificationsEnabledByUser }, { areOverlaysEnabledByUser }),
        timeRule,
        activityTestRule,
        GrantPermissionsRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.SYSTEM_ALERT_WINDOW),
        arguments = TestMainModuleArguments(
            testExperiments = experiments
        )
    )

    abstract fun Routing.getFavoritesRoute(): Route

    protected fun openFavorites() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru")
        performMain { openLowTab(R.string.favorite) }
    }
}
