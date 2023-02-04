package ru.auto.ara.test.offer.communicate

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.PostChatRoomDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class ChatOnlyTest {
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"

    private val webServerRule = WebServerRule {
        userSetup()
        postLoginOrRegisterSuccess()
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1083103750-310000"),
            PostChatRoomDispatcher("room_after_post"),
            GetRoomMessagesDispatcher.getEmptyResponse()
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1083103750-310000")
    }

    @Test
    fun shouldCreateChatFromChatOnlyDisclaimer() {
        checkOfferCard { isChatOnlyCommunicateBlockDisplayed() }
        performOfferCard { interactions.onMakeChatOrHelpButton().performClick() }.checkResult {
            isChatOnlyDisclaimerDisplayed()
        }
        performOfferCard { interactions.onBottomsheetActionButton().performClick() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkChatRoom { isChatSubjectDisplayed("Audi A5 I Рестайлинг, 2012", "1 100 000 \u20BD") }
    }

    @Test
    fun shouldCreateChatFromChatOnlyOffer() {
        performOfferCard { interactions.onMakeCallOrChat().waitUntilIsCompletelyDisplayed().performClick() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkChatRoom { isChatSubjectDisplayed("Audi A5 I Рестайлинг, 2012", "1 100 000 \u20BD") }
    }
}
