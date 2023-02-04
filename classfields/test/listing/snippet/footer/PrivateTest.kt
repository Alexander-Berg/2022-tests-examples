package ru.auto.ara.test.listing.snippet.footer

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.chat.GetRoomMessagesDispatcher
import ru.auto.ara.core.dispatchers.chat.PostChatRoomDispatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.chat.checkChatRoom
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class PrivateTest {
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val extendedOfferDispatcher = PostSearchOffersDispatcher("informers_extended_snippet_vin_ok_no_history")
    private val extendedWithMetroDispatcher = PostSearchOffersDispatcher("extended_private_with_metro")
    private val commonOfferDispatcher = PostSearchOffersDispatcher("informers_common_snippet_vin_ok_no_history")
    private val commonWithMetroDispatcher = PostSearchOffersDispatcher("common_private_with_metro")
    private val extendedWithChatOnly = PostSearchOffersDispatcher("extended_with_chat_only")

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars"),
            GetPhonesDispatcher.onePhone("1087439802-b6940925"),
            PostChatRoomDispatcher("room_after_post"),
            GetRoomMessagesDispatcher.getEmptyResponse(),
            ParseDeeplinkDispatcher.carsAll()
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldSeeContactsAndAddressOfPrivateSellerOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsDisplayed("Юрий\nЧастное лицо")
                isCallButtonDisplayed()
                isFooterWithAddressDisplayed("Москва")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeContactsAndMetroOfPrivateSellerOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedWithMetroDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsDisplayed("Юрий\nЧастное лицо")
                isCallButtonDisplayed()
                isFooterWithAddressWithMetroDisplayed("Александровский сад", "#0099CC")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeContactsAndAddressOfPrivateSellerOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsNotDisplayed()
                isCallButtonNotDisplayed()
                isFooterWithAddressDisplayed("Москва")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeContactsAndMetroOfPrivateSellerOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonWithMetroDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsNotDisplayed()
                isCallButtonNotDisplayed()
                isFooterWithAddressWithMetroDisplayed("Александровский сад", "#0099CC")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldMakeCallFromSnippet() {
        webServerRule.routing { delegateDispatcher(extendedWithMetroDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        Intents.init()
        performListingOffers {
            scrollToFilterPromo()
            interactions.onCallOrChatButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
    }

    @Test
    fun shouldMakeChatFromChatOnlyExtendedSnippet() {
        webServerRule.routing { delegateDispatcher(extendedWithChatOnly) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        Intents.init()
        performListingOffers {
            scrollToFilterPromo()
            interactions.onCallOrChatButton().waitUntil(
                isCompletelyDisplayed(),
                withClearText(R.string.action_write)
            ).performClick()
        }
        webServerRule.routing {
            userSetup()
            postLoginOrRegisterSuccess()
        }
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkChatRoom { isChatSubjectDisplayed("Audi A5 I Рестайлинг, 2012", "1 100 000 \u20BD") }
    }
}
