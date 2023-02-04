package ru.auto.ara.test.offer.chatbot

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.actions.ViewActions
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.chat.GetChatBotStartCheckup
import ru.auto.ara.core.dispatchers.chat.GetChatRoomDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.GetRoomSpamMessagesDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ChatBotSendCarTest {
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"
    private val OFFER_ID = "1082957054-8d55bf9a"
    private val FILE_OFFER_ID = "1093024666-aa502a2a"
    private val vibiralshikWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        postLoginOrRegisterSuccess()
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", OFFER_ID),
            GetChatBotStartCheckup(OFFER_ID, vibiralshikWatcher),
            GetRoomMessagesDispatcher.getChatbotAfterSendCar(),
            GetChatRoomDispatcher("vibiralshik_empty"),
            GetRoomSpamMessagesDispatcher.getEmptyResponse(),
        )
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
        )
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$OFFER_ID")
        checkOfferCard { isOfferCard() }
    }

    @Test
    fun shouldLoginAndSendStartCheckupRequest() {
        performOfferCard {
            interactions.onAppBar().waitUntilIsCompletelyDisplayed().perform(ViewActions.setAppBarExpandedState(false))
            scrollToPrepaymentWarning()
            interactions.onChatBotStartCheckupButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkLogin { isPhoneAuth() }
        webServerRule.routing { userSetup() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkChatRoom {
            isChatMessageDisplayed("Привет! Я собираюсь на осмотр этого автомобиля — https://auto.ru/cars/used/sale/$OFFER_ID")
            vibiralshikWatcher.checkQueryParameter("offer-link", "https://auto.ru/cars/used/sale/$OFFER_ID")
        }
    }

    @Test
    fun shouldSendStartCheckupRequestWhenAlreadyAuthorized() {
        performOfferCardVin { clickShowFreeReport() }
        webServerRule.routing { userSetup() }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkVinReport { isVinReport() }
        waitSomething(300, TimeUnit.MILLISECONDS)
        performVinReport { close() }

        performOfferCard {
            scrollToPrepaymentWarning()
            interactions.onChatBotStartCheckupButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkChatRoom {
            isChatMessageDisplayed("Привет! Я собираюсь на осмотр этого автомобиля — https://auto.ru/cars/used/sale/$OFFER_ID")
            vibiralshikWatcher.checkQueryParameter("offer-link", "https://auto.ru/cars/used/sale/$OFFER_ID")
        }
    }
}
