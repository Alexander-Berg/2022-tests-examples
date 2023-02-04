package ru.auto.ara.test.offer.tradein

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.offer_card.TradeInRequestDispatcher
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.UserAssets
import ru.auto.ara.core.dispatchers.user.postUserPhones
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceStringWithoutNonbreakingSpace
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class TradeInTest {
    private val URL_PREFIX = "https://auto.ru/cars/new/sale/"
    private val OFFER_ID = "1084250931-f8070529"
    private val PHONE = "+7 (000) 000-00-00"
    private val SECOND_PHONE = "+7 (000) 000-00-01"
    private val SMS_CODE = "0000"
    private val CARS_CATEGORY = "cars"
    private val dispatcherHolder = DispatcherHolder()
    private val tradeRequestWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            TradeInRequestDispatcher(OFFER_ID, tradeRequestWatcher),
            dispatcherHolder
        )
        postUserPhones()
        userSetup()
        postLoginOrRegisterSuccess()
        getOffer(OFFER_ID)
    }

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityRule,
        SetPreferencesRule()
    )


    @Test
    fun shouldSeeTradeInBlock() {
        webServerRule.routing { userSetup(UserAssets.TWO_PHONES) }
        launchActivityScrollToTradeInWithoutLogin()

        checkOfferCard { isTradeInBlockDisplayed() }
    }

    @Test
    fun shouldSeeTradeInInfoBottomsheet() {
        launchActivityScrollToTradeInWithoutLogin()
        performOfferCard { interactions.onTradeInInfoBtn().waitUntilIsCompletelyDisplayed().performClick() }
        checkOfferCard { isTradeInBottomSheetDisplayed() }
        performOfferCard { interactions.onBottomsheetActionButton().performClick() }
        checkLogin { isOnlyPhoneAuth(getResourceStringWithoutNonbreakingSpace(R.string.request_trade_in_title)) }
    }

    @Test
    fun shouldSendRequestWhenEmptyActiveOffers() {
        val userOffersWatcher = RequestWatcher()
        dispatcherHolder.innerDispatcher = GetUserOffersDispatcher.empty(CARS_CATEGORY, userOffersWatcher)
        launchActivityScrollToTradeInWithLogin()

        checkOfferCard {
            isOfferCard()
            isSuccessTradeInLabel()
            userOffersWatcher.checkQueryParameter("status", "ACTIVE")
            tradeRequestWatcher.checkRequestBodyParameters(
                "client_info.client_id" to "39318",
                "client_offer_info.category" to "CARS",
                "client_offer_info.section" to "NEW",
                "client_offer_info.offer_id" to OFFER_ID,
                "user_info.phone_number" to PHONE
            )
        }
    }

    @Test
    fun shouldSendRequestWithSelectedUserOffer() {
        val firstMark = "Chevrolet Lanos I, 2006"
        val secondMark = "Mercedes-Benz GLA-klasse X156 250, 2013"
        dispatcherHolder.innerDispatcher = GetUserOffersDispatcher.multipleCarsActive()
        launchActivityScrollToTradeInWithLogin()

        checkOfferCard {
            isTradeInBottomsheetForChooseCarDisplayed()
            isTradeInOfferSnippetDisplayed(firstMark, "320 000 км")
            isTradeInOfferSnippetDisplayed(secondMark, "9 859 км")
        }

        performOfferCard { interactions.onOfferTitle(secondMark).performClick() }.checkResult {
            isOfferCard()
            isSuccessTradeInLabel()
            tradeRequestWatcher.checkRequestBodyParameters(
                "client_info.client_id" to "39318",
                "client_offer_info.category" to "CARS",
                "client_offer_info.section" to "NEW",
                "client_offer_info.offer_id" to OFFER_ID,
                "user_info.phone_number" to PHONE,
                "user_offer_info.category" to "CARS",
                "user_offer_info.description.license_plate" to "АА627838",
                "user_offer_info.description.mark" to "MERCEDES",
                "user_offer_info.description.mileage" to "9859",
                "user_offer_info.description.model" to "GLA_CLASS",
                "user_offer_info.description.price" to "320000000",
                "user_offer_info.description.year" to "2013",
                "user_offer_info.offer_id" to "1083666539-6ffe4d13",
                "user_offer_info.section" to "USED"
            )
        }
    }

    @Test
    fun shouldSeePhoneAuthWhenAddAnotherPhone() {
        val anotherPhoneFieldLabel = "Другой номер"
        dispatcherHolder.innerDispatcher = GetUserOffersDispatcher.multipleCarsActive()
        launchActivityScrollToTradeInWithLogin()
        performOfferCard {
            interactions.onBottomsheetCloseIcon().waitUntilIsCompletelyDisplayed().performClick()
            scrollToTradeIn()
            clickTradeInRequest()
        }.checkResult {
            isBottomsheetWithTwoPhonesAndAnotherPhoneLabelDisplayed(PHONE, SECOND_PHONE, anotherPhoneFieldLabel)
        }
        performOfferCard { interactions.onLabel(anotherPhoneFieldLabel).performClick() }
        checkLogin { isOnlyPhoneAuth(getResourceStringWithoutNonbreakingSpace(R.string.request_trade_in_title)) }
    }

    @Test
    fun shouldSendRequestWithSelectedPhone() {
        dispatcherHolder.innerDispatcher = GetUserOffersDispatcher.multipleCarsActive()
        launchActivityScrollToTradeInWithLogin()
        performOfferCard {
            interactions.onBottomsheetCloseIcon().waitUntilIsCompletelyDisplayed().performClick()
            scrollToTradeIn()
            clickTradeInRequest()
            dispatcherHolder.innerDispatcher = GetUserOffersDispatcher.empty(CARS_CATEGORY)
            clickOnPhone(PHONE)
        }.checkResult {
            isOfferCard()
            isSuccessTradeInLabel()
            tradeRequestWatcher.checkRequestBodyParameter("user_info.phone_number", PHONE)
        }
    }

    private fun launchActivityScrollToTradeInWithoutLogin() {
        activityRule.launchDeepLinkActivity(URL_PREFIX + OFFER_ID)
        checkOfferCard { isOfferCard() }
        performOfferCard { scrollToTradeIn() }
    }

    private fun launchActivityScrollToTradeInWithLogin() {
        activityRule.launchDeepLinkActivity(URL_PREFIX + OFFER_ID)
        checkOfferCard { isOfferCard() }
        performOfferCard {
            scrollToTradeIn()
            clickTradeInRequest()
        }
        webServerRule.routing {
            postLoginOrRegisterSuccess()
            userSetup(UserAssets.TWO_PHONES)
        }
        performLogin { loginWithPhoneAndCode(PHONE, SMS_CODE) }
    }
}
