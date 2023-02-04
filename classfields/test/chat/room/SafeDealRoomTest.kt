package ru.auto.ara.test.chat.room

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.safe_deal.getSafeDealList
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.ChatRoomRobotChecker
import ru.auto.ara.core.robot.chat.performChatRoom
import ru.auto.ara.core.robot.safe_deal.checkSafeDealList
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class SafeDealRoomTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        getSafeDealList()
        getChatRoom("safe_deal_notification_center")
        getRoomMessagesFirstPage(RoomMessages.SAFE_DEAL_MESSAGES)
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        SetupTimeRule(date = "09.11.2021", localTime = "15:00"),
        SetupAuthRule(),
        ImmediateImageLoaderRule(),
    )

    @Test
    fun shouldShowSafeDealListIfMessageWithGoToDealsLinkClicked() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        ChatRoomRobotChecker.closeKeyboard()
        performChatRoom {
            clickMessageLink(
                messageSubstring = "Покупатель ещё раз предлагает безопасную сделку по Kia Rio.",
                linkText = "Перейти к сделкам",
            )
        }
        waitSomething(2)
        checkSafeDealList { compareSafeDealsWithAdsScreenshots() }
    }

    @Test
    fun shouldShowWebviewWithSafeDealIfMessageWithMoreLinkClicked() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        ChatRoomRobotChecker.closeKeyboard()
        performChatRoom {
            clickMessageLink(
                messageSubstring = "Александра указал свои паспортные данные в сделке по CHERY AMULET.",
                linkText = "Подробнее",
            )
        }
        checkWebView { isWebViewToolBarDisplayed("Канал новостей и предложений") }
    }

    companion object {
        private const val CHAT_ID = "testChat-28199386-38335478"
        private const val DEFAULT_CHAT_DEEPLINK = "autoru://app/chat/room/$CHAT_ID"
    }

}
