package ru.auto.ara.test.offer.communicate

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.chat.PostChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.RoomSpamMessage
import ru.auto.ara.core.dispatchers.chat.getChatRoom
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.getRoomSpamMessages
import ru.auto.ara.core.dispatchers.frontlog.checkFrontlogCommonParams
import ru.auto.ara.core.dispatchers.frontlog.postFrontLog
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.appmetrica.checkAppMetrica
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class OpenChatTest(private val param: TestParameter) {
    val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule {
        userSetup()
        getOffer(param.offerId, param.category)
        getChatRoom("from_customer_to_seller")
        getRoomMessagesFirstPage(RoomMessages.EMPTY)
        getRoomSpamMessages(RoomSpamMessage.EMPTY)
        delegateDispatcher(PostChatRoomDispatcher("room_after_post")).watch {
            checkRequestBodyParameters(
                "subject.offer.category" to param.category,
                "subject.offer.id" to param.offerId
            )
        }
        postFrontLog().watch { checkFrontlogCommonParams("chat_init_event") }
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        SetupAuthRule()
    )

    @Before
    fun setUp() {
        activityRule.launchDeepLinkActivity("https://auto.ru/${param.category}/used/sale/${param.offerId}")
    }

    @Test
    fun shouldOpenChatRoomFromCarOffer() {
        performOfferCard { clickMakeChatOrHelp() }
        checkChatRoom { isChatSubjectDisplayed("Audi A5 I Рестайлинг, 2012", "1 100 000 \u20BD") }
        checkAppMetrica {
            checkAppMetricaEvent(
                eventName = "Сообщения. Написать сообщение",
                eventParams = mapOf(
                    "Продавец" to "Частник",
                    "Источник" to "Карточка объявления"
                )
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data() = listOf(
            TestParameter("cars", "1080290554-5349dabf"),
            TestParameter("moto", "3042550-19b540c6"),
            TestParameter("trucks", "15673892-50417541")
        )

        data class TestParameter(
            val category: String,
            val offerId: String
        ) {
            override fun toString() = category
        }
    }
}
