package ru.auto.ara.test.deeplink

import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.matchers.ViewMatchers.withMaterialToolbarTitle
import ru.auto.ara.core.matchers.ViewMatchers.withNotEmptyMaterialToolbarSubtitle
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ChatDeeplinkTest {

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetRoomMessagesDispatcher.getEmptyResponse(),
            GetChatRoomDispatcher("from_customer_to_seller"),
            GetRoomSpamMessagesDispatcher.getEmptyResponse()
        )
        userSetup()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule()
    )

    @Test
    fun shouldOpenRoomFromDeeplink() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_DEEPLINK)
        checkChatRoom()
    }

    @Test
    fun shouldOpenRoomFromAppLink() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_CHAT_APPLINK)
        checkChatRoom()
    }

    private fun checkChatRoom() {
        checkChatRoom {
            isChatSubjectDisplayed("Audi A3 III (8V) Рестайлинг, 2019", "100 000 \u20BD")
            interactions.onToolbar().waitUntil(
                isCompletelyDisplayed(),
                withMaterialToolbarTitle("Продавец"),
                withNotEmptyMaterialToolbarSubtitle()
            )
            isPresetMessagesDisplayed(CHAT_ROOM_PRESETS_EMPTY_ROOM_DATA)
        }
    }

    companion object {
        private const val DEFAULT_CHAT_APPLINK = "autoru://app/chat/room/6822dc60e71440f35f012d0b35b5b234"
        private const val DEFAULT_CHAT_DEEPLINK = "https://auto.ru/chat/6822dc60e71440f35f012d0b35b5b234"
    }
}
