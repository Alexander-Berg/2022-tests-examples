package ru.auto.ara.test.offer.communicate

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.chat.PostChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class OpenChatNotAuthTest {

    val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        webServerRule.routing {
            getOffer("1080290554-5349dabf", "cars")
            getChatRoom("from_customer_to_seller")
            getRoomMessagesFirstPage(RoomMessages.EMPTY)
            getRoomSpamMessages(RoomSpamMessage.EMPTY)
            delegateDispatcher(PostChatRoomDispatcher("room_after_post"))
        }
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1080290554-5349dabf")
        performOfferCard { clickMakeChatOrHelp() }
    }

    @Test
    fun shouldReturnToOfferWhenAbortLogin() {
        performLogin { close() }
        Espresso.pressBack()
        checkOfferCard { isOfferCardTitle("Ford F-150 XIII, 2015") }
    }

    @Test
    fun shouldOpenChatRoomWhenLogin() {
        webServerRule.routing {
            postLoginOrRegisterSuccess()
            userSetup()
        }
        performLogin { loginWithPhoneAndCode() }
        checkChatRoom { isChatSubjectDisplayed("Audi A5 I Рестайлинг, 2012", "1 100 000 \u20BD") }
    }
}
