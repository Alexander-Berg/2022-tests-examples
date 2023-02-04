package ru.auto.ara.test.chat.room

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user.userSetupNone
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.chat.checkMessages
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.othertab.profile.performProfile
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

class ChatRoomLoginTest {

    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    private val webServerRule = WebServerRule {
        delegateDispatchers(GetChatRoomDispatcher("spam"))
        postLoginOrRegisterSuccess()
        postEmptyGarageListing()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule()
    )

    @Before
    fun setup() {
        activityRule.launchActivity()
    }

    @Test
    fun shouldLoginAndLogoutUpdateMessageCount() {
        performMain { openLowTab(MESSAGES) }
        performMessages { clickOnLoginButton() }
        webServerRule.routing { userSetup() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkMessages { isDialogsLoadedInList(3) }
        checkMain {
            isLowTabTheSame(MESSAGES, MESSAGES_TAB_WITH_1_COUNT_SELECTED_SCREENSHOT_NAME)
        }
        performMessages { openBurgerMenu() }
        performBurger { scrollAndClickOnUserItem() }
        webServerRule.routing { userSetupNone() }
        performProfile { scrollAndClickLogout() }
        performBurger { clickOnClose() }
        performMain { openLowTab(MESSAGES) }
        checkMain {
            isLowTabTheSame(MESSAGES, MESSAGES_TAB_WITHOUT_COUNT_SELECTED_SCREENSHOT_NAME)
        }
    }

    companion object {
        private const val MESSAGES = R.string.messages
        private const val PHONE = "+7 (000) 000-00-00"
        private const val CODE = "0000"
        const val MESSAGES_TAB_WITH_1_COUNT_SELECTED_SCREENSHOT_NAME = "main/tabs/messages_tab_with_1_count_selected.png"
        const val MESSAGES_TAB_WITHOUT_COUNT_SELECTED_SCREENSHOT_NAME = "main/tabs/messages_tab_without_count_selected.png"
    }
}
