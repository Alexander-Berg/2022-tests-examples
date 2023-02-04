package ru.auto.ara.test.offer.advantages

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.chat.RoomMessages
import ru.auto.ara.core.dispatchers.chat.getRoomMessagesFirstPage
import ru.auto.ara.core.dispatchers.chat.postChatRoom
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.checkAdvantageOnlineView
import ru.auto.ara.core.robot.offercard.performAdvantageOnlineView
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE_CHAT_ONLY
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE_IS_DEALER
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class AdvantageOnlineViewTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule {
        postChatRoom("room_after_post")
        getRoomMessagesFirstPage(RoomMessages.EMPTY)
        userSetup()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        SetupAuthRule()
    )

    @Test
    fun shouldShowAdvantageOnlineViewDescription() {
        openOfferCard(OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE)
        openOnlineViewAvailableSingle()
        checkAdvantageOnlineView(showCallBtn = true, showChatBtn = true)
    }

    @Test
    fun shouldShowAdvantageOnlineChatOnlyViewDescription() {
        openOfferCard(OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE_CHAT_ONLY)
        openOnlineViewAvailableSingle()
        checkAdvantageOnlineView(showCallBtn = false, showChatBtn = true)
    }

    @Test
    fun shouldShowAdvantageOnlineDealerWithoutChat() {
        openOfferCard(OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE_IS_DEALER)
        openOnlineViewAvailableSingle()
        checkAdvantageOnlineView(showCallBtn = true, showChatBtn = false)
    }

    @Test
    fun shouldShowAdvantageOnlineTapOnCallButton() {
        webServerRule.routing { delegateDispatcher(GetPhonesDispatcher.onePhone(OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE)) }
        openOfferCard(OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE)
        openOnlineViewAvailableSingle()
        checkAdvantageOnlineView(showCallBtn = true, showChatBtn = true)
        checkCallButtonAction()
    }

    @Test
    fun shouldShowAdvantageOnlineTapOnChatButton() {
        openOfferCard(OFFER_ID_WITH_ONLINE_VIEW_ADVANTAGE)
        openOnlineViewAvailableSingle()
        checkAdvantageOnlineView(showCallBtn = true, showChatBtn = true)
        checkOpenChatAction()
    }

    private fun openOnlineViewAvailableSingle() {
        performOfferCard {
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
    }

    private fun checkAdvantageOnlineView(showCallBtn: Boolean, showChatBtn: Boolean) {
        checkAdvantageOnlineView {
            isTitleDisplayed(getResourceString(R.string.offer_advantage_online_view_title))
            isDescriptionDisplayed(getResourceString(R.string.offer_advantage_online_view_description))
            if(showCallBtn){
                isCallButtonDisplayed(getResourceString(R.string.call))
            } else {
                isCallButtonNotDisplayed()
            }
            if(showChatBtn){
                isChatButtonDisplayed(getResourceString(R.string.write_message))
            } else {
                isChatButtonNotDisplayed()
            }
        }
    }

    private fun checkCallButtonAction(){
        Intents.init()
        performAdvantageOnlineView { makeCall() }
        checkCommon { isActionDialIntentCalled(PHONE_NUMBER) }
    }

    private fun checkOpenChatAction() {
        performAdvantageOnlineView { openChat() }
        checkChatRoom { isChatSubjectDisplayed(CHAT_TITLE, CHAT_SUBTITLE) }
    }

    private fun openOfferCard(offerId: String) {
        webServerRule.routing { delegateDispatcher(GetOfferDispatcher.getOffer(CARS_CATEGORY, offerId)) }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + offerId)
    }

    companion object {
        private const val CARS_CATEGORY = "cars"
        private const val OFFER_CARD_PATH = "https://auto.ru/cars/used/sale/"

        private const val PHONE_NUMBER = "+7 985 440-66-27"

        private const val CHAT_TITLE = "Audi A5 I Рестайлинг, 2012"
        private const val CHAT_SUBTITLE = "1 100 000 \u20BD"
    }
}
